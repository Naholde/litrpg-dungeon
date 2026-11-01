package com.andrey.litrpg.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Разлом на следующий этап. Пока хранители этапа живы — тусклая трещина; потом вспыхивает. Переход — когда
 * весь живой отряд собрался у разлома (решает прохождение, не блок).
 */
public class RiftBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public RiftBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(ACTIVE);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState other, Direction dir) {
        return other.is(this) || super.skipRendering(state, other, dir);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        boolean on = state.getValue(ACTIVE);
        for (int i = 0; i < (on ? 3 : 1); i++) {
            if (!on && r.nextInt(3) != 0) continue;
            level.addParticle(on ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL, pos.getX() + r.nextDouble(),
                    pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), (r.nextDouble() - 0.5) * 0.3, 0.05, (r.nextDouble() - 0.5) * 0.3);
        }
    }
}
