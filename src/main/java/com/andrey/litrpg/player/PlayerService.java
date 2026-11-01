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

    public static StatBreakdown breakdown(ServerPlayer player) {
        return breakdown(data(player));
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
        Net.send(player, new PlayerSyncPayload(d.role, d.level, d.xp, d.allocatedCopy()));
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
        if (from < p.classLevel() && to >= p.classLevel()) {
            SystemChat.send(player, Component.literal("Достигнут уровень " + p.classLevel()
                    + ". Система подберёт вам класс по стилю игры.").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
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
