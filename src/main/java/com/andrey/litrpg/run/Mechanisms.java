package com.andrey.litrpg.run;

import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Загадки и ловушки этапа (тип — в схеме):
 * <ul>
 *   <li>{@code plates} — на всех плитах стоят одновременно {@code hold} секунд (разделение отряда);</li>
 *   <li>{@code levers} — все рычаги включены;</li>
 *   <li>{@code sequence} — кнопки нажаты по порядку (ошибка — сброс и удар);</li>
 *   <li>{@code memory} — первая точка запускает показ, остальные — столбы: повторить вспышки, {@code rounds} раундов;</li>
 *   <li>{@code riddle} — стоя в области, написать ответ в чат;</li>
 *   <li>{@code hazard} — ловушка: каждые {@code period} с область (или её полосы) вспыхивает разметкой и бьёт.</li>
 * </ul>
 * Решённая загадка — «mech:id» в сделанном: от неё зависят преграды и опыт «за загадки».
 */
public final class Mechanisms {
    /** Прогресс загадок этой попытки (вайп и смена этапа сбрасывают). */
    private static final Map<String, Integer> PROGRESS = new HashMap<>();
    private static final Map<String, List<Integer>> MEMORY = new HashMap<>();
    private static final Map<String, Long> BUSY_UNTIL = new HashMap<>();
    private static final List<Runnable> NEXT = new ArrayList<>();
    private static final List<long[]> NEXT_AT = new ArrayList<>();

    private Mechanisms() {
    }

    static void clear() {
        PROGRESS.clear();
        MEMORY.clear();
        BUSY_UNTIL.clear();
        NEXT.clear();
        NEXT_AT.clear();
    }

    private static void later(long at, Runnable r) {
        NEXT_AT.add(new long[]{at});
        NEXT.add(r);
    }

    /** Каждый тик: отложенные вспышки и ловушки по ритму; каждые 10 тиков — плиты и рычаги. */
    static void tick(MinecraftServer server, RunState s, Layout.Stage st, List<ServerPlayer> alive) {
        ServerLevel level = RunService.dungeon(server);
        long now = level.getGameTime();
        for (int i = NEXT.size() - 1; i >= 0; i--) {
            if (now >= NEXT_AT.get(i)[0]) {
                NEXT_AT.remove(i);
                NEXT.remove(i).run();
            }
        }
        for (Layout.Mech m : st.mechs()) {
            if (m.type().equals("hazard")) {
                hazard(level, m, alive, now);
                continue;
            }
            if (s.done.contains("mech:" + m.id()) || now % 10 != 0) continue;
            switch (m.type()) {
                case "plates" -> plates(server, s, st, level, m, alive);
                case "levers" -> {
                    if (!m.points().isEmpty() && m.points().stream().allMatch(p -> powered(level.getBlockState(p)))) {
                        solve(server, s, st, m);
                    }
                }
                default -> {
                }
            }
        }
    }

    private static boolean powered(BlockState st) {
        return st.hasProperty(BlockStateProperties.POWERED) && st.getValue(BlockStateProperties.POWERED);
    }

    static void solve(MinecraftServer server, RunState s, Layout.Stage st, Layout.Mech m) {
        if (!s.done.add("mech:" + m.id())) return;
        s.setDirty();
        ServerLevel level = RunService.dungeon(server);
        for (BlockPos p : m.points()) Fx.pillar(level, Vec3.atBottomCenterOf(p), 0.6f, 3, 20, Fx.HOLY);
        Vec3 c = center(m);
        level.playSound(null, BlockPos.containing(c), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1f, 1.4f);
        for (ServerPlayer p : RunService.onlineMembers(server)) {
            SystemChat.send(p, Component.literal("Загадка решена: «" + title(m) + "».").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (m.puzzle()) Rewards.claim(server, s, "mech:" + m.id(), Rewards.extraXp(st), "загадка");
        RunService.syncAll(server);
    }

    private static String title(Layout.Mech m) {
        return m.text().isEmpty() ? m.id() : m.text().split("\\|")[0];
    }

    private static Vec3 center(Layout.Mech m) {
        if (m.area().isPresent()) return m.area().get().aabb().getCenter();
        if (m.points().isEmpty()) return Vec3.ZERO;
        Vec3 sum = Vec3.ZERO;
        for (BlockPos p : m.points()) sum = sum.add(Vec3.atCenterOf(p));
        return sum.scale(1.0 / m.points().size());
    }

    // ------------------------------------------------------------------ плиты (разделение отряда)

    /**
     * Плиты: на каждой кто-то стоит, и стоят все живые бойцы отряда. Втроём это три плиты по одному;
     * вчетвером–вшестером на плитах встаёт весь отряд, по двое на плиту, — и прикрыть некому.
     */
    private static void plates(MinecraftServer server, RunState s, Layout.Stage st, ServerLevel level, Layout.Mech m,
                               List<ServerPlayer> alive) {
        int on = 0;
        java.util.Set<ServerPlayer> standing = new java.util.HashSet<>();
        for (BlockPos p : m.points()) {
            Vec3 c = Vec3.atBottomCenterOf(p).add(0, 1, 0);
            boolean taken = false;
            for (ServerPlayer a : alive) {
                if (onPlate(a, c)) {
                    taken = true;
                    standing.add(a);
                }
            }
            if (taken) on++;
            Vector3f col = taken ? new Vector3f(0.4f, 1f, 0.5f) : new Vector3f(0.6f, 0.6f, 0.7f);
            level.sendParticles(new DustParticleOptions(col, 1.2f), p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0);
        }
        int need = Math.max(m.points().size(), alive.size());
        int hold = (int) Math.round(m.n("hold", 3) * 20);
        boolean ready = on == m.points().size() && standing.size() >= need;
        int prog = ready ? PROGRESS.merge(m.id(), 10, Integer::sum) : 0;
        if (prog == 0) PROGRESS.remove(m.id());
        for (ServerPlayer a : standing) {
            a.displayClientMessage(Component.literal("Плиты: " + on + " из " + m.points().size()
                    + (alive.size() > m.points().size() ? " · на плитах " + standing.size() + " из " + need + " бойцов" : "")
                    + (prog > 0 ? " · держите " + Math.max(0, (hold - prog) / 20 + 1) + " с" : "")).withStyle(ChatFormatting.AQUA), true);
        }
        if (prog >= hold) solve(server, s, st, m);
    }

    /** Стоит на плите: в пределах её блока и соседних полублоков — вдвоём на одной плите тоже помещаются. */
    private static boolean onPlate(ServerPlayer a, Vec3 top) {
        double dx = a.getX() - top.x, dz = a.getZ() - top.z;
        return dx * dx + dz * dz <= 1.1 * 1.1 && Math.abs(a.getY() - (top.y - 1)) <= 1.6;
    }

    // ------------------------------------------------------------------ кнопки, столбы памяти

    /** ПКМ по блоку в подземелье: шаг последовательности или загадки памяти. */
    static void onUse(ServerPlayer p, BlockPos pos) {
        MinecraftServer server = p.server;
        RunState s = RunService.state(server);
        Layout.Stage st = RunService.currentStage(server);
        if (st == null || !s.member(p.getUUID()) || s.isSpirit(p.getUUID())) return;
        for (Layout.Mech m : st.mechs()) {
            if (s.done.contains("mech:" + m.id()) || !m.points().contains(pos)) continue;
            if (m.type().equals("sequence")) sequence(server, s, st, m, p, pos);
            else if (m.type().equals("memory")) memory(server, s, st, m, p, pos);
        }
    }

    private static void sequence(MinecraftServer server, RunState s, Layout.Stage st, Layout.Mech m, ServerPlayer p, BlockPos pos) {
        int idx = PROGRESS.getOrDefault(m.id(), 0);
        ServerLevel level = p.serverLevel();
        if (m.points().get(idx).equals(pos)) {
            PROGRESS.put(m.id(), idx + 1);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1f, 0.7f + idx * 0.15f);
            Fx.pillar(level, Vec3.atBottomCenterOf(pos), 0.4f, 2, 12, Fx.ARCANE);
            if (idx + 1 == m.points().size()) solve(server, s, st, m);
        } else {
            PROGRESS.remove(m.id());
            zap(p, m, "Неверный порядок — механизм сбросился.");
        }
    }

    private static void memory(MinecraftServer server, RunState s, Layout.Stage st, Layout.Mech m, ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (now < BUSY_UNTIL.getOrDefault(m.id(), 0L)) return;
        List<BlockPos> pillars = m.points().subList(1, m.points().size());
        int round = PROGRESS.getOrDefault(m.id() + "#round", 0);
        if (pos.equals(m.points().get(0))) {
            // показ: 3, 5, 7… вспышек
            List<Integer> seq = new ArrayList<>();
            for (int i = 0; i < 3 + 2 * round; i++) seq.add(level.random.nextInt(pillars.size()));
            MEMORY.put(m.id(), seq);
            PROGRESS.put(m.id(), 0);
            for (int i = 0; i < seq.size(); i++) {
                BlockPos at = pillars.get(seq.get(i));
                int k = seq.get(i);
                later(now + 10 + i * 14L, () -> flash(level, at, k));
            }
            BUSY_UNTIL.put(m.id(), now + 10 + seq.size() * 14L);
            SystemChat.send(p, Component.literal("Запомните порядок вспышек (раунд " + (round + 1) + " из "
                    + (int) m.n("rounds", 3) + ").").withStyle(ChatFormatting.AQUA));
            return;
        }
        List<Integer> seq = MEMORY.get(m.id());
        if (seq == null) return;
        int idx = PROGRESS.getOrDefault(m.id(), 0);
        int k = pillars.indexOf(pos);
        if (k == seq.get(idx)) {
            flash(level, pos, k);
            PROGRESS.put(m.id(), idx + 1);
            if (idx + 1 == seq.size()) {
                MEMORY.remove(m.id());
                int next = round + 1;
                if (next >= (int) m.n("rounds", 3)) {
                    solve(server, s, st, m);
                } else {
                    PROGRESS.put(m.id() + "#round", next);
                    SystemChat.send(p, Component.literal("Верно! Нажмите начало — следующий раунд.").withStyle(ChatFormatting.GREEN));
                }
            }
        } else {
            MEMORY.remove(m.id());
            zap(p, m, "Не тот столб — начните раунд заново.");
        }
    }

    private static void flash(ServerLevel level, BlockPos at, int k) {
        Fx.pillar(level, Vec3.atBottomCenterOf(at.above()), 0.5f, 3, 10, new int[]{Fx.FIRE, Fx.ICE, Fx.NATURE, Fx.HOLY, Fx.SHADOW,
                Fx.ARCANE}[k % 6]);
        level.playSound(null, at, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.2f, 0.6f + k * 0.2f);
    }

    private static void zap(ServerPlayer p, Layout.Mech m, String why) {
        p.hurt(p.damageSources().magic(), (float) (m.n("zap", 0.15) * 20));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.6f, 1.6f);
        SystemChat.send(p, Component.literal(why).withStyle(ChatFormatting.RED));
    }

    // ------------------------------------------------------------------ текстовая загадка

    /** Сообщение в чат: если игрок стоит у загадки — проверить ответ. */
    static boolean onChat(ServerPlayer p, String message) {
        RunState s = RunService.state(p.server);
        Layout.Stage st = RunService.currentStage(p.server);
        if (st == null || !s.member(p.getUUID()) || s.isSpirit(p.getUUID())) return false;
        for (Layout.Mech m : st.mechs()) {
            if (!m.type().equals("riddle") || s.done.contains("mech:" + m.id()) || m.area().isEmpty()) continue;
            if (!m.area().get().aabb().inflate(1).contains(p.position())) continue;
            String said = normalize(message);
            boolean right = m.answers().stream().map(Mechanisms::normalize).anyMatch(said::equals);
            if (right) {
                p.server.execute(() -> solve(p.server, s, st, m));
            } else {
                p.server.execute(() -> SystemChat.send(p, Component.literal("Система: «" + message.strip() + "» — нет.")
                        .withStyle(ChatFormatting.GRAY)));
            }
            return true;
        }
        return false;
    }

    static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("[^\\p{L}\\p{N} ]", "").strip().replaceAll("\\s+", " ");
    }

    // ------------------------------------------------------------------ ловушки

    /**
     * Ловушка: каждые {@code period} с (со сдвигом {@code offset}) область вспыхивает на {@code warn} с и бьёт тех, кто внутри,
     * долей {@code share} здоровья. {@code pattern}: 0 — вся область, 1/2 — полосы вдоль x/z (через одну, по очереди),
     * 3 — ярусы по высоте (шахты с лестницей или лозой).
     */
    private static void hazard(ServerLevel level, Layout.Mech m, List<ServerPlayer> alive, long now) {
        if (m.area().isEmpty()) return;
        Layout.Box b = m.area().get();
        Vec3 c = b.aabb().getCenter();
        if (alive.stream().noneMatch(a -> a.position().distanceTo(c) < 28)) return;
        int period = (int) Math.round(m.n("period", 4) * 20), warn = (int) Math.round(m.n("warn", 1.2) * 20);
        long t = now - Math.round(m.n("offset", 0) * 20);
        if (Math.floorMod(t, period) != 0) return;
        List<Layout.Box> parts = hazardParts(b, (int) m.n("pattern", 0), (int) m.n("stripe", 1), Math.floorDiv(t, period));
        for (Layout.Box part : parts) {
            boolean xLong = part.x2() - part.x1() >= part.z2() - part.z1();
            double y = part.y1() + 0.05;
            Vec3 from = xLong ? new Vec3(part.x1(), y, (part.z1() + part.z2() + 1) / 2.0) : new Vec3((part.x1() + part.x2() + 1) / 2.0, y, part.z1());
            Vec3 to = xLong ? new Vec3(part.x2() + 1, y, (part.z1() + part.z2() + 1) / 2.0) : new Vec3((part.x1() + part.x2() + 1) / 2.0, y, part.z2() + 1);
            float width = xLong ? part.z2() - part.z1() + 1 : part.x2() - part.x1() + 1;
            Fx.line(level, from, to, width, warn, Fx.DANGER, true);
        }
        later(now + warn, () -> {
            for (Layout.Box part : parts) {
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, part.aabb().inflate(0.1, 0.5, 0.1))) {
                    if (p.isSpectator() || p.isCreative()) continue;
                    p.invulnerableTime = 0;
                    p.hurt(level.damageSources().magic(), (float) (m.n("share", 0.25) * 20));
                }
                Vec3 pc = part.aabb().getCenter();
                level.playSound(null, BlockPos.containing(pc), switch ((int) m.n("sound", 0)) {
                    case 1 -> SoundEvents.FIRECHARGE_USE;
                    case 2 -> SoundEvents.ARROW_SHOOT;
                    default -> SoundEvents.TRIDENT_HIT;
                }, SoundSource.BLOCKS, 0.7f, 1.2f);
            }
        });
    }

    /**
     * Какие части области бьют в этот цикл: 0 — вся; 1/2 — полосы вдоль x/z через одну; 3 — ярусы по высоте через один
     * (ловушка подъёма: шахта бьёт поочерёдно, лезут в такт). Ширина полосы/яруса — {@code stripe}.
     */
    static List<Layout.Box> hazardParts(Layout.Box b, int pattern, int stripe, long cycle) {
        List<Layout.Box> parts = new ArrayList<>();
        if (pattern == 0) {
            parts.add(b);
            return parts;
        }
        int w = Math.max(1, stripe);
        int from = switch (pattern) { case 1 -> b.z1(); case 3 -> b.y1(); default -> b.x1(); };
        int to = switch (pattern) { case 1 -> b.z2(); case 3 -> b.y2(); default -> b.x2(); };
        for (int k = from, i = 0; k <= to; k += w, i++) {
            if ((i + cycle) % 2 != 0) continue;
            int k2 = Math.min(to, k + w - 1);
            parts.add(switch (pattern) {
                case 1 -> new Layout.Box(b.x1(), b.y1(), k, b.x2(), b.y2(), k2);
                case 3 -> new Layout.Box(b.x1(), k, b.z1(), b.x2(), k2, b.z2());
                default -> new Layout.Box(k, b.y1(), b.z1(), k2, b.y2(), b.z2());
            });
        }
        return parts;
    }

    // ------------------------------------------------------------------ сброс

    /** Вайп и новый этап: рычаги и кнопки — как в карте. */
    static void resetBlocks(ServerLevel level, Layout.Stage st) {
        var map = com.andrey.litrpg.world.DungeonMap.get();
        for (Layout.Mech m : st.mechs()) {
            for (BlockPos p : m.points()) {
                BlockState orig = map.original(p);
                if (level.isLoaded(p) && !level.getBlockState(p).equals(orig)) level.setBlock(p, orig, Block.UPDATE_ALL);
            }
        }
        clear();
    }

    /** Отладка: снять все загадки этапа. */
    static int solveAll(MinecraftServer server) {
        RunState s = RunService.state(server);
        Layout.Stage st = RunService.currentStage(server);
        if (st == null) return 0;
        int n = 0;
        for (Layout.Mech m : st.mechs()) {
            if (m.puzzle() && !s.done.contains("mech:" + m.id())) {
                solve(server, s, st, m);
                n++;
            }
        }
        return n;
    }
}
