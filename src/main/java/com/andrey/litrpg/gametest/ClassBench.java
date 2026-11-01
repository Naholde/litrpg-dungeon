package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.combat.DamageService;
import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.combat.WeaponKind;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.mob.Bestiary;
import com.andrey.litrpg.mob.DungeonMob;
import com.andrey.litrpg.mob.MobRank;
import com.andrey.litrpg.mob.ModMobs;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.skill.ClassRegistry;
import com.andrey.litrpg.skill.PlayerClass;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillService;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Стенд классов (фаза 12): каждый класс 90 с жмёт свои навыки по готовности против настоящих монстров — одной цели
 * и толпы из пяти. Лекарь в это время держит раненого союзника и себя, танк принимает удар в «единицу» раз в секунду.
 * Цифры — из настоящего кода навыков (объекты, помощники, метки, урон со временем), а не из догадок.
 * <p>Запуск: {@code gradlew runGameTestServer -Pbench} → {@code build/reports/bench/<класс>_<ур>.txt};
 * сводку собирает {@code BalanceSimTest} (отчёт {@code build/reports/classes.md}). Без {@code -Pbench} стенд не запускается.
 */
@GameTestHolder(LitRPG.MOD_ID)
public final class ClassBench {
    static final int SECONDS = 90;
    static final int[] LEVELS = {20, 45};

    private ClassBench() {
    }

    @GameTestGenerator
    public static List<TestFunction> bench() {
        List<TestFunction> out = new ArrayList<>();
        if (!Boolean.getBoolean("litrpg.bench")) return out;
        String only = System.getProperty("litrpg.bench.only", "");
        for (PlayerClass c : ClassRegistry.all()) {
            if (!only.isEmpty() && !only.equals(c.id())) continue;
            for (int level : LEVELS) {
                out.add(new TestFunction("bench_" + level, "bench_" + c.id() + "_" + level, "litrpg:arena",
                        SECONDS * 20 + 200, 0, true, h -> run(h, c.role(), c.id(), level)));
            }
        }
        // До 10 уровня класса нет — набор роли.
        for (Role r : only.isEmpty() ? new Role[]{Role.TANK, Role.DAMAGE, Role.SUPPORT} : new Role[0]) {
            String id = "kit_" + r.name().toLowerCase(Locale.ROOT);
            out.add(new TestFunction("bench_6", "bench_" + id + "_6", "litrpg:arena", SECONDS * 20 + 200, 0, true,
                    h -> run(h, r, id, 6)));
        }
        return out;
    }

    private static DungeonMob dummy(GameTestHelper h, int level, int x, int z) {
        DungeonMob m = h.spawn(ModMobs.TYPES.get(Bestiary.CRYPT_WARRIOR.id()).get(), new BlockPos(x, 2, z));
        m.setup(level, MobRank.NORMAL);
        m.setNoAi(true);
        var hp = m.getAttribute(Attributes.MAX_HEALTH);
        // Здоровье — как у хранителя (80 «единиц»): навыки «процентом от здоровья» считаются честно.
        if (hp != null) hp.setBaseValue(80 * BossDamage.unit(level));
        m.setHealth(m.getMaxHealth());
        return m;
    }

    private static double shields(LivingEntity e) {
        if (!e.hasData(com.andrey.litrpg.core.ModAttachments.STATUSES)) return 0;
        long now = e.level().getGameTime();
        return StatusService.list(e).all().stream().filter(s -> s.type() == StatusType.SHIELD && !s.expired(now))
                .mapToDouble(s -> s.magnitude()).sum();
    }

    private static void run(GameTestHelper h, Role role, String classId, int level) {
        ServerPlayer p = TestPlayers.create(h), ally = TestPlayers.create(h);
        PlayerService.setRole(p, role);
        PlayerService.setLevel(p, level);
        if (ClassRegistry.get(classId) != null) PlayerService.setClass(p, classId);
        PlayerService.setRole(ally, Role.DAMAGE);
        PlayerService.setLevel(ally, level);
        DungeonMob main = dummy(h, level, 6, 4);
        List<DungeonMob> crowd = new ArrayList<>(List.of(main, dummy(h, level, 6, 6), dummy(h, level, 7, 3),
                dummy(h, level, 5, 2), dummy(h, level, 7, 5)));
        com.andrey.litrpg.party.PartyService.form(List.of(p, ally));
        HealService.select(p, ally.getUUID());
        double unit = BossDamage.unit(level);
        var w = Balance.get(Balance.GEAR).weapons();
        double auto = w.meleeBase() + w.meleePerLevel() * (level - 1);
        List<Skill> kit = ClassRegistry.loadout(role, classId, level);
        double[] acc = new double[7]; // 0 урон по цели, 1 урон по всем, 2 лечение, 3 щиты, 4 урон по танку, 5 «единиц» по танку
        int[] casts = new int[kit.size()];
        float[] mobHp = new float[crowd.size()];
        float[] lastHp = {0, 0};
        double[] lastShield = {0};
        int[] tick = {-20};
        h.onEachTick(() -> {
            int t = tick[0]++;
            if (t == -1) {
                p.teleportTo(main.getX() - 2.2, main.getY(), main.getZ());
                ally.teleportTo(main.getX() - 2.6, main.getY(), main.getZ() + 1.2);
                for (int i = 0; i < crowd.size(); i++) mobHp[i] = crowd.get(i).getHealth();
            }
            if (t < 0) return;
            if (t == SECONDS * 20) {
                write(classId, role, level, acc, unit, kit, casts);
                com.andrey.litrpg.party.PartyService.leave(p);
                com.andrey.litrpg.party.PartyService.leave(ally);
                h.succeed();
                return;
            }
            if (t > SECONDS * 20) return;
            if (!System.getProperty("litrpg.bench.only", "").isEmpty() && t % 40 == 0) {
                LitRPG.LOG.info("[стенд] t={} объектов {} союзник {}/{} сам {}/{} лечение {}", t,
                        h.getLevel().getEntitiesOfClass(com.andrey.litrpg.skill.prop.ClassProp.class, p.getBoundingBox().inflate(20)).stream()
                                .map(e -> e.kind().id() + "@" + String.format(Locale.ROOT, "%.1f", e.distanceTo(ally))).toList(),
                        ally.getHealth(), ally.getMaxHealth(), p.getHealth(), p.getMaxHealth(), acc[2]);
            }
            // Урон за прошедший тик (объекты, помощники, урон со временем — всё, что успело ударить).
            for (int i = 0; i < crowd.size(); i++) {
                DungeonMob m = crowd.get(i);
                double d = Math.max(0, mobHp[i] - m.getHealth());
                acc[1] += d;
                if (m == main) acc[0] += d;
                if (m.getHealth() < m.getMaxHealth() * 0.4f) m.setHealth(m.getMaxHealth());
                mobHp[i] = m.getHealth();
            }
            // Лечение за прошедший тик (всё, что подняло здоровье союзника и своё).
            if (t > 0) acc[2] += Math.max(0, ally.getHealth() - lastHp[0]) + Math.max(0, p.getHealth() - lastHp[1]);
            double sh = shields(ally) + shields(p);
            acc[3] += Math.max(0, sh - lastShield[0]);
            lastShield[0] = sh;
            if (role != Role.TANK) {
                // Союзника и себя всё время бьют (0,4 «единицы» в секунду каждого) — лечить есть что, а «откат»
                // хронолога видит настоящую историю здоровья. Упал ниже 15% — подняли до 60% (это не лечение).
                if (t % 10 == 0) {
                    for (ServerPlayer x : List.of(ally, p)) {
                        x.setHealth((float) Math.max(1, x.getHealth() - 0.2 * unit));
                        if (x.getHealth() < x.getMaxHealth() * 0.15f) x.setHealth(x.getMaxHealth() * 0.6f);
                    }
                }
            } else if (t % 20 == 10) {
                // Танк: раз в секунду удар в «единицу» от монстра.
                p.setHealth(p.getMaxHealth());
                float before = p.getHealth();
                p.invulnerableTime = 0;
                p.hurt(main.damageSources().mobAttack(main), (float) unit);
                acc[4] += before - p.getHealth();
                acc[5] += 1;
                p.setHealth(p.getMaxHealth());
            }
            lastHp[0] = ally.getHealth();
            lastHp[1] = p.getHealth();
            StatusService.tick(p);
            StatusService.tick(ally);
            SkillService.tick(p);
            // Обычный удар оружием раз в 0,8 с — у всех одинаковый (ярость и ци копятся и от него).
            if (t % 16 == 0) DamageService.deal(p, p, main, auto, WeaponKind.MELEE);
            if (SkillService.state(p).casting() == null) {
                for (int i = kit.size() - 1; i >= 0; i--) {
                    Skill sk = kit.get(i);
                    if (sk.passive()) continue;
                    // Прицел как у живого игрока: по врагу — в грудь, наземное лечение — под ноги союзнику,
                    // наземный удар — под ноги врагу.
                    if (sk.targeting() == com.andrey.litrpg.skill.Targeting.GROUND) {
                        p.lookAt(EntityAnchorArgument.Anchor.EYES, role == Role.SUPPORT
                                ? ally.position().add(0.6, 0, -0.6) : main.position());
                    } else {
                        p.lookAt(EntityAnchorArgument.Anchor.EYES, main.position().add(0, 1.0, 0));
                    }
                    if (SkillService.use(p, i)) {
                        casts[i]++;
                        break;
                    }
                }
            }
        });
    }

    private static void write(String classId, Role role, int level, double[] acc, double unit, List<Skill> kit, int[] casts) {
        double one = acc[0], total = acc[1];
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "class=%s%nrole=%s%nlevel=%d%nunit=%.2f%n", classId, role.name(), level, unit));
        sb.append(String.format(Locale.ROOT, "dps_one=%.2f%ndps_all=%.2f%nhps=%.2f%nsps=%.2f%n", one / SECONDS, total / SECONDS,
                acc[2] / SECONDS, acc[3] / SECONDS));
        sb.append(String.format(Locale.ROOT, "taken=%.4f%n", acc[5] > 0 ? acc[4] / (acc[5] * unit) : 0));
        StringBuilder cs = new StringBuilder();
        for (int i = 0; i < kit.size(); i++) cs.append(kit.get(i).id).append(':').append(casts[i]).append(' ');
        sb.append("casts=").append(cs.toString().trim()).append('\n');
        Path dir = Path.of(System.getProperty("litrpg.root", "."), "build/reports/bench");
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(classId + "_" + level + ".txt"), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LitRPG.LOG.error("[LitRPG] стенд: не записать {}", classId, e);
        }
    }
}
