package com.andrey.litrpg.net;

import java.util.List;

/**
 * Версия сетевого протокола и список пакетов. Без классов Minecraft — чтобы проверять обычным тестом.
 * Добавил/изменил пакет → подними {@link #VERSION} и допиши строку в {@code src/test/resources/net_protocol.txt},
 * иначе тест {@code NetworkProtocolTest} не даст собрать мод (клиент и сервер не должны расходиться молча).
 */
public final class Protocol {
    public static final int VERSION = 8;

    public static final List<String> PAYLOADS = List.of(
            "balance_sync",
            "player_sync",
            "choose_role",
            "allocate_stats",
            "popup",
            "mob_target",
            "party_sync",
            "select_target",
            "staff_cast",
            "status_sync",
            "fx",
            "skill_sync",
            "skill_action",
            "boss_sync",
            "shop",
            "shop_act"
    );

    private Protocol() {
    }
}
