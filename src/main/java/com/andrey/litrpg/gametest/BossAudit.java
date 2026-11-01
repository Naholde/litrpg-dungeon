package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Проверка «от каждого удара хранителя можно уйти» (фаза 12): каждый навык каждого хранителя запускается по разу,
 * а {@link Telegraph#audit} записывает его разметку — фигуру, сколько бежать до края и сколько на это времени.
 * Пишет {@code build/reports/bench/boss_<id>.txt}; вывод — {@code BalanceSimTest} (отчёт balance.md).
 * Запуск вместе со стендом классов: {@code gradlew runGameTestServer -Pbench}.
 */
@GameTestHolder(LitRPG.MOD_ID)
public final class BossAudit {
    private static final Map<BossEntity, List<String>> SEEN = new ConcurrentHashMap<>();

    private BossAudit() {
    }

    @GameTestGenerator
    public static List<TestFunction> audit() {
        List<TestFunction> out = new ArrayList<>();
        if (!Boolean.getBoolean("litrpg.bench") || !System.getProperty("litrpg.bench.only", "").isEmpty()) return out;
        Telegraph.audit = (c, line) -> SEEN.computeIfAbsent(c.boss(), b -> new ArrayList<>()).add(c.ability.key() + ";" + line);
        for (var holder : ModEntities.BOSSES) {
            String id = holder.getId().getPath();
            out.add(new TestFunction("audit", "audit_" + id, "litrpg:arena", 1400, 0, true, h -> run(h, holder.get(), id)));
        }
        return out;
    }

    private static void run(GameTestHelper h, EntityType<? extends BossEntity> type, String id) {
        BossEntity b = h.spawn(type, new BlockPos(4, 2, 4));
        b.setArena(b.position(), 14);
        Role[] roles = {Role.TANK, Role.SUPPORT, Role.DAMAGE};
        double[][] at = {{2.5, 4.5}, {7.5, 7.5}, {7.5, 1.5}};
        for (int i = 0; i < 3; i++) {
            ServerPlayer p = TestPlayers.create(h);
            Vec3 v = h.absoluteVec(new Vec3(at[i][0], 2, at[i][1]));
            p.teleportTo(h.getLevel(), v.x, v.y, v.z, 0, 0);
            PlayerService.setRole(p, roles[i]);
            PlayerService.setLevel(p, 30);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                    2000, 4, false, false));
        }
        List<String> errors = new ArrayList<>();
        h.runAfterDelay(10, () -> {
            var list = b.debugAbilities();
            for (int i = 0; i < list.size(); i++) {
                var a = list.get(i);
                h.runAfterDelay(i * 80L, () -> {
                    try {
                        if (b.isAlive()) b.debugCast(a);
                    } catch (RuntimeException e) {
                        errors.add(a.key() + ";ошибка;" + e);
                    }
                });
            }
            h.runAfterDelay(list.size() * 80L + 40, () -> {
                StringBuilder sb = new StringBuilder();
                sb.append("boss=").append(id).append('\n');
                for (var a : list) {
                    sb.append("ability=").append(a.key()).append(';').append(a.name()).append(';')
                            .append(Math.round(b.p(a.key() + "_cast") * 20)).append('\n');
                }
                for (String l : SEEN.getOrDefault(b, List.of())) sb.append("mark=").append(l).append('\n');
                for (String l : errors) sb.append("error=").append(l).append('\n');
                try {
                    Path dir = Path.of(System.getProperty("litrpg.root", "."), "build/reports/bench");
                    Files.createDirectories(dir);
                    Files.writeString(dir.resolve("boss_" + id + ".txt"), sb.toString(), StandardCharsets.UTF_8);
                } catch (java.io.IOException e) {
                    LitRPG.LOG.error("[LitRPG] проверка хранителя {}", id, e);
                }
                SEEN.remove(b);
                if (b.isAlive()) b.discard();
                h.succeed();
            });
        });
    }
}
