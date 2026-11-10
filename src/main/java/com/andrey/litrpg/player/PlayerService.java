package com.andrey.litrpg.player;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.core.ModAttachments;
import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.Progression;
import com.andrey.litrpg.net.PlayerSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.andrey.litrpg.net.Net;
import com.andrey.litrpg.skill.ClassOffers;
import com.andrey.litrpg.skill.ClassRegistry;
import com.andrey.litrpg.skill.PlayerClass;
import com.andrey.litrpg.skill.SkillService;
import com.andrey.litrpg.skill.Style;

/** Всё, что меняет персонажа, идёт через этот класс: изменение → пересчёт → отправка клиенту. */
public final class PlayerService {
    private static final net.minecraft.resources.ResourceLocation STATS_MOD = LitRPG.id("stats");

    private PlayerService() {
    }

    public static PlayerData data(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER);
    }

    public static StatBreakdown breakdown(PlayerData d) {
        return StatBreakdown.compute(d.inputs(), Balance.get(Balance.PROGRESSION),
                Balance.get(Balance.CHARACTER), Balance.get(Balance.ROLES));
    }

    /** Полный расчёт с надетой экипировкой — его используют бой и окно. */
    public static StatBreakdown breakdown(ServerPlayer player) {
        return StatBreakdown.compute(data(player).inputs(com.andrey.litrpg.gear.PlayerGear.collect(player)),
                Balance.get(Balance.PROGRESSION), Balance.get(Balance.CHARACTER), Balance.get(Balance.ROLES));
    }

    /** Пересчитать характеристики, применить к атрибутам Minecraft и отправить клиенту. */
    public static void refresh(ServerPlayer player) {
        StatBreakdown b = breakdown(player);
        float oldMax = player.getMaxHealth();
        float ratio = oldMax > 0 ? player.getHealth() / oldMax : 1f;
        apply(player, Attributes.MAX_HEALTH, b.get(Derived.MAX_HEALTH) - 20, AttributeModifier.Operation.ADD_VALUE);
        apply(player, Attributes.MOVEMENT_SPEED, b.get(Derived.MOVE_SPEED), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        float newMax = player.getMaxHealth();
        if (newMax != oldMax && player.isAlive()) player.setHealth(Math.min(newMax, ratio * newMax));
        sync(player);
    }

    private static void apply(ServerPlayer p, Holder<Attribute> attr, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(STATS_MOD);
        if (cur != null && cur.amount() == amount && cur.operation() == op) return;
        inst.removeModifier(STATS_MOD);
        // Постоянный модификатор сохраняется в мире — здоровье не обрезается до 20 при перезаходе.
        inst.addPermanentModifier(new AttributeModifier(STATS_MOD, amount, op));
    }

    public static void sync(ServerPlayer player) {
        PlayerData d = data(player);
        Net.send(player, new PlayerSyncPayload(d.role, d.level, d.xp, d.allocatedCopy(), d.classId,
                new java.util.HashMap<>(d.ranks), new java.util.ArrayList<>(d.offers)));
        SkillService.sync(player);
        LookService.changed(player);
    }

    // ------------------------------------------------------------------ опыт и уровни

    public static void addXp(ServerPlayer player, long amount) {
        PlayerData d = data(player);
        Progression p = Balance.get(Balance.PROGRESSION);
        int before = d.level;
        Leveling.Result r = Leveling.addXp(d.level, d.xp, amount, p);
        d.level = r.level();
        d.xp = r.xp();
        if (r.levelsGained() > 0) onLevelUp(player, before, d.level);
        else sync(player);
    }

    public static void setLevel(ServerPlayer player, int level) {
        PlayerData d = data(player);
        Progression p = Balance.get(Balance.PROGRESSION);
        int before = d.level;
        d.level = Math.max(1, Math.min(p.maxLevel(), level));
        d.xp = 0;
        // Понизили уровень — вложенных очков может стать больше заработанных: возвращаем все.
        if (d.freeStatPoints(p) < 0) java.util.Arrays.fill(d.allocated, 0);
        if (d.level > before) onLevelUp(player, before, d.level);
        else refresh(player);
        // Понизили уровень ниже открытия класса — класс остаётся (его снимает только торговец).
    }

    private static void onLevelUp(ServerPlayer player, int from, int to) {
        refresh(player);
        player.setHealth(player.getMaxHealth());
        Progression p = Balance.get(Balance.PROGRESSION);
        PlayerData d = data(player);
        SystemChat.send(player, Component.literal("Уровень повышен: " + from + " → " + to + ". ")
                .append(Component.literal("Свободных очков характеристик: " + d.freeStatPoints(p))
                        .withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" (окно персонажа — K)").withStyle(ChatFormatting.GRAY)));
        SystemChat.title(player, Component.literal("Уровень " + to).withStyle(ChatFormatting.GOLD),
                Component.literal("Здоровье восстановлено").withStyle(ChatFormatting.GRAY));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1f);
        if (d.classId.isEmpty() && to >= p.classLevel()) offerClasses(player);
        else if (d.playerClass() != null) announceUnlocks(player, from, to);
    }

    // ------------------------------------------------------------------ класс

    /** Подобрать классы по стилю игры (если класса нет, роль выбрана и уровень позволяет). */
    public static void offerClasses(ServerPlayer player) {
        PlayerData d = data(player);
        Progression p = Balance.get(Balance.PROGRESSION);
        if (!d.classId.isEmpty() || !d.role.chosen() || d.level < p.classLevel()) return;
        java.util.Set<String> taken = new java.util.HashSet<>();
        for (ServerPlayer m : com.andrey.litrpg.party.PartyService.online(player.server, player)) {
            if (m != player) taken.add(data(m).classId);
        }
        d.offers.clear();
        for (var s : ClassOffers.offer(d.role, d.style, taken, 3)) d.offers.add(s.cls().id());
        if (d.offers.isEmpty()) return;
        SystemChat.send(player, Component.literal("Уровень " + p.classLevel() + ". Система изучила ваш стиль боя и подобрала "
                + "классы — окно выбора уже открыто (или клавиша K).").withStyle(ChatFormatting.LIGHT_PURPLE));
        SystemChat.title(player, Component.literal("Выбор класса").withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal("Система подобрала классы по стилю игры").withStyle(ChatFormatting.GRAY));
        sync(player);
    }

    /** Игрок выбрал класс из предложенных. */
    public static boolean chooseClass(ServerPlayer player, String classId) {
        PlayerData d = data(player);
        if (!d.classId.isEmpty() || !d.offers.contains(classId)) return false;
        setClass(player, classId);
        PlayerClass c = d.playerClass();
        SystemChat.send(player, Component.literal("Класс получен: ").append(
                Component.literal(c.name()).withColor(c.color() & 0xFFFFFF)).append(Component.literal(". " + c.idea())));
        SystemChat.title(player, Component.literal(c.icon() + " " + c.name()).withColor(c.color() & 0xFFFFFF),
                Component.literal("Класс получен").withStyle(ChatFormatting.GRAY));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1f);
        return true;
    }

    /** Назначить класс (выбор игрока, торговец, команда): ранги сбрасываются, перезарядки тоже. */
    public static void setClass(ServerPlayer player, String classId) {
        PlayerData d = data(player);
        d.classId = ClassRegistry.get(classId) != null ? classId : "";
        d.offers.clear();
        d.ranks.clear();
        SkillService.reset(player);
        refresh(player);
        if (d.classId.isEmpty()) offerClasses(player); // класс сброшен — Система предлагает заново
    }

    /** Поднять ранг навыка класса: ранг n стоит n очков класса. */
    public static boolean rankUp(ServerPlayer player, String skillId) {
        PlayerData d = data(player);
        PlayerClass c = d.playerClass();
        if (c == null) return false;
        boolean open = c.unlocked(d.level).stream().anyMatch(s -> s.id.equals(skillId));
        int max = Balance.get(Balance.SKILLS).common().maxRank();
        int cur = d.rank(skillId);
        if (!open || cur >= max || d.freeClassPoints(Balance.get(Balance.PROGRESSION)) < cur + 1) return false;
        d.ranks.put(skillId, cur + 1);
        sync(player);
        return true;
    }

    private static void announceUnlocks(ServerPlayer player, int from, int to) {
        PlayerClass c = data(player).playerClass();
        for (var u : c.skills()) {
            if (u.level() > from && u.level() <= to) {
                SystemChat.send(player, Component.literal("Новый навык: ").append(
                        Component.literal(u.skill().name).withColor(u.skill().color)));
            }
        }
    }

    public static void markBought(ServerPlayer player, String key) {
        data(player).bought.add(key);
    }

    /** Новый этап — лавки обновляются. */
    public static void clearBought(ServerPlayer player) {
        data(player).bought.clear();
    }

    /** Забыть ранги: очки класса возвращаются. */
    public static void resetRanks(ServerPlayer player) {
        data(player).ranks.clear();
        sync(player);
    }

    public static void addStyle(ServerPlayer player, Style s, int n) {
        data(player).style.merge(s, n, Integer::sum);
    }

    // ------------------------------------------------------------------ роль и характеристики

    /** Выбор роли игроком: только если роль ещё не выбрана. */
    public static boolean chooseRole(ServerPlayer player, Role role) {
        PlayerData d = data(player);
        if (d.role.chosen() || !role.chosen()) return false;
        setRole(player, role);
        com.andrey.litrpg.combat.StarterKit.give(player, role);
        SystemChat.send(player, Component.literal("Роль принята: ").append(
                Component.literal(role.title).withColor(role.color & 0xFFFFFF)));
        SystemChat.title(player, Component.literal(role.title).withColor(role.color & 0xFFFFFF),
                Component.literal("Система запомнила ваш выбор").withStyle(ChatFormatting.GRAY));
        return true;
    }

    public static void setRole(ServerPlayer player, Role role) {
        data(player).role = role;
        refresh(player);
    }

    /** Вложить очки: {@code add[i]} очков в характеристику i. Всё или ничего. */
    public static boolean allocate(ServerPlayer player, int[] add) {
        PlayerData d = data(player);
        if (add.length != Stat.ALL.length) return false;
        int total = 0;
        for (int a : add) {
            if (a < 0) return false;
            total += a;
        }
        if (total == 0 || total > d.freeStatPoints(Balance.get(Balance.PROGRESSION))) return false;
        for (int i = 0; i < add.length; i++) d.allocated[i] += add[i];
        refresh(player);
        return true;
    }

    public static void resetStats(ServerPlayer player) {
        java.util.Arrays.fill(data(player).allocated, 0);
        refresh(player);
    }

    // ------------------------------------------------------------------ бой

    public static void markCombat(ServerPlayer player) {
        data(player).lastCombatTick = player.server.getTickCount();
    }

    public static boolean inCombat(ServerPlayer player) {
        int timeout = Balance.get(Balance.CHARACTER).regen().combatTimeoutSeconds() * 20;
        return player.server.getTickCount() - data(player).lastCombatTick < timeout;
    }
}
