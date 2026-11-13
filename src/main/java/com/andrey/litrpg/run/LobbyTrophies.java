package com.andrey.litrpg.run;

import com.andrey.litrpg.LitRPG;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Зал трофеев (обновление 15): на пьедестале каждого хранителя — его чучело, как только хранителя впервые победили
 * на сервере (уменьшенная модель, без боя — витрина), и табличка «кто и когда». Не побеждённый — пустой пьедестал
 * с «???». Раз в 2 секунды, только в загруженных чанках.
 */
public final class LobbyTrophies {
    static final String TAG = "litrpg_trophy";

    private LobbyTrophies() {
    }

    public static void tick(MinecraftServer server) {
        ServerLevel level = RunService.dungeon(server);
        if (level == null) return;
        Records r = Records.get(server);
        for (Layout.TrophySpot t : Layout.get().lobby().trophies()) {
            if (!level.isLoaded(BlockPos.containing(t.at()))) continue;
            Records.Trophy won = r.trophies().get(t.boss());
            String tag = TAG + ":" + t.boss();
            List<Entity> here = level.getEntities((Entity) null, new AABB(t.at(), t.at()).inflate(2, 4, 2),
                    e -> e.getTags().contains(tag) && !(e instanceof net.minecraft.world.entity.Display));
            if (won != null && here.isEmpty()) statue(level, t, tag);
            if (won == null) here.forEach(Entity::discard);
            for (int i = 1; i < here.size(); i++) here.get(i).discard();
            LobbyBoards.place(level, tag + ":plaque", t.at().add(0, 3.4, 0), t.yaw(), 0.55f, 160, false,
                    plaque(t, won), won == null ? 0x60101010 : 0x90182436);
        }
    }

    private static void statue(ServerLevel level, Layout.TrophySpot t, String tag) {
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(LitRPG.id(t.boss())).orElse(null);
        if (type == null) return;
        Entity e = type.create(level);
        if (e == null) return;
        e.moveTo(t.at().x, t.at().y, t.at().z, t.yaw(), 0);
        e.setYHeadRot(t.yaw());
        e.getPersistentData().putBoolean(com.andrey.litrpg.dev.Showcase.TAG, true);   // витрина: без боя и без ИИ
        e.addTag(tag);
        e.setInvulnerable(true);
        e.setSilent(true);
        if (e instanceof Mob m) {
            m.setNoAi(true);
            m.setPersistenceRequired();
            m.setYBodyRot(t.yaw());
        }
        if (e instanceof LivingEntity le) {
            var scale = le.getAttribute(Attributes.SCALE);
            if (scale != null) scale.setBaseValue(t.scale());
        }
        level.addFreshEntity(e);
    }

    private static Component plaque(Layout.TrophySpot t, Records.Trophy won) {
        MutableComponent c = Component.empty();
        if (won == null) {
            c.append(Component.literal("???\n").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
            c.append(Component.literal("Этап " + t.stage() + " · ещё не побеждён").withStyle(ChatFormatting.GRAY));
            return c;
        }
        Difficulty d = Difficulty.byId(won.difficulty());
        c.append(Component.literal(LobbyBoards.bossName(t.boss()) + "\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        c.append(Component.literal("Впервые повержен " + won.date() + "\n").withStyle(ChatFormatting.GRAY));
        c.append(Component.literal(String.join(", ", won.names()) + "\n").withStyle(ChatFormatting.WHITE));
        c.append(Component.literal(d.title).withColor(d.color & 0xFFFFFF));
        return c;
    }
}
