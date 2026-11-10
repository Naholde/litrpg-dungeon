package com.andrey.litrpg.run;

import com.andrey.litrpg.combat.entity.TrainingDummy;
import com.andrey.litrpg.core.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Манекены Двора испытаний в Преддверии (обновление 14, пункт 16): стоят там, где велит схема ({@code lobby.dummies}).
 * Раз в несколько секунд, если чанк загружен: нет манекена у точки — ставится новый, лишние убираются.
 * Метка {@link #TAG} отличает их от манекенов, поставленных командой {@code /litrpg dummy}.
 */
public final class LobbyDummies {
    public static final String TAG = "litrpg_lobby_dummy";
    private static final double NEAR = 1.5;

    private LobbyDummies() {
    }

    /** Поставить недостающих и убрать лишних. Возвращает, сколько манекенов поставлено. */
    public static int ensure(ServerLevel level, List<Layout.Dummy> spots) {
        int placed = 0;
        for (Layout.Dummy d : spots) {
            if (!level.isLoaded(BlockPos.containing(d.at()))) continue;
            AABB around = new AABB(d.at(), d.at()).inflate(NEAR, 2, NEAR);
            List<TrainingDummy> here = level.getEntitiesOfClass(TrainingDummy.class, around, e -> e.getTags().contains(TAG));
            for (int i = 1; i < here.size(); i++) here.get(i).discard();
            if (!here.isEmpty()) {
                TrainingDummy t = here.get(0);
                if (t.position().distanceToSqr(d.at()) > 0.04) t.moveTo(d.at().x, d.at().y, d.at().z, d.yaw(), 0);
                continue;
            }
            TrainingDummy t = ModEntities.TRAINING_DUMMY.get().create(level);
            if (t == null) continue;
            t.moveTo(d.at().x, d.at().y, d.at().z, d.yaw(), 0);
            t.setYHeadRot(d.yaw());
            t.setYBodyRot(d.yaw());
            t.setAggressive(d.aggressive());
            t.addTag(TAG);
            level.addFreshEntity(t);
            placed++;
        }
        return placed;
    }
}
