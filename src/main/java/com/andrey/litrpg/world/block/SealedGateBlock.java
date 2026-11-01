package com.andrey.litrpg.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Печать-преграда: светящаяся стена, сквозь которую видно, но не пройти. Открывает её только прохождение. */
public class SealedGateBlock extends Block {
    public SealedGateBlock(Properties props) {
        super(props);
    }

    /** Соседние блоки печати сливаются в одну стену (без граней между ними). */
    @Override
    protected boolean skipRendering(BlockState state, BlockState other, Direction dir) {
        return other.is(this) || super.skipRendering(state, other, dir);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(),
                    (r.nextDouble() - 0.5) * 0.5, -0.2, (r.nextDouble() - 0.5) * 0.5);
        }
    }
}
