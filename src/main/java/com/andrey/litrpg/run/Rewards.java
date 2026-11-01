package com.andrey.litrpg.run;

import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.Progression;
import com.andrey.litrpg.core.balance.RunBalance;
import com.andrey.litrpg.player.PlayerService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.UUID;

/**
 * Точный опыт этапа: за этап отряд получает ровно столько опыта, сколько нужно на его 10 уровней. Половина — за отряды
 * (по весам), 45% — за хранителей, 5% — за загадки и тайники. Каждая награда выдаётся один раз за прохождение: после
 * вайпа повторная победа опыта не даёт. Монстры отрядов опыта поштучно не дают.
 */
public final class Rewards {
    private Rewards() {
    }

    /** Весь опыт этапа: от первого его уровня до последнего. */
    public static long budget(Layout.Stage st) {
        return Balance.get(Balance.PROGRESSION).xpBetween(st.fromLevel(), st.toLevel());
    }

    /** Загадки и секретки этапа (за них — доля «extra»). */
    public static int extras(Layout.Stage st) {
        return (int) (st.mechs().stream().filter(Layout.Mech::puzzle).count() + st.caches().stream().filter(Layout.Cache::secret).count());
    }

    public static long extraXp(Layout.Stage st) {
        int n = extras(st);
        return n == 0 ? 0 : Math.round(budget(st) * Balance.get(Balance.RUN).xp().extra() / n);
    }

    /** Доля «за хранителей» с учётом того, что загадок на этапе может не быть. */
    static double bossShare(Layout.Stage st, int extras) {
        RunBalance.Xp x = Balance.get(Balance.RUN).xp();
        return extras > 0 ? x.bosses() : x.bosses() + x.extra();
    }

    public static long packXp(Layout.Stage st, Layout.Pack pack) {
        double total = st.packs().stream().mapToDouble(Layout.Pack::weight).sum();
        if (total <= 0) return 0;
        double packs = Balance.get(Balance.RUN).xp().packs();
        if (st.bosses().isEmpty()) packs += bossShare(st, extras(st));
        return Math.round(budget(st) * packs * pack.weight() / total);
    }

    public static long bossXp(Layout.Stage st) {
        if (st.bosses().isEmpty()) return 0;
        return Math.round(budget(st) * bossShare(st, extras(st)) / st.bosses().size());
    }

    /** Выдать награду {@code id} всему отряду (кто не в сети — получит при входе). Повторно — ничего. */
    static void claim(MinecraftServer server, RunState s, String id, long xp, String what) {
        if (!s.claims.add(id)) return;
        s.setDirty();
        for (UUID member : s.party) {
            ServerPlayer p = server.getPlayerList().getPlayer(member);
            if (p != null) grant(p, s, id, xp, what);
        }
    }

    private static void grant(ServerPlayer p, RunState s, String id, long xp, String what) {
        if (!s.granted.computeIfAbsent(p.getUUID(), k -> new HashSet<>()).add(id)) return;
        s.setDirty();
        if (xp <= 0) return;
        PlayerService.addXp(p, xp);
        SystemChat.send(p, Component.literal("+" + xp + " опыта").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" — " + what).withStyle(ChatFormatting.GRAY)));
    }

    /** Игрок вошёл: выдать награды, которые отряд получил без него. */
    static void catchUp(ServerPlayer p, RunState s) {
        Layout layout = Layout.get();
        for (String id : s.claims) {
            if (s.granted.getOrDefault(p.getUUID(), java.util.Set.of()).contains(id)) continue;
            long xp = xpFor(layout, id);
            grant(p, s, id, xp, "пока вас не было");
        }
    }

    /** Опыт за награду по её id ("pack:…", "boss:…"). */
    static long xpFor(Layout layout, String id) {
        String[] kv = id.split(":", 2);
        if (kv.length < 2) return 0;
        for (Layout.Stage st : layout.stages()) {
            if (kv[0].equals("pack")) {
                var p = st.pack(kv[1]);
                if (p.isPresent()) return packXp(st, p.get());
            } else if (kv[0].equals("boss") && st.boss(kv[1]).isPresent()) {
                return bossXp(st);
            } else if ((kv[0].equals("mech") && st.mech(kv[1]).isPresent())
                    || (kv[0].equals("cache") && st.caches().stream().anyMatch(c -> c.id().equals(kv[1])))) {
                return extraXp(st);
            }
        }
        return 0;
    }

    /**
     * Конец этапа: у каждого — не меньше последнего уровня этапа (округления не должны оставить игрока без класса
     * на 10 уровне).
     */
    static void topUp(ServerPlayer p, Layout.Stage st) {
        var d = PlayerService.data(p);
        if (d.level() >= st.toLevel()) return;
        Progression prog = Balance.get(Balance.PROGRESSION);
        long need = prog.xpBetween(d.level(), st.toLevel()) - d.xp();
        if (need > 0) PlayerService.addXp(p, need);
    }
}
