package com.andrey.litrpg.boss;

import com.andrey.litrpg.core.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Полигон: круглая арена для проверки хранителей до того, как появится подземелье.
 * Пол с рунным кругом, колонны по краю, светильники; в центре — Испытатель Системы.
 */
public final class Polygon {
    public static final int RADIUS = 18;

    private Polygon() {
    }

    public static TrialWarden build(ServerLevel level, BlockPos center) {
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState ring = Blocks.CYAN_GLAZED_TERRACOTTA.defaultBlockState();
        BlockState rim = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState pillar = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        int r = RADIUS + 3;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + 0.5) continue;
                BlockPos base = center.offset(dx, -1, dz);
                level.setBlock(base, d > RADIUS + 0.5 ? rim : Math.abs(d - RADIUS * 0.6) < 0.6 ? ring : floor, 3);
                for (int y = 0; y <= 12; y++) level.setBlock(base.above(1 + y), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            BlockPos p = center.offset((int) Math.round(Math.cos(a) * (RADIUS + 1.5)), 0, (int) Math.round(Math.sin(a) * (RADIUS + 1.5)));
            for (int y = 0; y < 6; y++) level.setBlock(p.above(y), pillar, 3);
            level.setBlock(p.above(6), Blocks.SEA_LANTERN.defaultBlockState(), 3);
        }
        level.setBlock(center.below(), Blocks.LODESTONE.defaultBlockState(), 3);
        TrialWarden boss = ModEntities.TRIAL_WARDEN.get().create(level);
        if (boss == null) return null;
        boss.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0, 0);
        boss.setArena(boss.position(), RADIUS);
        level.addFreshEntity(boss);
        return boss;
    }
}
