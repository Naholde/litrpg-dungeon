package com.andrey.litrpg.client;

import com.andrey.litrpg.net.SkillSyncPayload;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillRegistry;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/** Таймеры панели на клиенте: сервер присылает «осталось», клиент считает дальше сам до следующего пакета. */
public final class ClientSkills {
    private record Timer(long until, int total) {
        double left(long now) {
            return Math.max(0, until - now);
        }
    }

    private static final Map<String, Timer> COOLDOWNS = new HashMap<>();
    private static Timer gcd = new Timer(0, 0), dash = new Timer(0, 0), cast = new Timer(0, 0);
    private static String castSkill = "";
    private static float resource;

    private ClientSkills() {
    }

    private static long now() {
        var mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0;
    }

    public static void update(SkillSyncPayload p) {
        long now = now();
        COOLDOWNS.clear();
        for (var s : p.slots()) COOLDOWNS.put(s.skill(), new Timer(now + s.left(), s.total()));
        gcd = new Timer(now + p.gcdLeft(), p.gcdTotal());
        dash = new Timer(now + p.dashLeft(), p.dashTotal());
        cast = new Timer(now + p.castLeft(), p.castTotal());
        castSkill = p.castSkill();
        resource = p.resource();
    }

    /** Доля оставшейся перезарядки навыка (0 — готов) и секунды. */
    public static double cooldownFraction(String skill, float partial) {
        Timer t = COOLDOWNS.get(skill);
        if (t == null || t.total <= 0) return 0;
        return Math.min(1, (t.left(now()) - partial) / t.total);
    }

    public static double cooldownSeconds(String skill, float partial) {
        Timer t = COOLDOWNS.get(skill);
        return t == null ? 0 : Math.max(0, (t.left(now()) - partial) / 20.0);
    }

    public static double gcdFraction(float partial) {
        return gcd.total <= 0 ? 0 : Math.max(0, Math.min(1, (gcd.left(now()) - partial) / gcd.total));
    }

    public static double dashFraction(float partial) {
        return dash.total <= 0 ? 0 : Math.max(0, Math.min(1, (dash.left(now()) - partial) / dash.total));
    }

    public static double dashSeconds(float partial) {
        return Math.max(0, (dash.left(now()) - partial) / 20.0);
    }

    /** Читаемый навык и доля прочитанного (null — не читает). */
    public static Skill casting() {
        return castSkill.isEmpty() || cast.left(now()) <= 0 ? null : SkillRegistry.get(castSkill);
    }

    public static double castProgress(float partial) {
        return cast.total <= 0 ? 0 : Math.min(1, 1 - (cast.left(now()) - partial) / cast.total);
    }

    public static double castSecondsLeft(float partial) {
        return Math.max(0, (cast.left(now()) - partial) / 20.0);
    }

    public static float resource() {
        return resource;
    }

    public static void reset() {
        COOLDOWNS.clear();
        gcd = dash = cast = new Timer(0, 0);
        castSkill = "";
        resource = 0;
    }
}
