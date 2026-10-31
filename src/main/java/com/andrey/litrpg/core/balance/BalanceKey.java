package com.andrey.litrpg.core.balance;

import com.mojang.serialization.MapCodec;

/**
 * Раздел баланса: файл {@code data/litrpg/balance/<name>.json}.
 * Все поля в кодеке обязательные — пропущенное поле считается ошибкой, а не тихим значением по умолчанию.
 * {@code defaults} используются только если файл сломан (и об этом пишется в лог и операторам).
 */
public record BalanceKey<T>(String name, MapCodec<T> codec, T defaults) {
}
