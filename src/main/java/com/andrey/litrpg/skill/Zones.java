package com.andrey.litrpg.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Зоны на земле, которые действуют несколько секунд и срабатывают раз в {@code interval} тиков. */
public final class Zones {
    public static final class Zone {
        public final ServerLevel level;
        public final Vec3 center;
        public final double radius;
        public final UUID owner;
        final long until;
        final int interval;
        final Consumer<Zone> pulse;

        Zone(ServerLevel level, Vec3 center, double radius, UUID owner, long until, int interval, Consumer<Zone> pulse) {
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.owner = owner;
            this.until = until;
            this.interval = interval;
            this.pulse = pulse;
        }
    }

    private static final List<Zone> ACTIVE = new ArrayList<>();

    private Zones() {
    }

    public static void add(ServerLevel level, Vec3 center, double radius, UUID owner, int durationTicks, int interval,
                           Consumer<Zone> pulse) {
        ACTIVE.add(new Zone(level, center, radius, owner, level.getGameTime() + durationTicks, Math.max(1, interval), pulse));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        ACTIVE.removeIf(z -> z.level == level && now >= z.until);
        // Копия: срабатывание зоны может создать новую зону.
        for (Zone z : new ArrayList<>(ACTIVE)) {
            if (z.level == level && (z.until - now) % z.interval == 0) z.pulse.accept(z);
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
