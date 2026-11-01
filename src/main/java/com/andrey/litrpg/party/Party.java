package com.andrey.litrpg.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Отряд: до 6 игроков, порядок важен (Ctrl+1…6 в рамках группы). */
public final class Party {
    public static final int MAX = 6;

    final UUID id = UUID.randomUUID();
    final List<UUID> members = new ArrayList<>();

    public UUID id() {
        return id;
    }

    public List<UUID> members() {
        return Collections.unmodifiableList(members);
    }

    public boolean contains(UUID who) {
        return members.contains(who);
    }
}
