package com.andrey.litrpg.core.balance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Прохождение: отряд, опыт этапа, мир (появление, разлом, спасение), духи и воскрешение. Файл {@code balance/run.json}. */
public record RunBalance(Party party, Xp xp, World world, Spirits spirits, Difficulty difficulty) {

    /**
     * Сложность забега (решение владельца 13.11.2026): Обычная и Героическая. Множители здоровья и урона хранителей и
     * монстров; {@code lootFind} прибавляется к «находке» каждого (выше редкость, больше монет и легендарных трофеев).
     */
    public record Mode(double hp, double damage, double lootFind) {
        static final Codec<Mode> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("hp").forGetter(Mode::hp),
                Codec.DOUBLE.fieldOf("damage").forGetter(Mode::damage),
                Codec.DOUBLE.fieldOf("loot_find").forGetter(Mode::lootFind)
        ).apply(i, Mode::new));
    }

    public record Difficulty(Mode normal, Mode heroic) {
        static final Codec<Difficulty> CODEC = RecordCodecBuilder.create(i -> i.group(
                Mode.CODEC.fieldOf("normal").forGetter(Difficulty::normal),
                Mode.CODEC.fieldOf("heroic").forGetter(Difficulty::heroic)
        ).apply(i, Difficulty::new));
    }

    /** Сколько игроков нужно у Печати; повторное нажатие — подтверждение старта без какой-то роли. */
    public record Party(int minPlayers, int maxPlayers, double sealConfirmSeconds) {
        static final Codec<Party> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("min_players").forGetter(Party::minPlayers),
                Codec.INT.fieldOf("max_players").forGetter(Party::maxPlayers),
                Codec.DOUBLE.fieldOf("seal_confirm_seconds").forGetter(Party::sealConfirmSeconds)
        ).apply(i, Party::new));
    }

    /**
     * Опыт этапа делится: отряды (по весам), хранители (поровну), загадки и тайники. Если загадок на этапе нет —
     * их доля уходит хранителям. Сумма — ровно столько, сколько нужно на 10 уровней этапа.
     */
    public record Xp(double packs, double bosses, double extra) {
        static final Codec<Xp> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("packs").forGetter(Xp::packs),
                Codec.DOUBLE.fieldOf("bosses").forGetter(Xp::bosses),
                Codec.DOUBLE.fieldOf("extra").forGetter(Xp::extra)
        ).apply(i, Xp::new));
    }

    /**
     * @param spawnRadius  отряд и хранитель появляются, когда кто-то из отряда ближе этого
     * @param mobLeash     монстр, утащенный дальше от своего места, возвращается и лечится
     * @param rescueDamage упавший в пропасть или лаву теряет эту долю здоровья
     */
    public record World(double spawnRadius, double mobLeash, double riftRadius, double riftSeconds, double rescueDamage) {
        static final Codec<World> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("spawn_radius").forGetter(World::spawnRadius),
                Codec.DOUBLE.fieldOf("mob_leash").forGetter(World::mobLeash),
                Codec.DOUBLE.fieldOf("rift_radius").forGetter(World::riftRadius),
                Codec.DOUBLE.fieldOf("rift_seconds").forGetter(World::riftSeconds),
                Codec.DOUBLE.fieldOf("rescue_damage").forGetter(World::rescueDamage)
        ).apply(i, World::new));
    }

    /**
     * @param reviveSeconds       воскрешение союзником вне боя (Shift рядом с духом)
     * @param reviveCombatSeconds то же во время боя
     * @param bossReviveHealth    с каким здоровьем возвращаются павшие, когда хранитель повержен
     * @param leash               дух не отходит от места гибели дальше этого
     */
    /**
     * Подъём павшего: встать над телом и держать Shift. Поддержка поднимает вдвое быстрее и может повторять
     * это часто, остальные — долго и редко, поэтому за лекаря в бою держатся.
     */
    public record Spirits(double wipeSeconds, double reviveSupportSeconds, double reviveOtherSeconds,
                          double reviveCooldownSupport, double reviveCooldownOther, double reviveRadius,
                          double reviveHealth, double bossReviveHealth, double leash) {
        static final Codec<Spirits> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("wipe_seconds").forGetter(Spirits::wipeSeconds),
                Codec.DOUBLE.fieldOf("revive_support_seconds").forGetter(Spirits::reviveSupportSeconds),
                Codec.DOUBLE.fieldOf("revive_other_seconds").forGetter(Spirits::reviveOtherSeconds),
                Codec.DOUBLE.fieldOf("revive_cooldown_support").forGetter(Spirits::reviveCooldownSupport),
                Codec.DOUBLE.fieldOf("revive_cooldown_other").forGetter(Spirits::reviveCooldownOther),
                Codec.DOUBLE.fieldOf("revive_radius").forGetter(Spirits::reviveRadius),
                Codec.DOUBLE.fieldOf("revive_health").forGetter(Spirits::reviveHealth),
                Codec.DOUBLE.fieldOf("boss_revive_health").forGetter(Spirits::bossReviveHealth),
                Codec.DOUBLE.fieldOf("leash").forGetter(Spirits::leash)
        ).apply(i, Spirits::new));
    }

    public static final MapCodec<RunBalance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Party.CODEC.fieldOf("party").forGetter(RunBalance::party),
            Xp.CODEC.fieldOf("xp").forGetter(RunBalance::xp),
            World.CODEC.fieldOf("world").forGetter(RunBalance::world),
            Spirits.CODEC.fieldOf("spirits").forGetter(RunBalance::spirits),
            Difficulty.CODEC.fieldOf("difficulty").forGetter(RunBalance::difficulty)
    ).apply(i, RunBalance::new));

    public static final RunBalance DEFAULT = new RunBalance(new Party(3, 6, 10), new Xp(0.5, 0.45, 0.05),
            new World(40, 34, 5, 5, 0.2), new Spirits(3, 5, 10, 15, 45, 2.5, 0.3, 0.5, 7),
            new Difficulty(new Mode(0.65, 0.7, 0), new Mode(1, 1, 1.0)));
}
