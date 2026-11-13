package com.andrey.litrpg.run;

import com.andrey.litrpg.core.SystemChat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Титулы и достижения (обновление 15). Титул получают за дело — пройти подземелье, поставить рекорд, пройти паркур —
 * и носят навсегда; активный виден перед ником в чате, над головой и в списке игроков (команда табло на титул).
 * Список и условия — здесь; выдача — {@link Chronicle}, паркур, полигон.
 */
public enum Titles {
    PATHFINDER("pathfinder", "Первопроходец", 0x7FD3FF, "Пройти подземелье до конца"),
    HERO("hero", "Герой Подземелья", 0xFFB347, "Пройти подземелье на героической сложности"),
    FLAWLESS("flawless", "Без потерь", 0xA8F0A8, "Пройти подземелье без единого вайпа"),
    IMMORTAL("immortal", "Бессмертный", 0xFFD700, "Пройти подземелье, ни разу не пав"),
    SWIFT("swift", "Быстрее времени", 0x9AE0FF, "Поставить рекорд самого быстрого забега"),
    SLAYER("slayer", "Гроза хранителей", 0xFF5A4F, "Победить всех пятнадцать хранителей"),
    DESTROYER("destroyer", "Разрушитель", 0xFF7A3A, "Рекорд урона за забег"),
    LIGHTBEARER("lightbearer", "Светоч", 0x7BF5A0, "Рекорд лечения за забег"),
    BULWARK("bulwark", "Несокрушимый", 0x4FA3FF, "Рекорд принятого урона за забег"),
    ANGEL("angel", "Ангел-хранитель", 0xF4F4FF, "Поднять 25 павших (за всё время)"),
    TREASURE("treasure", "Кладоискатель", 0xE0C060, "Открыть 15 тайников (за всё время)"),
    ACROBAT("acrobat", "Акробат", 0xC8A0FF, "Пройти паркур Цитадели"),
    WINDRUNNER("windrunner", "Ветер в ногах", 0xA0F0FF, "Пройти паркур Цитадели быстрее 60 секунд"),
    ELUSIVE("elusive", "Неуловимый", 0xD8D8E8, "Увернуться от 20 приёмов Испытателя подряд");

    public final String id, name, how;
    public final int color;

    Titles(String id, String name, int color, String how) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.how = how;
    }

    public static Titles byId(String id) {
        for (Titles t : values()) if (t.id.equals(id)) return t;
        return null;
    }

    /** Паркур быстрее этого (тики) — «Ветер в ногах». */
    public static final long WINDRUNNER_TICKS = 60 * 20;
    public static final int ELUSIVE_STREAK = 20;
    public static final int ANGEL_REVIVES = 25;
    public static final int TREASURE_CACHES = 15;
    public static final int ALL_BOSSES = 15;

    public Component badge() {
        return Component.literal("[" + name + "]").withColor(color);
    }

    // ------------------------------------------------------------------ выдача

    /** Выдать титул (если его ещё нет): объявление всем, первый титул сразу становится активным. */
    public static boolean grant(MinecraftServer server, java.util.UUID id, String name, Titles t) {
        Records r = Records.get(server);
        Records.Profile p = r.profile(id, name);
        if (!p.titles.add(t.id)) return false;
        if (p.title.isEmpty()) p.title = t.id;
        r.changed();
        Component msg = Component.literal(p.name + " получает титул ").withStyle(ChatFormatting.GRAY)
                .append(t.badge()).append(Component.literal(" — " + t.how.toLowerCase(java.util.Locale.ROOT) + ".")
                        .withStyle(ChatFormatting.GRAY));
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) SystemChat.send(sp, msg);
        ServerPlayer owner = server.getPlayerList().getPlayer(id);
        if (owner != null) {
            owner.playNotifySound(net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    net.minecraft.sounds.SoundSource.MASTER, 0.7f, 1f);
            apply(owner);
        }
        return true;
    }

    public static boolean grant(ServerPlayer p, Titles t) {
        return grant(p.server, p.getUUID(), p.getGameProfile().getName(), t);
    }

    /** Выбрать активный титул (пустой — без титула). */
    public static boolean choose(ServerPlayer p, String id) {
        Records r = Records.get(p.server);
        Records.Profile prof = r.profile(p.getUUID(), p.getGameProfile().getName());
        if (!id.isEmpty() && !prof.titles.contains(id)) return false;
        prof.title = id;
        r.changed();
        apply(p);
        return true;
    }

    // ------------------------------------------------------------------ показ

    private static final String TEAM = "lt_";

    /** Показать активный титул: команда табло с приставкой (видна в чате, над головой, в списке игроков). */
    public static void apply(ServerPlayer p) {
        Scoreboard sb = p.server.getScoreboard();
        String name = p.getScoreboardName();
        PlayerTeam cur = sb.getPlayersTeam(name);
        Records.Profile prof = Records.get(p.server).profileOrNull(p.getUUID());
        Titles t = prof == null ? null : byId(prof.title);
        if (t == null) {
            if (cur != null && cur.getName().startsWith(TEAM)) sb.removePlayerFromTeam(name, cur);
            return;
        }
        String teamName = TEAM + t.id;
        PlayerTeam team = sb.getPlayerTeam(teamName);
        if (team == null) {
            team = sb.addPlayerTeam(teamName);
            team.setPlayerPrefix(t.badge().copy().append(Component.literal(" ")));
            team.setAllowFriendlyFire(true);
        }
        if (cur != team) sb.addPlayerToTeam(name, team);
    }
}
