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

/**
 * Все цифры баланса. Механика живёт в коде, цифры — здесь, в json из {@code data/litrpg/balance/}.
 * {@code /reload} перечитывает файлы без пересборки, сервер рассылает их клиентам.
 */
public final class Balance {
    private static final Map<String, BalanceKey<?>> KEYS = new LinkedHashMap<>();
    private static final Map<BalanceKey<?>, Object> VALUES = new ConcurrentHashMap<>();
    private static final Set<String> FROM_DATA = ConcurrentHashMap.newKeySet();
    private static volatile List<String> errors = List.of();
    private static volatile int generation;

    public static final BalanceKey<Progression> PROGRESSION = register("progression", Progression.CODEC, Progression.DEFAULT);
    public static final BalanceKey<CharacterBalance> CHARACTER = register("character", CharacterBalance.CODEC, CharacterBalance.DEFAULT);
    public static final BalanceKey<RolesBalance> ROLES = register("roles", RolesBalance.CODEC, RolesBalance.DEFAULT);
    public static final BalanceKey<CombatBalance> COMBAT = register("combat", CombatBalance.CODEC, CombatBalance.DEFAULT);
    public static final BalanceKey<SkillsBalance> SKILLS = register("skills", SkillsBalance.CODEC, SkillsBalance.defaults());
    public static final BalanceKey<BossesBalance> BOSSES = register("bosses", BossesBalance.CODEC, BossesBalance.defaults());

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

    /** Растёт при каждой загрузке баланса — по нему кэши понимают, что цифры поменялись. */
    public static int generation() {
        return generation;
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
        generation++;
        for (String p : problems) LitRPG.LOG.error("[LitRPG] Баланс: {}", p);
        return errors;
    }

    private static <T> void applyOne(BalanceKey<T> key, JsonElement json, List<String> problems) {
        var result = key.codec().codec().parse(JsonOps.INSTANCE, json);
        result.resultOrPartial(msg -> problems.add(key.name() + ": " + msg)).ifPresentOrElse(value -> {
            VALUES.put(key, value);
            if (result.error().isEmpty()) {
                // Поле, которого нет после обратного кодирования, кодек не знает — скорее всего опечатка.
                key.codec().codec().encodeStart(JsonOps.INSTANCE, value).result()
                        .ifPresent(known -> unknownFields(key.name(), json, known, problems));
                FROM_DATA.add(key.name());
            }
        }, () -> VALUES.remove(key));
    }

    private static void unknownFields(String path, JsonElement given, JsonElement known, List<String> problems) {
        if (!given.isJsonObject() || !known.isJsonObject()) return;
        JsonObject k = known.getAsJsonObject();
        for (var e : given.getAsJsonObject().entrySet()) {
            if (!k.has(e.getKey())) problems.add(path + ": лишнее поле «" + e.getKey() + "» (опечатка?)");
            else unknownFields(path + "." + e.getKey(), e.getValue(), k.get(e.getKey()), problems);
        }
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
