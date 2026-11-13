package com.andrey.litrpg.run;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.RunBalance;
import net.minecraft.server.MinecraftServer;

/**
 * Сложность забега (обновление 15, решение владельца 13.11.2026). Выбирается в лобби перед стартом и не меняется до
 * конца забега. Цифры — {@code balance/run.json → difficulty}. Вне забега (лобби, автотесты, полигон) множители
 * нейтральные: здоровье и урон ×1, находка +0 — так ведут себя все проверки баланса.
 */
public enum Difficulty {
    NORMAL("normal", "Обычная", 0xFF7FD3FF, "Хранители и монстры слабее: для первого знакомства с подземельем."),
    HEROIC("heroic", "Героическая", 0xFFFFB347, "Полная сила хранителей. Добыча лучше, свои рекорды и титул.");

    public final String id;
    public final String title;
    public final int color;
    public final String tagline;

    Difficulty(String id, String title, int color, String tagline) {
        this.id = id;
        this.title = title;
        this.color = color;
        this.tagline = tagline;
    }

    public static Difficulty byId(String id) {
        for (Difficulty d : values()) if (d.id.equals(id)) return d;
        return NORMAL;
    }

    public RunBalance.Mode mode() {
        RunBalance.Difficulty d = Balance.get(Balance.RUN).difficulty();
        return this == HEROIC ? d.heroic() : d.normal();
    }

    private static final RunBalance.Mode NEUTRAL = new RunBalance.Mode(1, 1, 0);

    /** Множители идущего забега; без забега — нейтральные. */
    public static RunBalance.Mode active(MinecraftServer server) {
        if (server == null || server instanceof net.minecraft.gametest.framework.GameTestServer && !RunService.testing) return NEUTRAL;
        RunState s = RunService.state(server);
        return s.active(Layout.get().lastStage()) ? s.difficulty.mode() : NEUTRAL;
    }

    /** Сложность идущего забега (или null, если забега нет). */
    public static Difficulty current(MinecraftServer server) {
        if (server == null) return null;
        RunState s = RunService.state(server);
        return s.active(Layout.get().lastStage()) ? s.difficulty : null;
    }
}
