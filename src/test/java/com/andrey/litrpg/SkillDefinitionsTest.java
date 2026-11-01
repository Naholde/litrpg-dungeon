package com.andrey.litrpg;

import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.skill.ClassOffers;
import com.andrey.litrpg.skill.ClassRegistry;
import com.andrey.litrpg.skill.PlayerClass;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillRegistry;
import com.andrey.litrpg.skill.SkillText;
import com.andrey.litrpg.skill.Style;
import com.andrey.litrpg.skill.Targeting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Правила, которые нельзя нарушать при добавлении классов и навыков. */
class SkillDefinitionsTest {

    @Test
    void everyTemplateFormatsWithoutUnknownKeys() {
        for (Skill s : SkillRegistry.all()) {
            for (int rank = 0; rank <= 5; rank++) {
                String body = SkillText.body(s, rank, null);
                assertFalse(body.contains("{"), s.id + ": не подставлен ключ в «" + body + "»");
            }
            assertFalse(SkillText.tooltip(s, 0, com.andrey.litrpg.skill.Resource.NONE, null).isEmpty());
        }
    }

    @Test
    void targetedSkillsHaveRange() {
        for (Skill s : SkillRegistry.all()) {
            if (s.targeting() == Targeting.ENEMY || s.targeting() == Targeting.ALLY || s.targeting() == Targeting.GROUND) {
                assertTrue(s.params().containsKey("range"), s.id + ": навыку с целью нужна дальность (range)");
            }
        }
    }

    @Test
    void classesFollowTheRules() {
        for (PlayerClass c : ClassRegistry.all()) {
            assertTrue(c.skills().size() >= 5 && c.skills().size() <= 6, c.id() + ": у класса 5–6 способностей");
            Set<String> names = new HashSet<>();
            int prev = 0, atStart = 0;
            for (var u : c.skills()) {
                assertTrue(names.add(u.skill().name), c.id() + ": повторяется название навыка «" + u.skill().name + "»");
                assertTrue(u.level() >= 10 && u.level() <= 48, c.id() + ": навыки открываются с 10 по 48 уровень");
                assertTrue(u.level() >= prev, c.id() + ": навыки по возрастанию уровня");
                prev = u.level();
                if (u.level() == 10) atStart++;
            }
            assertTrue(atStart >= 3, c.id() + ": на 10 уровне класс даёт хотя бы 3 навыка (не меньше, чем было у роли)");
            if (c.role() == Role.TANK) {
                assertTrue(c.skills().stream().anyMatch(u -> u.skill() == SkillRegistry.TAUNT),
                        c.id() + ": у танка обязательно есть провокация");
            }
            assertFalse(c.style().isEmpty(), c.id() + ": нужен стиль игры для подбора");
        }
        for (Role r : new Role[]{Role.TANK, Role.DAMAGE, Role.SUPPORT}) {
            assertEquals(3, ClassRegistry.ROLE_KITS.get(r).size(), r + ": у роли 3 начальных навыка");
            assertFalse(ClassRegistry.ofRole(r).isEmpty(), r + ": у роли есть хотя бы один класс");
        }
    }

    /** Решение владельца: отряд — 1 танк, 1 лекарь и 3 бойца, поэтому классов 6 / 18 / 6. */
    @Test
    void rosterMatchesOwnersDecision() {
        assertEquals(6, ClassRegistry.ofRole(Role.TANK).size(), "танков должно быть 6");
        assertEquals(18, ClassRegistry.ofRole(Role.DAMAGE).size(), "бойцов должно быть 18");
        assertEquals(6, ClassRegistry.ofRole(Role.SUPPORT).size(), "поддержки должно быть 6");
        assertEquals(30, ClassRegistry.all().size(), "всего классов 30");
    }

    /** Навык класса принадлежит только ему: общими бывают лишь навыки роли. */
    @Test
    void classSkillsAreNotShared() {
        Set<Skill> roleSkills = new HashSet<>();
        ClassRegistry.ROLE_KITS.values().forEach(roleSkills::addAll);
        Map<Skill, String> owner = new java.util.HashMap<>();
        for (PlayerClass c : ClassRegistry.all()) {
            for (var u : c.skills()) {
                if (roleSkills.contains(u.skill())) continue;
                String before = owner.put(u.skill(), c.id());
                assertTrue(before == null, u.skill().id + ": навык и у «" + before + "», и у «" + c.id() + "»");
            }
        }
    }

    /** У каждого класса шесть способностей — столько ячеек на панели. */
    @Test
    void everyClassHasSixSkills() {
        for (PlayerClass c : ClassRegistry.all()) {
            assertEquals(6, c.skills().size(), c.id() + ": должно быть 6 способностей");
        }
    }

    /** Пассивный навык не применяется с панели, но у него есть перезарядка. */
    @Test
    void passiveSkillsHaveCooldown() {
        for (Skill s : SkillRegistry.all()) {
            if (!s.passive()) continue;
            assertTrue(s.value("cooldown", 0) > 0, s.id + ": у пассивного навыка должна быть перезарядка");
            assertEquals(0, s.value("cast", 0), s.id + ": пассивный навык не читается");
        }
    }

    @Test
    void offersPreferMatchingStyle() {
        // Игрок-боец, который только и делал, что бил вблизи, — первым получает Берсерка.
        var offers = ClassOffers.offer(Role.DAMAGE, Map.of(Style.MELEE, 100), Set.of(), 3);
        assertEquals("berserker", offers.get(0).cls().id());
        // Занятый товарищем класс не предлагается.
        assertTrue(ClassOffers.offer(Role.DAMAGE, Map.of(), Set.of("berserker"), 3).stream()
                .noneMatch(s -> s.cls().id().equals("berserker")));
    }
}
