package com.andrey.litrpg.core;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.combat.threat.ThreatTable;
import com.andrey.litrpg.player.PlayerData;
import com.andrey.litrpg.skill.SkillState;
import com.andrey.litrpg.status.StatusList;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Данные, которые мод хранит на сущностях и в мире. */
public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> REG =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LitRPG.MOD_ID);

    public static final Supplier<AttachmentType<PlayerData>> PLAYER = REG.register("player",
            () -> AttachmentType.serializable(PlayerData::new).copyOnDeath().build());

    /** Таблица угрозы врага — не сохраняется: после перезапуска бой начинается заново. */
    public static final Supplier<AttachmentType<ThreatTable>> THREAT = REG.register("threat",
            () -> AttachmentType.builder(ThreatTable::new).build());

    /** Состояние навыков: перезарядки, чтение, ресурс — не сохраняются. */
    public static final Supplier<AttachmentType<SkillState>> SKILL_STATE = REG.register("skill_state",
            () -> AttachmentType.builder(SkillState::new).build());

    /** Метки со сроком (оглушения, щиты, уязвимости…) — живут секунды, не сохраняются. */
    public static final Supplier<AttachmentType<StatusList>> STATUSES = REG.register("statuses",
            () -> AttachmentType.builder(StatusList::new).build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        REG.register(modBus);
    }
}
