package com.andrey.litrpg;

import com.andrey.litrpg.anim.Anim;
import com.andrey.litrpg.anim.AnimPose;
import com.andrey.litrpg.anim.Anims;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Фаза «Визуал»: у каждого навыка своё движение игрока, и все движения считаются без сбоев. */
class PlayerAnimTest {

    /** Нет движения — навык будет махать запасным замахом, а это заметно. */
    @Test
    void everySkillHasItsOwnMove() {
        List<String> missing = new ArrayList<>();
        for (Skill s : SkillRegistry.all()) if (!Anims.has(s.id)) missing.add(s.id);
        assertTrue(missing.isEmpty(), "нет движений (Anims<Роль>): " + missing);
    }

    /** Движение с именем не от навыка никогда не сыграет — значит, в имени опечатка. */
    @Test
    void everyMoveBelongsToSkillOrIsCommon() {
        Set<String> ids = new HashSet<>();
        for (Skill s : SkillRegistry.all()) ids.add(s.id);
        List<String> orphans = new ArrayList<>();
        for (Anim a : Anims.all()) {
            if (a.id.startsWith("common.")) continue;
            String base = a.id.endsWith(".cast") ? a.id.substring(0, a.id.length() - 5) : a.id;
            if (!ids.contains(base)) orphans.add(a.id);
        }
        assertTrue(orphans.isEmpty(), "движения ни от какого навыка: " + orphans);
    }

    /** Поза чтения нужна только тем навыкам, которые читаются. */
    @Test
    void castMovesOnlyForSkillsWithCastTime() {
        for (Anim a : Anims.all()) {
            if (!a.id.endsWith(".cast") || a.id.startsWith("common.")) continue;
            String base = a.id.substring(0, a.id.length() - 5);
            Skill s = SkillRegistry.all().stream().filter(x -> x.id.equals(base)).findFirst().orElseThrow();
            assertTrue(s.value("cast", 0) > 0, a.id + ": у навыка нет чтения, поза чтения не нужна");
        }
    }

    @Test
    void movesAreSane() {
        for (Anim a : Anims.all()) {
            assertTrue(a.ticks >= 8, a.id + ": движение короче 0,4 с не увидят");
            assertTrue(a.fade >= 1 && a.fade < a.ticks, a.id + ": сглаживание не по длине движения");
            assertFalse(a.tracks().isEmpty(), a.id + ": ни одной дорожки");
            for (Anim.Track t : a.tracks()) {
                int prev = -1;
                for (Anim.Key k : t.keys()) {
                    assertTrue(k.t() >= 0 && k.t() <= a.ticks, a.id + " / " + t.part() + ": кадр вне длины движения");
                    assertTrue(k.t() >= prev, a.id + " / " + t.part() + ": кадры не по времени");
                    prev = k.t();
                    boolean root = t.part() == Anim.Part.ROOT;
                    float limit = root ? 760 : 400;
                    assertTrue(Math.abs(k.x()) <= limit && Math.abs(k.y()) <= limit && Math.abs(k.z()) <= limit,
                            a.id + " / " + t.part() + ": поворот больше " + limit + "°");
                    float off = root ? 1.2f : 12;
                    assertTrue(Math.abs(k.ox()) <= off && Math.abs(k.oy()) <= off && Math.abs(k.oz()) <= off,
                            a.id + " / " + t.part() + ": слишком большой сдвиг");
                }
            }
        }
    }

    /** Поза снимается в любой момент движения: без NaN и без скачков силы. */
    @Test
    void everyMoveCanBeSampled() {
        AnimPose pose = new AnimPose();
        for (Anim a : Anims.all()) {
            for (float t = 0; t <= a.ticks + 1; t += 0.25f) {
                pose.sample(a, t, 1);
                assertTrue(pose.weight >= 0 && pose.weight <= 1, a.id + ": сила позы вне 0..1 на " + t);
                for (Anim.Part p : Anim.Part.values()) {
                    if (!pose.has(p)) continue;
                    assertTrue(Float.isFinite(pose.x(p)) && Float.isFinite(pose.y(p)) && Float.isFinite(pose.z(p))
                                    && Float.isFinite(pose.ox(p)) && Float.isFinite(pose.oy(p)) && Float.isFinite(pose.oz(p)),
                            a.id + " / " + p + ": не число на " + t);
                }
            }
            // Растянутое движение (долгое чтение) тоже должно считаться.
            pose.sample(a, a.ticks * 1.5f, 2);
            assertTrue(pose.weight >= 0 && pose.weight <= 1, a.id + ": сила позы вне 0..1 при растяжении");
        }
    }
}
