package com.andrey.litrpg.core;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.world.block.RiftBlock;
import com.andrey.litrpg.world.block.SealBlock;
import com.andrey.litrpg.world.block.SealedGateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Блоки подземелья. Все неразрушимы: мир защищён, менять его может только прохождение. */
public final class ModBlocks {
    private static final DeferredRegister.Blocks REG = DeferredRegister.createBlocks(LitRPG.MOD_ID);

    private static BlockBehaviour.Properties fixed() {
        return BlockBehaviour.Properties.of().strength(-1, 3_600_000).noLootTable().pushReaction(PushReaction.BLOCK);
    }

    /** Печать в центре преддверия: ПКМ — начать прохождение. */
    public static final DeferredBlock<SealBlock> SEAL = REG.register("seal",
            () -> new SealBlock(fixed().mapColor(MapColor.GOLD).lightLevel(s -> 12).sound(SoundType.AMETHYST)));

    /** Печать-преграда: открывается, когда выполнены условия (отряды, хранители). */
    public static final DeferredBlock<SealedGateBlock> SEALED_GATE = REG.register("sealed_gate",
            () -> new SealedGateBlock(fixed().mapColor(MapColor.COLOR_PURPLE).lightLevel(s -> 9).noOcclusion()
                    .sound(SoundType.AMETHYST).isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));

    /** Разлом на следующий этап: открывается после хранителей этапа. */
    public static final DeferredBlock<RiftBlock> RIFT = REG.register("rift",
            () -> new RiftBlock(fixed().mapColor(MapColor.COLOR_MAGENTA).noCollission().noOcclusion()
                    .lightLevel(s -> s.getValue(RiftBlock.ACTIVE) ? 15 : 4).sound(SoundType.AMETHYST)));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        REG.register(modBus);
    }
}
