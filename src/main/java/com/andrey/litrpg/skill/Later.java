package com.andrey.litrpg.skill;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Отложенные действия: «через столько тиков сделать вот это». Нужны всем, у кого удар приходит не сразу —
 * метеор с разметкой, мина, приговор жнеца, взрыв двойника.
 * <p>Живут только в текущей игре: перезапуск сервера отложенные действия забывает.
 */
public final class Later {
    private record Task(ServerLevel level, long at, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private Later() {
    }

    public static void run(ServerLevel level, int ticks, Runnable action) {
        TASKS.add(new Task(level, level.getGameTime() + Math.max(1, ticks), action));
    }

    /** Каждый тик мира: выполнить то, чему пришло время. */
    public static void tick(ServerLevel level) {
        if (TASKS.isEmpty()) return;
        long now = level.getGameTime();
        List<Runnable> due = new ArrayList<>();
        TASKS.removeIf(t -> {
            if (t.level() != level || now < t.at()) return false;
            due.add(t.action());
            return true;
        });
        // Действие может поставить новое отложенное — список уже без него, поэтому порядок не ломается.
        for (Runnable r : due) r.run();
    }

    public static void clear() {
        TASKS.clear();
    }
}
