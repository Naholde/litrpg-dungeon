package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.BalanceKey;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Автотесты в настоящем игровом сервере ({@code gradlew runGameTestServer}, запускаются и на GitHub).
 * Шаблон {@code litrpg:empty} — пустая площадка 1×1×1.
 */
@GameTestHolder(LitRPG.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoreTests {
    private CoreTests() {
    }

    /** Все файлы баланса из мода прочитаны полностью, без ошибок и без подмены значениями по умолчанию. */
    @GameTest(template = "empty")
    public static void balanceLoadsFromData(GameTestHelper helper) {
        if (!Balance.errors().isEmpty()) {
            helper.fail("Ошибки в балансе: " + String.join("; ", Balance.errors()));
            return;
        }
        for (BalanceKey<?> key : Balance.keys()) {
            if (!Balance.loadedFromData(key)) {
                helper.fail("Раздел баланса не прочитан из файла: " + key.name());
                return;
            }
        }
        helper.succeed();
    }
}
