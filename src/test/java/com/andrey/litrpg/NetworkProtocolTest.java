package com.andrey.litrpg;

import com.andrey.litrpg.net.Protocol;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * История протокола в {@code net_protocol.txt}: строка «версия: пакет, пакет…».
 * Изменился список пакетов — нужна новая строка с новой версией, иначе клиент и сервер разойдутся молча.
 */
class NetworkProtocolTest {
    private static final String FILE = "/net_protocol.txt";

    @Test
    void protocolVersionTracksPayloads() throws Exception {
        List<String> lines = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(FILE), FILE), StandardCharsets.UTF_8)).lines()
                .map(String::strip).filter(s -> !s.isEmpty() && !s.startsWith("#")).toList();
        Set<Integer> versions = new HashSet<>();
        for (String line : lines) {
            int v = Integer.parseInt(line.substring(0, line.indexOf(':')).strip());
            assertTrue(versions.add(v), "версия " + v + " встречается в net_protocol.txt дважды");
        }
        String last = lines.get(lines.size() - 1);
        String expected = Protocol.VERSION + ": " + String.join(", ", Protocol.PAYLOADS);
        assertEquals(expected, last,
                "Список пакетов изменился: подними Protocol.VERSION и допиши строку в net_protocol.txt");
    }
}
