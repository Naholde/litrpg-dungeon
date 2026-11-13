package com.andrey.litrpg.boss;

import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.core.ModAttachments;
import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.BossesBalance;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.net.BossSyncPayload;
import com.andrey.litrpg.net.Net;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Основа хранителя. Каждый хранитель — свой класс со своими навыками ({@link #abilities()}) и фазами;
 * общее здесь: подготовка навыка (хранитель замирает, светится и выкрикивает название, на полу — разметка),
 * урон долями здоровья бойца, неистовство со временем, щит до разрушения узлов силы, сброс боя,
 * масштаб здоровья под размер отряда, рамка хранителя у игроков.
 */
public abstract class BossEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> CAST_NAME = SynchedEntityData.defineId(BossEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SHIELDED = SynchedEntityData.defineId(BossEntity.class, EntityDataSerializers.BOOLEAN);

    /** Навык хранителя. Цифры — из bosses.json по ключам {@code <key>_cd} и {@code <key>_cast}. */
    public record Ability(String name, String key, int priority, boolean interruptible, Predicate<BossEntity> usable,
                          Consumer<Cast> start) {
    }

    /** Идущая подготовка навыка. Задачи с этим номером отменяются, если подготовку сбили. */
    public final class Cast {
        public final Ability ability;
        public final LivingEntity target;
        public final int ticks;
        final int id;
        final long end;

        Cast(Ability ability, LivingEntity target, int ticks, int id, long end) {
            this.ability = ability;
            this.target = target;
            this.ticks = ticks;
            this.id = id;
            this.end = end;
        }

        public BossEntity boss() {
            return BossEntity.this;
        }

        /** Сделать что-то через {@code delay} тиков (удар по разметке). */
        public void at(int delay, Runnable r) {
            schedule(delay, id, r);
        }
    }

    private record Task(long at, int castId, Runnable run, String label) {
    }

    /** Каким приёмом сейчас бьёт хранитель — для журнала боя («Удар цепью», «Гул врат», «Давка»…). */
    private String hitLabel = "Удар";

    public String hitLabel() {
        return hitLabel;
    }

    /** Боец, в чей журнал боя пишется этот хранитель (у Вессы — бой Морры). */
    public BossEntity logOwner() {
        return this;
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<Task> tasks = new ArrayList<>();
    private final Map<String, Long> readyAt = new HashMap<>();
    private final List<UUID> spawned = new ArrayList<>();
    /** Узлы, которые держат щит (ледяные гробницы и прочие объекты боя щит не держат). */
    private final List<UUID> shieldNodes = new ArrayList<>();
    private final List<Pool> pools = new ArrayList<>();

    /** Лужа на полу (яд, споры, огонь): каждую секунду бьёт стоящих внутри, может расти. */
    private static final class Pool {
        final Vec3 at;
        double radius;
        final double grow, maxRadius, units;
        final long until;
        final int color;
        final java.util.function.Consumer<ServerPlayer> extra;
        String label = "Лужа";

        Pool(Vec3 at, double radius, double grow, double maxRadius, long until, double units, int color,
             java.util.function.Consumer<ServerPlayer> extra) {
            this.at = at;
            this.radius = radius;
            this.grow = grow;
            this.maxRadius = maxRadius;
            this.until = until;
            this.units = units;
            this.color = color;
            this.extra = extra;
        }
    }
    private Cast casting;
    private int castCounter;
    private Vec3 home;
    private double arenaRadius = 18;
    private boolean engaged;
    private long combatStart;

    /** Тик вступления в бой (для времени боя — рекорды хранителей). */
    public long combatStart() {
        return combatStart;
    }
    private long lastPlayerSeen;
    private long nextAuto;
    /** Давление (фаза 12): когда следующая волна по отряду и следующая метка. */
    private long nextPulse, nextMark;
    private boolean pressureTold;
    private long nextRush;
    private boolean rushMelee;
    /**
     * Ритм: до {@code busyUntil} новый приём не начнётся (3–5 с от начала прошлого и не раньше конца его замаха).
     * Ярость/передышка — только у хранителей с {@link #frenzy()}: {@code surgeUntil} — конец ярости (приёмы
     * чаще), {@code tiredUntil} — конец передышки «Изнурён» (не бьёт, урон по нему выше).
     */
    private long busyUntil, tiredUntil, surgeUntil;
    /** Когда начат последний приём (страховка темпа: не реже раза в max_seconds). */
    private long lastStart;
    /**
     * Чей приём сейчас идёт: номер подготовки последнего навыка и связанных с ним «проверочных» разметок
     * (залп, цепи из стен). Задача, поставленная изнутри такого приёма (даже вложенная — волна обвала через
     * 1,7 с после своей разметки), продлевает занятость хранителя — новый навык не ляжет поверх.
     */
    private int activeAbility = -1, runningCast = -1;
    private Ability activeRef;
    private final java.util.Set<Integer> linked = new java.util.HashSet<>();
    private int partySize = 3;
    private int phase;
    private int enrageStage;
    private int syncTimer;
    /** Хранитель прохождения: id из схемы и размер отряда на старте (0 — считать по игрокам на арене). */
    private String runId;
    private int fixedParty;
    /** Второй зал: на половине здоровья бой переносится туда (вместе с отрядом). */
    private Vec3 arena2;
    private double arena2Radius;
    private Vec3 homeOrig;
    private double arenaOrig;
    private boolean relocated;
    /** Прохождение узнаёт о переносе боя (духов павших — следом). */
    public static java.util.function.BiConsumer<BossEntity, Vec3> onRelocate = (b, from) -> {
    };

    protected BossEntity(EntityType<? extends BossEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder bossAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    // ------------------------------------------------------------------ что задаёт конкретный хранитель

    public abstract BossRegistry.Type type();

    /** Навыки по убыванию важности. */
    protected abstract List<Ability> abilities();

    /** Пороги фаз по доле здоровья, по убыванию (0,66 → фаза 1, 0,33 → фаза 2). */
    protected abstract double[] phaseThresholds();

    /** Бой начался (хранитель может позвать помощников, расставить объекты). */
    protected void onEngage(List<ServerPlayer> players) {
    }

    /** Хранитель «выключен» (лежит, ждёт): не ходит, не бьёт, не готовит навыки. */
    protected boolean dormant() {
        return false;
    }

    /** Хранитель перешёл в фазу {@code phase} (1, 2…). */
    protected abstract void onPhase(int phase);

    /** Id модели GeckoLib (файлы geo/animations/textures). */
    public abstract String modelId();

    // ------------------------------------------------------------------ цифры

    public double p(String key) {
        BossesBalance b = Balance.get(Balance.BOSSES);
        Double def = type().params().get(key);
        if (def == null) throw new IllegalArgumentException(type().id() + ": нет параметра «" + key + "»");
        return b.value(type().id(), key, def);
    }

    public int bossLevel() {
        return (int) Math.round(p("level"));
    }

    public double enrageMultiplier() {
        return 1 + (Balance.get(Balance.BOSSES).common().enrageMult() - 1) * enrageStage;
    }

    public int phase() {
        return phase;
    }

    public int partySize() {
        return partySize;
    }

    public boolean shielded() {
        return entityData.get(SHIELDED);
    }

    public String castName() {
        return entityData.get(CAST_NAME);
    }

    public boolean engaged() {
        return engaged;
    }

    public Vec3 home() {
        return home != null ? home : position();
    }

    public double arenaRadius() {
        return arenaRadius;
    }

    public void setArena(Vec3 center, double radius) {
        home = center;
        arenaRadius = radius;
    }

    /** Поставлен прохождением: id из схемы и размер отряда, под который считаются здоровье и урон. */
    public void setRun(String id, int partySize) {
        runId = id;
        fixedParty = partySize;
    }

    public String runId() {
        return runId;
    }

    /** Центр второго зала (или этой арены, если второго нет): куда ставить объекты второй половины боя. */
    public Vec3 secondArenaCenter() {
        return relocated || arena2 == null ? home() : arena2;
    }

    public double secondArenaRadius() {
        return relocated || arena2 == null ? arenaRadius() : arena2Radius;
    }

    /** Сорвать подготовку без перезарядки (хранитель упал, сменил фазу). */
    protected void cancelCast() {
        if (casting == null) return;
        int id = casting.id;
        tasks.removeIf(t -> t.castId == id);
        busyUntil = level().getGameTime() + gapTicks();
        endCast();
    }

    public void setSecondArena(Vec3 center, double radius) {
        arena2 = center;
        arena2Radius = radius;
    }

    public boolean relocated() {
        return relocated;
    }

    /** Название второго зала для фразы хранителя (переопределяют хранители). */
    protected String secondArenaName() {
        return "другой зал";
    }

    /**
     * Перенос боя: хранитель кричит, через 3 с он и все живые на арене — во втором зале (по кругу у края),
     * арена теперь там. Неистовство, фазы и угроза сохраняются.
     */
    private void relocate() {
        relocated = true;
        FightLog.event(this, "бой перенесён: " + secondArenaName());
        triggerAnim("main", "relocate");
        tell(Component.literal(type().name() + " уходит в " + secondArenaName() + " — Система переносит отряд следом!")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        Fx.pillar((ServerLevel) level(), position(), 2, 8, 60, Fx.SHADOW);
        level().playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.5f);
        schedule(60, () -> {
            Vec3 from = home();
            List<ServerPlayer> party = playersInArena();
            homeOrig = home;
            arenaOrig = arenaRadius;
            home = arena2;
            arenaRadius = arena2Radius;
            teleportTo(arena2.x, arena2.y, arena2.z);
            for (int i = 0; i < party.size(); i++) {
                double a = Math.PI * 2 * i / Math.max(1, party.size());
                double r = arena2Radius * 0.6;
                ServerPlayer p = party.get(i);
                p.teleportTo((ServerLevel) level(), arena2.x + Math.cos(a) * r, arena2.y, arena2.z + Math.sin(a) * r, p.getYRot(), 0);
                p.fallDistance = 0;
            }
            Fx.ring((ServerLevel) level(), arena2, (float) arena2Radius, 30, Fx.SHADOW);
            onRelocate.accept(this, from);
            onRelocated();
        });
    }

    /** Бой перенесён (хранитель может сменить набор навыков, поставить узлы…). */
    protected void onRelocated() {
    }

    // ------------------------------------------------------------------ игроки на арене

    public List<ServerPlayer> playersInArena() {
        if (!(level() instanceof ServerLevel sl)) return List.of();
        Vec3 c = home();
        double r = arenaRadius + 2;
        return sl.players().stream().filter(p -> p.isAlive() && !p.isSpectator() && !p.gameMode.isCreative()
                && p.position().distanceToSqr(c) <= r * r && Math.abs(p.getY() - c.y) < 16).toList();
    }

    /** Кто видит рамку хранителя: все в 10 блоках от арены (даже если их оттеснили за край). */
    private List<ServerPlayer> watchers() {
        if (!(level() instanceof ServerLevel sl)) return List.of();
        double r = arenaRadius + 10;
        return sl.players().stream().filter(p -> p.position().distanceToSqr(home()) <= r * r).toList();
    }

    public void tell(Component text) {
        for (ServerPlayer p : playersInArena()) SystemChat.send(p, text);
    }

    // ------------------------------------------------------------------ тик

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(CAST_NAME, "");
        b.define(SHIELDED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) return;
        if (com.andrey.litrpg.dev.Showcase.is(this)) return;   // витрина модели: без боя
        if (home == null) home = position();
        long now = level().getGameTime();
        List<ServerPlayer> players = playersInArena();
        if (!engaged) {
            getNavigation().stop();
            if (players.stream().anyMatch(p -> p.distanceTo(this) < arenaRadius * 0.75)) engage(players);
            return;
        }
        if (players.isEmpty()) {
            if (now - lastPlayerSeen > Balance.get(Balance.BOSSES).common().resetSeconds() * 20) {
                resetFight();
                return;
            }
        } else {
            lastPlayerSeen = now;
        }
        if (position().distanceToSqr(home) > (arenaRadius + 6) * (arenaRadius + 6)) teleportTo(home.x, home.y, home.z);
        // Цель потеряна (исчезновение, невидимость, приманка разбита), а на арене кто-то есть — хранитель
        // не стоит столбом: снова «видит» всех видимых на арене, и таблица угрозы выбирает, кого бить.
        if (getTarget() == null && now % 10 == 0 && !players.isEmpty()) {
            for (ServerPlayer p : players) ThreatService.add(this, p, 1);
            ThreatService.tick(this);
        }
        updateEnrage(now);
        if (frenzy()) updateFrenzy(now);
        boolean tired = now < tiredUntil;
        if (!dormant() && !tired) pressure(now, players);
        // Давка (заполняла простои между навыками) снята: при темпе 3–5 с простоев нет, а давка отодвигала приёмы.
        tickPools(now);
        updatePhase();
        if (arena2 != null && !relocated && getHealth() <= getMaxHealth() * 0.5f) relocate();
        runTasks(now);
        if (dormant()) {
            getNavigation().stop();
        } else if (tired) {
            getNavigation().stop();
        } else if (casting != null) {
            getNavigation().stop();
            if (casting.target != null) getLookControl().setLookAt(casting.target);
            if (now >= casting.end) endCast();
        } else if (now < busyUntil || !tryStartAbility(now)) {
            chaseAndHit(now);
        }
        updateShield();
        if (++syncTimer >= 5) {
            syncTimer = 0;
            sync(watchers());
        }
    }

    private void engage(List<ServerPlayer> players) {
        engaged = true;
        combatStart = level().getGameTime();
        lastPlayerSeen = combatStart;
        nextAuto = combatStart + 20;
        nextPulse = combatStart + 8 * 20;
        nextMark = combatStart + 12 * 20;
        busyUntil = 0;
        tiredUntil = 0;
        surgeUntil = frenzy() ? combatStart + Math.round(p("frenzy_surge") * 20) : 0;
        pressureTold = false;
        partySize = Math.max(1, Math.min(com.andrey.litrpg.party.Party.MAX, fixedParty > 0 ? fixedParty : players.size()));
        double max = p("hp_units") * BossDamage.unit(bossLevel()) * Balance.get(Balance.BOSSES).common().hpScale(partySize)
                * BossDamage.earlyHp(bossLevel())
                * com.andrey.litrpg.run.Difficulty.active(getServer()).hp();   // обычная/героическая
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(max);
        setHealth((float) max);
        // Хранитель знает всех на арене сразу: цель ведёт таблица угрозы (у танка множитель ×5).
        for (ServerPlayer p : players) ThreatService.add(this, p, 1);
        ThreatService.tick(this);
        // Первые навыки — не сразу: группа успевает встать.
        for (Ability a : abilities()) readyAt.put(a.key(), combatStart + Math.round(p(a.key() + "_cd") * 10));
        tell(Component.literal(type().name() + " вступает в бой. Отряд: " + partySize + ", ур. " + bossLevel() + ".")
                .withStyle(ChatFormatting.RED));
        String quote = BossGuide.of(type().id()).quote();
        if (!quote.isEmpty()) {
            tell(Component.literal(type().name() + ": «" + quote + "»").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
        FightLog.start(this, players);
        onEngage(players);
        level().playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6f, 1.4f);
    }

    /** Сброс боя: всё как было — здоровье, фазы, щит, подручные, угроза. */
    public void resetFight() {
        if (engaged && logOwner() == this) FightLog.end(this, "сброс (отряд пал или ушёл)", watchers());
        engaged = false;
        casting = null;
        tasks.clear();
        readyAt.clear();
        phase = 0;
        enrageStage = 0;
        busyUntil = 0;
        tiredUntil = 0;
        surgeUntil = 0;
        activeAbility = -1;
        linked.clear();
        entityData.set(CAST_NAME, "");
        entityData.set(SHIELDED, false);
        removeSpawned();
        if (relocated && homeOrig != null) {
            home = homeOrig;
            arenaRadius = arenaOrig;
        }
        relocated = false;
        if (hasData(ModAttachments.THREAT)) ThreatService.table(this).clear();
        StatusService.clear(this);
        setTarget(null);
        setHealth(getMaxHealth());
        if (home != null) teleportTo(home.x, home.y, home.z);
        for (ServerPlayer p : ((ServerLevel) level()).players()) {
            if (p.position().distanceToSqr(home()) < (arenaRadius + 30) * (arenaRadius + 30)) {
                Net.send(p, BossSyncPayload.gone(getId()));
                SystemChat.send(p, Component.literal("Бой с хранителем сброшен.").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /**
     * Давление — у каждого хранителя под своим именем ({@link BossGuide#pressure}). Волна: кольцо расходится
     * от хранителя 1,5 с, потом бьёт всех на арене — уйти нельзя, лечить нужно. Метка: боец не-цель горит
     * {@code mark_duration} секунд, вокруг него на полу заполняется круг; в конец — удар всем, кто внутри.
     * Отойди от союзников — и метка заденет только тебя.
     */
    private void pressure(long now, List<ServerPlayer> players) {
        BossGuide.Pressure names = BossGuide.pressure(type().id());
        if (names == null || players.isEmpty()) return;
        BossesBalance.Pressure pr = Balance.get(Balance.BOSSES).common().pressure();
        double grow = pressureGrow(bossLevel(), partySize);
        ServerLevel sl = (ServerLevel) level();
        if (!pressureTold) {
            pressureTold = true;
            tell(Component.literal("«" + names.pulse() + "» бьёт весь отряд — лекарь, держите всех. «" + names.mark()
                    + "» — меченый, отойди от своих, пока круг не заполнился.").withStyle(ChatFormatting.GRAY));
        }
        if (now >= nextPulse && (casting != null || now < busyUntil)) {
            // Ритм: волна ждёт, пока хранитель доиграет приём, и бьёт в паузе — не вместе с навыком.
            nextPulse = Math.max(nextPulse, busyUntil + Math.round(rhythm().pulseGapSeconds() * 20));
        }
        if (now >= nextPulse) {
            nextPulse = now + Math.round(pr.pulseSeconds() * 20 * (0.85 + random.nextDouble() * 0.3));
            hitLabel = names.pulse();
            int wind = 30;
            Fx.wave(sl, position(), (float) arenaRadius, 2.5f, wind, names.color());
            Fx.pillar(sl, position(), 1.2f, 5, wind, names.color());
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 0.8f, 1.4f);
            schedule(wind, () -> {
                for (ServerPlayer p : playersInArena()) {
                    BossDamage.hit(this, p, pr.pulseUnits() * grow);
                    Fx.burst(sl, p.position().add(0, 1, 0), 0.6f, 6, names.color());
                }
                sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.6f, 1.2f);
            });
            hitLabel = "Удар";
        }
        if (now >= nextMark) {
            nextMark = now + Math.round(pr.markSeconds() * 20 * (0.85 + random.nextDouble() * 0.3));
            List<ServerPlayer> pool = new ArrayList<>(players);
            pool.remove(getTarget());
            java.util.Collections.shuffle(pool, new java.util.Random(random.nextLong()));
            int count = Math.min(pool.size(), partySize >= 5 ? 2 : 1);
            hitLabel = names.mark();
            int dur = (int) Math.round(pr.markDuration() * 20);
            float r = (float) pr.markRadius();
            for (ServerPlayer p : pool.subList(0, count)) {
                Fx.discOn(sl, p, r, dur, Fx.DANGER, true);
                Fx.mark(sl, p, "☠", dur, names.color());
                SystemChat.send(p, Component.literal("На вас «" + names.mark() + "»: отойдите от своих — через "
                        + Math.round(pr.markDuration()) + " с рванёт по всем рядом!").withStyle(ChatFormatting.RED));
                for (int t = 20; t <= dur; t += 20) {
                    schedule(t, () -> {
                        if (p.isAlive() && !p.isSpectator()) BossDamage.hit(this, p, pr.markUnits() * grow);
                    });
                }
                schedule(dur, () -> {
                    if (!p.isAlive() || p.isSpectator()) return;
                    for (ServerPlayer o : playersInArena()) {
                        if (o != p && o.distanceTo(p) <= r) BossDamage.hit(this, o, pr.markBurstUnits());
                    }
                    Fx.ring(sl, p.position(), r, 10, names.color());
                    sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.5f, 1.5f);
                });
            }
            hitLabel = "Удар";
        }
    }

    /**
     * Сила давления: растёт с уровнем хранителя (вещи лекаря к концу подземелья лечат втрое сильнее, чем в начале),
     * а для отряда больше трёх — тише на каждого (волна бьёт всех, а лекарь, скорее всего, один).
     */
    public static double pressureGrow(int level, int party) {
        return (1 + level / 25.0) * (1 + Math.max(0, level - 25) / 30.0) * Math.pow(3.0 / Math.max(3, party), 0.8);
    }

    private void updateEnrage(long now) {
        int stage = (int) ((now - combatStart) / Math.max(20, Math.round(Balance.get(Balance.BOSSES).common().enrageSeconds() * 20)));
        if (stage > enrageStage) {
            enrageStage = stage;
            FightLog.event(this, "неистовство ×" + Derived.trim2(enrageMultiplier()));
            triggerAnim("main", "enrage");
            tell(Component.literal(type().name() + " впадает в неистовство: удары сильнее (×"
                    + Derived.trim2(enrageMultiplier()) + ").").withStyle(ChatFormatting.DARK_RED));
            Fx.ring((ServerLevel) level(), position(), 8, 16, Fx.DANGER);
            level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1f, 0.6f);
        }
    }

    /**
     * Давка между навыками: хранитель всё время заставляет двигаться. Разметка горит меньше секунды —
     * шагом из неё не выйти, только рывком или заранее встав правильно. Чередуются выпады по бойцам
     * и круг у ног хранителя, чтобы ближний бой не стоял в куче.
     */
    private void rush(long now, List<ServerPlayer> players) {
        if (players.isEmpty() || casting != null || now < nextRush || now < busyUntil) return;
        BossesBalance.Rush r = Balance.get(Balance.BOSSES).common().rush();
        nextRush = now + Math.round(r.seconds() * 20 * (0.8 + random.nextDouble() * 0.4) * BossDamage.earlyCooldown(bossLevel()));
        int warn = Math.max(6, (int) Math.round(r.warn() * 20 * BossDamage.earlyCast(bossLevel())));
        busyUntil = now + warn + gapTicks();
        rushMelee = !rushMelee;
        hitLabel = "Давка";
        if (rushMelee) {
            Telegraph.circle(testCast(warn), position(), (float) r.meleeRadius(), r.meleeUnits() * enrageMultiplier());
            level().playSound(null, blockPosition(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.2f, 0.6f);
            hitLabel = "Удар";
            return;
        }
        List<ServerPlayer> aim = new ArrayList<>(players);
        aim.remove(getTarget());
        if (aim.isEmpty()) aim = players;
        java.util.Collections.shuffle(aim, new java.util.Random(now));
        int n = Math.max(1, Math.min(aim.size(), (players.size() + 1) / 2));
        for (int i = 0; i < n; i++) {
            Telegraph.circle(testCast(warn), aim.get(i).position(), (float) r.radius(), r.units() * enrageMultiplier());
        }
        level().playSound(null, blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.HOSTILE, 1.1f, 0.8f);
        hitLabel = "Удар";
    }

    private void updatePhase() {
        double[] th = phaseThresholds();
        double ratio = getHealth() / getMaxHealth();
        while (phase < th.length && ratio <= th[phase]) {
            phase++;
            FightLog.event(this, type().name() + ": фаза " + (phase + 1));
            triggerAnim("main", "phase");
            onPhase(phase);
        }
    }

    // ------------------------------------------------------------------ навыки

    private BossesBalance.Rhythm rhythm() {
        return Balance.get(Balance.BOSSES).common().rhythm();
    }

    /** Пауза после замаха, пока следующий приём не может начаться. */
    private long gapTicks() {
        return Math.round(rhythm().afterCast() * 20);
    }

    /** Через сколько тиков от начала приёма можно начать следующий: 3–5 с, в ярости — {@code frenzy_pace}. */
    private long paceTicks() {
        if (frenzy() && level().getGameTime() < surgeUntil) {
            double f = p("frenzy_pace");
            return Math.round(f * 20 * (0.85 + random.nextDouble() * 0.3));
        }
        var r = rhythm();
        return Math.round((r.minSeconds() + random.nextDouble() * (r.maxSeconds() - r.minSeconds())) * 20);
    }

    /**
     * Следующий навык. Из готовых берётся тот, кто ждёт дольше всех (а не первый в списке) — у хранителя
     * читаемая очередь приёмов, и ни один не повторяется подряд, пока ждут другие. Следующий — через 3–5 с
     * от начала этого и не раньше конца его замаха.
     */
    private boolean tryStartAbility(long now) {
        if (getTarget() == null) return false;
        Ability pick = null;
        long oldest = Long.MAX_VALUE;
        for (Ability a : abilities()) {
            long ready = readyAt.getOrDefault(a.key(), 0L);
            if (now < ready || !a.usable().test(this)) continue;
            if (ready < oldest) {
                oldest = ready;
                pick = a;
            }
        }
        if (pick == null && now - lastStart >= Math.round(rhythm().maxSeconds() * 20)) {
            // Страховка темпа: прошло больше max_seconds, а готовых нет — берём тот, что откатится раньше всех.
            long soonest = Long.MAX_VALUE;
            for (Ability a : abilities()) {
                long ready = readyAt.getOrDefault(a.key(), 0L);
                if (a.usable().test(this) && ready < soonest) {
                    soonest = ready;
                    pick = a;
                }
            }
        }
        if (pick == null) return false;
        Ability a = pick;
        lastStart = now;
        double cdMult = BossDamage.earlyCooldown(bossLevel()) * (frenzy() && now < surgeUntil ? 0.5 : 1);
        readyAt.put(a.key(), now + Math.round(p(a.key() + "_cd") * 20 * cdMult));
        int ticks = (int) Math.round(p(a.key() + "_cast") * 20 * BossDamage.earlyCast(bossLevel()));
        casting = new Cast(a, getTarget(), ticks, ++castCounter, now + ticks);
        entityData.set(CAST_NAME, a.name() + (a.interruptible() ? "|1" : "|0") + "|" + ticks + "|" + (now + ticks));
        getNavigation().stop();
        tell(Component.literal(type().name() + " готовит: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(a.name()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(a.interruptible() ? " — можно сбить оглушением!" : "").withStyle(ChatFormatting.AQUA)));
        triggerAnim("main", "a_" + a.key());
        activeAbility = casting.id;
        activeRef = a;
        linked.clear();
        busyUntil = Math.max(now + ticks + gapTicks(), now + paceTicks());
        runningCast = casting.id;
        hitLabel = a.name();
        FightLog.cast(this, a.name());
        try {
            a.start().accept(casting);
        } finally {
            runningCast = -1;
            hitLabel = "Удар";
        }
        return true;
    }

    /**
     * Хранитель с идеей «ярость и передышка» (сейчас — Страж Прилива): {@code frenzy_surge} секунд бьёт без остановки
     * (приём раз в {@code frenzy_pace} с, откаты вдвое короче), урона по нему проходит лишь {@code frenzy_armor};
     * потом {@code frenzy_window} секунд «Изнурён» — стоит, не бьёт, урон по нему ×{@code frenzy_bonus}.
     * Убивают его в окнах — здоровье считается под это (BalanceSimTest).
     */
    protected boolean frenzy() {
        return false;
    }

    /** Название ярости и передышки для фраз (переопределяют). */
    protected String surgeName() {
        return "Ярость";
    }

    protected String windowName() {
        return "Изнурён";
    }

    private void updateFrenzy(long now) {
        if (tiredUntil > 0 && now >= tiredUntil) {
            tiredUntil = 0;
            surgeUntil = now + Math.round(p("frenzy_surge") * 20);
            busyUntil = now + 20;
            entityData.set(CAST_NAME, "");
            tell(Component.literal(type().name() + ": " + surgeName() + "! Уходите из-под ударов — урон по нему почти не проходит.")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2f, 0.9f);
            return;
        }
        if (tiredUntil == 0 && surgeUntil > 0 && now >= surgeUntil && casting == null) startWindow(now);
    }

    private void startWindow(long now) {
        int ticks = (int) Math.round(p("frenzy_window") * 20);
        tiredUntil = now + ticks;
        busyUntil = tiredUntil + 20;
        FightLog.event(this, windowName().toLowerCase());
        nextAuto = Math.max(nextAuto, tiredUntil + 10);
        entityData.set(CAST_NAME, windowName() + " — бейте!|0|" + ticks + "|" + tiredUntil);
        getNavigation().stop();
        triggerAnim("main", "tired");
        tell(Component.literal(type().name() + ": " + windowName().toLowerCase() + " — окно! Урон по нему ×"
                + Derived.trim2(p("frenzy_bonus")) + ".").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        ServerLevel sl = (ServerLevel) level();
        Fx.pillar(sl, position(), getBbWidth() * 0.7f, getBbHeight() + 1, ticks, 0x9CFFB0);
        Fx.ring(sl, position(), getBbWidth() + 1.5f, ticks, 0x9CFFB0);
        level().playSound(null, blockPosition(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 1.2f, 0.8f);
    }

    /** Замедлить бойца на долю {@code amount} на {@code ticks} (лужи, сети, течения). */
    public void slow(ServerPlayer p, double amount, int ticks) {
        com.andrey.litrpg.status.StatusService.apply(p, com.andrey.litrpg.status.Status.of(com.andrey.litrpg.status.StatusType.SLOW,
                type().id() + "_slow", getUUID(), amount, level().getGameTime(), ticks));
    }

    /** Толкнуть бойца по направлению {@code dir} (течения, рёв). */
    public static void push(ServerPlayer p, Vec3 dir, double strength) {
        Vec3 d = dir.multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) return;
        d = d.normalize().scale(strength);
        p.push(d.x, 0.05, d.z);
        p.hurtMarked = true;
    }

    /** Направление под углом {@code deg} в плоскости пола. */
    public static Vec3 dir(double deg) {
        double a = Math.toRadians(deg);
        return new Vec3(Math.cos(a), 0, Math.sin(a));
    }

    /** Случайная точка арены на доле радиуса от {@code from} до {@code to}. */
    public Vec3 randomSpot(double from, double to) {
        double a = random.nextDouble() * Math.PI * 2, r = arenaRadius() * (from + random.nextDouble() * (to - from));
        return home().add(Math.cos(a) * r, 0, Math.sin(a) * r);
    }

    /** Цвет стихии хранителя (фаза 13): обводка его разметки и вспышка удара. */
    public int element() {
        BossGuide.Pressure pr = BossGuide.pressure(type().id());
        return pr != null ? pr.color() : Fx.DANGER;
    }

    /**
     * Как выглядит удар хранителя по месту: вспышка, осколки и пыль его стихии. Хранители переопределяют —
     * у Привратника летят кости, у Карна лёд, у Древня споры.
     */
    public void impact(Vec3 at, float radius) {
        ServerLevel sl = (ServerLevel) level();
        int col = element();
        Fx.burst(sl, at.add(0, 0.4, 0), radius * 0.8f, 8, col);
        Fx.shards(sl, at.add(0, 0.3, 0), radius, Math.max(4, Math.round(radius * 3)), 12, col);
        Fx.dust(sl, at.add(0, 0.5, 0), col, Math.round(6 + radius * 4), radius * 0.5, 0.05, 1.2f);
    }

    /** До какого тика хранитель доигрывает приём и стоит паузу (тесты ритма). */
    public long busyUntil() {
        return busyUntil;
    }

    /** Хранитель в передышке (стенд, тесты, рамка). */
    public boolean tired() {
        return level().getGameTime() < tiredUntil;
    }

    /** Навыки хранителя (стенд и проверки). */
    public List<Ability> debugAbilities() {
        return abilities();
    }

    /** Начать подготовку навыка в обход перезарядки и условий (стенд «от всего можно уйти»). */
    public void debugCast(Ability a) {
        long now = level().getGameTime();
        int ticks = (int) Math.round(p(a.key() + "_cast") * 20);
        casting = new Cast(a, getTarget(), ticks, ++castCounter, now + ticks);
        activeAbility = casting.id;
        activeRef = a;
        linked.clear();
        runningCast = casting.id;
        try {
            a.start().accept(casting);
        } finally {
            runningCast = -1;
        }
    }

    private void endCast() {
        casting = null;
        entityData.set(CAST_NAME, "");
    }

    /** Оглушение во время подготовки, которую можно сбить, — подготовка сорвана (сам хранитель не оглушается). */
    public void onStunned() {
        if (casting == null || !casting.ability.interruptible()) return;
        int id = casting.id;
        tasks.removeIf(t -> t.castId == id);
        readyAt.put(casting.ability.key(), level().getGameTime() + Math.round(p(casting.ability.key() + "_cd") * 10));
        tell(Component.literal("Подготовка «" + casting.ability.name() + "» сорвана!").withStyle(ChatFormatting.AQUA));
        busyUntil = level().getGameTime() + gapTicks();
        Fx.burst((ServerLevel) level(), position().add(0, getBbHeight() * 0.7, 0), 2.5f, 10, Fx.ARCANE);
        level().playSound(null, blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1f, 0.8f);
        endCast();
    }

    public void schedule(int delay, int castId, Runnable r) {
        if (castId == -1) castId = runningCast;
        long at = level().getGameTime() + delay;
        tasks.add(new Task(at, castId, r, hitLabel));

    }

    public void schedule(int delay, Runnable r) {
        schedule(delay, -1, r);
    }

    /**
     * Задача, не продлевающая приём: живёт сама (свечи стреляют, пока стоят), хранитель тем временем ведёт
     * бой дальше. Срыв подготовки её не отменяет.
     */
    public void detached(int delay, Runnable r) {
        schedule(delay, -2, r);
    }

    private void runTasks(long now) {
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (now >= t.at) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) {
            if (!isAlive() || !engaged) continue;
            runningCast = t.castId;
            hitLabel = t.label;
            try {
                t.run.run();
            } finally {
                runningCast = -1;
                hitLabel = "Удар";
            }
        }
    }

    // ------------------------------------------------------------------ движение и обычный удар

    private void chaseAndHit(long now) {
        LivingEntity t = getTarget();
        if (t == null) {
            getNavigation().stop();
            return;
        }
        double reach = getBbWidth() * 0.8 + 2.2;
        if (distanceTo(t) > reach) {
            getNavigation().moveTo(t, 1.0);
        } else {
            getNavigation().stop();
            getLookControl().setLookAt(t);
            if (now >= nextAuto) {
                nextAuto = now + Math.round(Balance.get(Balance.BOSSES).common().autoAttackSeconds() * 20);
                triggerAnim("main", "attack");
                schedule(7, () -> {
                    if (getTarget() != null && distanceTo(getTarget()) <= reach + 1) BossDamage.hit(this, getTarget(), p("melee_units"));
                });
                level().playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1f, 0.7f);
            }
        }
    }

    // ------------------------------------------------------------------ щит и подручные

    /** Закрыться щитом: урон не проходит, пока живы узлы силы (их нужно разбить). */
    public void raiseShield(List<? extends Entity> nodes) {
        entityData.set(SHIELDED, true);
        nodes.forEach(n -> {
            if (!spawned.contains(n.getUUID())) track(n);
            shieldNodes.add(n.getUUID());
        });
        tell(Component.literal(type().name() + " закрылся щитом! Разбейте узлы силы.").withStyle(ChatFormatting.AQUA));
    }

    private void updateShield() {
        if (!shielded() || !(level() instanceof ServerLevel sl)) return;
        boolean anyNode = shieldNodes.stream().map(sl::getEntity).anyMatch(e -> e != null && e.isAlive());
        if (!anyNode) {
            shieldNodes.clear();
            entityData.set(SHIELDED, false);
            tell(Component.literal("Щит пал — бейте хранителя!").withStyle(ChatFormatting.GREEN));
            Fx.burst(sl, position().add(0, 2, 0), 4, 14, Fx.ARCANE);
            level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 0.6f);
        }
    }

    /** Запомнить призванное (узлы, подручных) — убрать при сбросе и смерти. */
    public void track(Entity e) {
        spawned.add(e.getUUID());
    }

    private void removeSpawned() {
        if (!(level() instanceof ServerLevel sl)) return;
        for (UUID id : spawned) {
            Entity e = sl.getEntity(id);
            if (e != null) e.discard();
        }
        spawned.clear();
        shieldNodes.clear();
        pools.clear();
        zones.clear();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (!engaged && source.getEntity() instanceof ServerPlayer) engage(playersInArena());
        if (shielded() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            Fx.burst((ServerLevel) level(), position().add(0, getBbHeight() * 0.6, 0), 1.6f, 6, Fx.ARCANE);
            return false;
        }
        if (frenzy()) amount *= (float) (tired() ? p("frenzy_bonus") : p("frenzy_armor"));
        float before = getHealth();
        boolean ok = super.hurt(source, amount);
        if (ok && source.getEntity() instanceof ServerPlayer sp) FightLog.dealt(this, sp, before - getHealth());
        return ok;
    }

    /** Хранителей не оглушить, не замедлить и не обездвижить — только сбить подготовку. */
    public boolean immuneTo(StatusType t) {
        return t == StatusType.STUN || t == StatusType.ROOT || t == StatusType.SLOW || t == StatusType.SILENCE;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ------------------------------------------------------------------ смерть

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) return;
        tasks.clear();
        removeSpawned();
        triggerAnim("main", "death");
        List<ServerPlayer> players = playersInArena();
        if (logOwner() == this) FightLog.end(this, "победа", watchers());
        else FightLog.event(this, type().name() + " пала");
        long secs = (level().getGameTime() - combatStart) / 20;
        for (ServerPlayer p : players) {
            SystemChat.title(p, Component.literal(type().name() + " повержен").withStyle(ChatFormatting.GOLD),
                    Component.literal("Время боя: " + secs / 60 + " мин " + secs % 60 + " с").withStyle(ChatFormatting.GRAY));
            Net.send(p, BossSyncPayload.gone(getId()));
        }
        level().playSound(null, blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 0.7f, 1.3f);
    }

    @Override
    protected void tickDeath() {
        // Анимация гибели длиннее ванильной секунды.
        if (++deathTime >= 40 && !level().isClientSide) remove(RemovalReason.KILLED);
    }

    // ------------------------------------------------------------------ рамка у игроков

    private void sync(List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        List<BossSyncPayload.ThreatRow> rows = new ArrayList<>();
        if (hasData(ModAttachments.THREAT)) {
            var sorted = ThreatService.sorted(this);
            double top = sorted.isEmpty() ? 1 : Math.max(1, sorted.get(0).getValue());
            sorted.stream().limit(5).forEach(e -> {
                ServerPlayer who = getServer().getPlayerList().getPlayer(e.getKey());
                if (who != null) rows.add(new BossSyncPayload.ThreatRow(who.getGameProfile().getName(), (float) (e.getValue() / top)));
            });
        }
        var payload = new BossSyncPayload(getId(), type().name(), type().music(), getHealth(), getMaxHealth(), shielded(), enrageStage,
                castName(), level().getGameTime(), rows);
        for (ServerPlayer p : players) Net.send(p, payload);
    }

    // ------------------------------------------------------------------ анимации GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String m = "animation." + modelId() + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(m + "idle");
        RawAnimation walk = RawAnimation.begin().thenLoop(m + "walk");
        RawAnimation cast = RawAnimation.begin().thenLoop(m + "cast");
        var main = new AnimationController<>(this, "main", 4, state -> {
            if (!castName().isEmpty()) return state.setAndContinue(cast);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        });
        main.triggerableAnim("attack", RawAnimation.begin().thenPlay(m + "attack"))
                .triggerableAnim("slam", RawAnimation.begin().thenPlay(m + "slam"))
                .triggerableAnim("death", RawAnimation.begin().thenPlayAndHold(m + "death"))
                .triggerableAnim("phase", RawAnimation.begin().thenPlay(m + "phase"))
                .triggerableAnim("enrage", RawAnimation.begin().thenPlay(m + "enrage"))
                .triggerableAnim("relocate", RawAnimation.begin().thenPlay(m + "relocate"))
                .triggerableAnim("tired", RawAnimation.begin().thenPlay(m + "tired"));
        // У каждого навыка — своя анимация замаха длиной в его подготовку (tools/gen_models.py).
        for (Ability a : abilities()) main.triggerableAnim("a_" + a.key(), RawAnimation.begin().thenPlay(m + "a_" + a.key()));
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ------------------------------------------------------------------ сохранение

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.putDouble("homeX", home.x);
            tag.putDouble("homeY", home.y);
            tag.putDouble("homeZ", home.z);
        }
        tag.putDouble("arena", arenaRadius);
        if (runId != null) tag.putString("runId", runId);
        tag.putInt("fixedParty", fixedParty);
        if (arena2 != null) {
            tag.putDouble("a2x", arena2.x);
            tag.putDouble("a2y", arena2.y);
            tag.putDouble("a2z", arena2.z);
            tag.putDouble("a2r", arena2Radius);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("homeX")) home = new Vec3(tag.getDouble("homeX"), tag.getDouble("homeY"), tag.getDouble("homeZ"));
        if (tag.contains("arena")) arenaRadius = tag.getDouble("arena");
        if (tag.contains("runId")) runId = tag.getString("runId");
        fixedParty = tag.getInt("fixedParty");
        if (tag.contains("a2x")) {
            arena2 = new Vec3(tag.getDouble("a2x"), tag.getDouble("a2y"), tag.getDouble("a2z"));
            arena2Radius = tag.getDouble("a2r");
        }
    }

    // ------------------------------------------------------------------ помощники хранителей

    /** Единица силы ударов этого хранителя (здоровье бойца его уровня). */
    public double unit() {
        return BossDamage.unit(bossLevel());
    }

    /**
     * Подручный: монстр бестиария уровня хранителя у точки {@code at}. Убирается при сбросе и смерти хранителя,
     * добычи и опыта не даёт; сразу бросается на случайного игрока.
     */
    public com.andrey.litrpg.mob.DungeonMob summon(com.andrey.litrpg.mob.Bestiary.Species s, Vec3 at, com.andrey.litrpg.mob.MobRank rank) {
        if (!(level() instanceof ServerLevel sl)) return null;
        var mob = com.andrey.litrpg.mob.ModMobs.TYPES.get(s.id()).get().create(sl);
        if (mob == null) return null;
        mob.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
        mob.setup(bossLevel(), rank);
        // Бестиарий в фазе 12 стал крепче втрое — подручные хранителя остаются «между делом»: ÷2,5.
        var mhp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (mhp != null) {
            mhp.setBaseValue(mhp.getBaseValue() / 2.5);
            mob.setHealth(mob.getMaxHealth());
        }
        mob.setPersistenceRequired();
        mob.getPersistentData().putBoolean(com.andrey.litrpg.gear.LootService.NO_LOOT, true);
        sl.addFreshEntity(mob);
        track(mob);
        ServerPlayer t = randomNonTarget();
        if (t != null) {
            ThreatService.add(mob, t, 1);
            mob.setTarget(t);
        }
        Fx.burst(sl, at.add(0, 0.5, 0), 1f, 8, Fx.SHADOW);
        return mob;
    }

    /** Объект боя (узел силы, ледяная гробница, филактерия): ломается только руками игроков. */
    public <T extends PowerNode> T spawnNode(EntityType<T> type, Vec3 at, double hpUnits, String name) {
        if (!(level() instanceof ServerLevel sl)) return null;
        T n = type.create(sl);
        if (n == null) return null;
        n.moveTo(at.x, at.y, at.z, 0, 0);
        n.setHp(hpUnits * unit() * Math.max(0.6, partySize() / 3.0));
        if (name != null) {
            n.setCustomName(Component.literal(name).withStyle(ChatFormatting.AQUA));
            n.setCustomNameVisible(true);
        }
        sl.addFreshEntity(n);
        track(n);
        return n;
    }

    /**
     * Лужа на полу: {@code units} в секунду стоящим внутри, радиус растёт на {@code grow} в секунду до {@code maxRadius};
     * исчезает через {@code seconds}. {@code extra} — добавка попавшему (замедление и т.п.).
     */
    public void addPool(Vec3 at, double radius, double grow, double maxRadius, int seconds, double units, int color,
                        java.util.function.Consumer<ServerPlayer> extra) {
        Pool pool = new Pool(at, radius, grow, maxRadius, level().getGameTime() + seconds * 20L, units, color, extra);
        pool.label = hitLabel;
        pools.add(pool);
    }

    public int poolCount() {
        return pools.size();
    }

    /**
     * Зона на полу любой формы (фаза 13, «зонировать, а не закидывать»): {@code inside} — кто в зоне, {@code draw} —
     * как её рисовать (раз в полсекунды). Раз в секунду бьёт стоящих внутри на {@code units} и делает {@code extra}.
     */
    public static final class Zone {
        final java.util.function.Predicate<Vec3> inside;
        final java.util.function.Consumer<ServerLevel> draw;
        final long until;
        final double units;
        final java.util.function.Consumer<ServerPlayer> extra;
        final String label;

        Zone(java.util.function.Predicate<Vec3> inside, java.util.function.Consumer<ServerLevel> draw, long until, double units,
             java.util.function.Consumer<ServerPlayer> extra, String label) {
            this.inside = inside;
            this.draw = draw;
            this.until = until;
            this.units = units;
            this.extra = extra;
            this.label = label;
        }
    }

    private final List<Zone> zones = new ArrayList<>();

    /** Поставить зону на {@code ticks} тиков (см. {@link Zone}). */
    public void addZone(int ticks, double units, java.util.function.Predicate<Vec3> inside,
                        java.util.function.Consumer<ServerLevel> draw, java.util.function.Consumer<ServerPlayer> extra) {
        zones.add(new Zone(inside, draw, level().getGameTime() + ticks, units, extra, hitLabel));
    }

    public int zoneCount() {
        return zones.size();
    }

    private void tickZones(long now) {
        if (zones.isEmpty() || now % 10 != 0 || !(level() instanceof ServerLevel sl)) return;
        zones.removeIf(z -> now >= z.until);
        boolean hit = now % 20 == 0;
        for (Zone z : List.copyOf(zones)) {
            z.draw.accept(sl);
            if (!hit) continue;
            hitLabel = z.label;
            for (ServerPlayer pl : playersInArena()) {
                if (!z.inside.test(pl.position())) continue;
                if (z.units > 0) BossDamage.hit(this, pl, z.units);
                if (z.extra != null && pl.isAlive()) z.extra.accept(pl);
            }
            hitLabel = "Удар";
        }
    }

    private void tickPools(long now) {
        tickZones(now);
        if (pools.isEmpty() || now % 20 != 0 || !(level() instanceof ServerLevel sl)) return;
        pools.removeIf(p -> now >= p.until);
        for (Pool p : pools) {
            hitLabel = p.label;
            Fx.disc(sl, p.at, (float) p.radius, 21, p.color, false);
            for (ServerPlayer pl : playersInArena()) {
                double dx = pl.getX() - p.at.x, dz = pl.getZ() - p.at.z;
                if (dx * dx + dz * dz <= p.radius * p.radius) {
                    BossDamage.hit(this, pl, p.units);
                    if (p.extra != null && pl.isAlive()) p.extra.accept(pl);
                }
            }
            p.radius = Math.min(p.maxRadius, p.radius + p.grow);
        }
        hitLabel = "Удар";
    }

    /** Живые подручные этого хранителя. */
    public List<Entity> spawnedAlive() {
        if (!(level() instanceof ServerLevel sl)) return List.of();
        List<Entity> out = new ArrayList<>();
        for (UUID id : spawned) {
            Entity e = sl.getEntity(id);
            if (e != null && e.isAlive()) out.add(e);
        }
        return out;
    }

    // ------------------------------------------------------------------ для автотестов

    /** Начать навык по ключу сейчас же (без перезарядки и условий). */
    public void testStart(String key) {
        long now = level().getGameTime();
        for (Ability a : abilities()) {
            if (!a.key().equals(key)) continue;
            int ticks = (int) Math.round(p(a.key() + "_cast") * 20);
            casting = new Cast(a, getTarget(), ticks, ++castCounter, now + ticks);
            entityData.set(CAST_NAME, a.name() + (a.interruptible() ? "|1" : "|0") + "|" + ticks + "|" + (now + ticks));
            a.start().accept(casting);
            return;
        }
        throw new IllegalArgumentException("нет навыка " + key);
    }

    /** Пустая подготовка на {@code ticks} — чтобы проверить разметку отдельно от навыков. */
    public Cast testCast(int ticks) {
        Ability none = new Ability("Проверка", "test", 99, false, b -> false, c -> {
        });
        boolean inside = runningCast >= 0 && (runningCast == activeAbility || linked.contains(runningCast));
        // Разметка, поставленная изнутри приёма, — часть приёма: занятость ждёт и её, проверка пишет её под его именем.
        Cast c = new Cast(inside && activeRef != null ? activeRef : none, getTarget(), ticks, ++castCounter,
                level().getGameTime() + ticks);
        if (inside) linked.add(c.id);
        return c;
    }

    public boolean isCasting() {
        return casting != null;
    }

    /** Ближайший к цели игрок, кроме танка (для «разделённой ноши» и т.п.). */
    public ServerPlayer randomNonTarget() {
        List<ServerPlayer> list = new ArrayList<>(playersInArena());
        list.removeIf(p -> p == getTarget());
        if (list.isEmpty()) return getTarget() instanceof ServerPlayer p ? p : null;
        return list.get(random.nextInt(list.size()));
    }

    public List<ServerPlayer> playersByDistance() {
        List<ServerPlayer> list = new ArrayList<>(playersInArena());
        list.sort(Comparator.comparingDouble(this::distanceTo));
        return list;
    }
}
