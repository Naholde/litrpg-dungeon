package com.andrey.litrpg;

import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.gear.Trophies;
import com.andrey.litrpg.player.Role;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** У каждого хранителя подземелья — четыре трофея: танку, бойцу, поддержке и украшение любому; имена не повторяются. */
class TrophiesTest {
    @Test
    void everyGuardianHasFourTrophies() {
        Set<String> names = new HashSet<>();
        for (BossRegistry.Type t : BossRegistry.all()) {
            if (t.id().equals("trial_warden") || t.id().equals("vessa")) continue;
            var list = Trophies.of(t.id());
            assertEquals(4, list.size(), t.id() + ": трофеев не 4");
            EnumSet<Role> roles = EnumSet.noneOf(Role.class);
            for (var tr : list) {
                roles.add(tr.role());
                assertTrue(names.add(tr.name()), "повтор имени трофея: " + tr.name());
                assertTrue(tr.a() != tr.b(), tr.name() + ": одинаковые фирменные свойства");
            }
            assertEquals(EnumSet.of(Role.TANK, Role.DAMAGE, Role.SUPPORT, Role.NONE), roles, t.id() + ": не на все роли");
        }
    }
}
