package com.andrey.litrpg.world.block;

import com.andrey.litrpg.run.RunService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Печать в лобби: ПКМ — окно сбора отряда (сложность, готовность, старт забега). */
public class SealBlock extends Block {
    public SealBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) com.andrey.litrpg.run.LobbyService.openFor(sp);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5 + r.nextGaussian() * 0.8, pos.getY() + 1.6 + r.nextDouble(),
                    pos.getZ() + 0.5 + r.nextGaussian() * 0.8, 0, -0.8, 0);
        }
        if (r.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0, 0.03, 0);
        }
    }
}
