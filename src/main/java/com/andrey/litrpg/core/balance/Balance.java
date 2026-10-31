package com.andrey.litrpg.core.balance;

import com.andrey.litrpg.LitRPG;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Все цифры баланса. Механика живёт в коде, цифры — здесь, в json из {@code data/litrpg/balance/}.
 * {@code /reload} перечитывает файлы без пересборки, сервер рассылает их клиентам.
 */
public final class Balance {
    private static final Map<String, BalanceKey<?>> KEYS = new LinkedHashMap<>();
    private static final Map<BalanceKey<?>, Object> VALUES = new ConcurrentHashMap<>();
    private static final Set<String> FROM_DATA = ConcurrentHashMap.newKeySet();
    private static volatile List<String> errors = List.of();

    public static final BalanceKey<Progression> PROGRESSION = register("progression", Progression.CODEC, Progression.DEFAULT);

    private Balance() {
    }

    /** Вызывается из конструктора мода, чтобы статические разделы зарегистрировались до загрузки данных. */
    public static void bootstrap() {
    }

    public static <T> BalanceKey<T> register(String name, com.mojang.serialization.MapCodec<T> codec, T defaults) {
        BalanceKey<T> key = new BalanceKey<>(name, codec, defaults);
        if (KEYS.putIfAbsent(name, key) != null) throw new IllegalStateException("Раздел баланса уже есть: " + name);
        return key;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(BalanceKey<T> key) {
        Object v = VALUES.get(key);
        return v != null ? (T) v : key.defaults();
    }

    public static Collection<BalanceKey<?>> keys() {
        return Collections.unmodifiableCollection(KEYS.values());
    }

    public static BalanceKey<?> key(String name) {
        return KEYS.get(name);
    }

    /** Раздел прочитан из файла без ошибок (а не взят по умолчанию). */
    public static boolean loadedFromData(BalanceKey<?> key) {
        return FROM_DATA.contains(key.name());
    }

    public static List<String> errors() {
        return errors;
    }

    /**
     * Применить json всех разделов (с диска на сервере или из пакета на клиенте).
     * Сломанный раздел оставляет значения по умолчанию и попадает в {@link #errors()}.
     */
    public static List<String> apply(Map<String, JsonElement> files) {
        List<String> problems = new ArrayList<>();
        FROM_DATA.clear();
        for (BalanceKey<?> key : KEYS.values()) {
            JsonElement json = files.get(key.name());
            if (json == null) {
                problems.add(key.name() + ": файл не найден, взяты значения по умолчанию");
                VALUES.remove(key);
                continue;
            }
            applyOne(key, json, problems);
        }
        for (String name : files.keySet()) {
            if (!KEYS.containsKey(name)) problems.add(name + ": такого раздела баланса нет (опечатка в имени файла?)");
        }
        errors = List.copyOf(problems);
        for (String p : problems) LitRPG.LOG.error("[LitRPG] Баланс: {}", p);
        return errors;
    }

    private static <T> void applyOne(BalanceKey<T> key, JsonElement json, List<String> problems) {
        if (json.isJsonObject()) {
            Set<String> known = key.codec().keys(JsonOps.INSTANCE)
                    .map(JsonElement::getAsString).collect(Collectors.toSet());
            for (String field : json.getAsJsonObject().keySet()) {
                if (!known.contains(field)) problems.add(key.name() + ": лишнее поле «" + field + "» (опечатка?)");
            }
        }
        var result = key.codec().codec().parse(JsonOps.INSTANCE, json);
        result.resultOrPartial(msg -> problems.add(key.name() + ": " + msg)).ifPresentOrElse(value -> {
            VALUES.put(key, value);
            if (result.error().isEmpty()) FROM_DATA.add(key.name());
        }, () -> VALUES.remove(key));
    }

    /** Текущие значения всех разделов в json — для рассылки клиентам и команды {@code /litrpg balance}. */
    public static Map<String, String> snapshot() {
        Map<String, String> out = new LinkedHashMap<>();
        for (BalanceKey<?> key : KEYS.values()) out.put(key.name(), encode(key).toString());
        return out;
    }

    public static <T> JsonElement encode(BalanceKey<T> key) {
        return key.codec().codec().encodeStart(JsonOps.INSTANCE, get(key)).result().orElseGet(JsonObject::new);
    }
}
