package com.andrey.litrpg.mob;

import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.combat.entity.MagicBolt;
import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.core.ModAttachments;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.MobsBalance;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Монстр подземелья: вид ({@link Bestiary.Species}), уровень и ранг. Здоровье и урон — доли «единицы» (здоровья бойца
 * того же уровня), поведение — по роли. Над головой: «Ур. 7 · Костяной страж ★».
 */
public class DungeonMob extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int level = 1;
    private MobRank rank = MobRank.NORMAL;
    private boolean setUp;
    /** Место монстра отряда: утащили дальше поводка — возвращается и лечится. */
    private net.minecraft.core.BlockPos home;
    /** Имя мини-хранителя (null — обычный монстр). */
    private String title;
    private final List<long[]> pending = new ArrayList<>();
    private final List<Runnable> pendingRun = new ArrayList<>();
    /** Когда можно снова ударить приманку (двойника, зеркало, помощника). */
    private long nextDecoyHit;

    public DungeonMob(EntityType<? extends DungeonMob> type, Level lvl) {
        super(type, lvl);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    public Bestiary.Species species() {
        return ModMobs.speciesOf(getType());
    }

    public int mobLevel() {
        return level;
    }

    public MobRank rank() {
        return rank;
    }

    /** Отряд из схемы прохождения (null — монстр не из отряда). */
    public String packId() {
        var t = getPersistentData().getCompound("litrpg_run");
        return t.getString("kind").equals("pack") ? t.getString("id") : null;
    }

    public void setHome(net.minecraft.core.BlockPos pos) {
        home = pos;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    private double p(String key) {
        Bestiary.Species s = species();
        return Balance.get(Balance.MOBS).value(s.id(), key, s.param(key));
    }

    /** Задать уровень и ранг: здоровье, урон, скорость, имя. */
    public void setup(int level, MobRank rank) {
        this.level = Math.max(1, level);
        this.rank = rank;
        this.setUp = true;
        double unit = BossDamage.unit(this.level);
        set(Attributes.MAX_HEALTH, p("hp_units") * unit * rank.health);
        set(Attributes.ATTACK_DAMAGE, p("dmg_units") * unit * rank.damage);
        set(Attributes.MOVEMENT_SPEED, p("speed"));
        setHealth(getMaxHealth());
        updateName();
    }

    private void set(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, double v) {
        var inst = getAttribute(a);
        if (inst != null) inst.setBaseValue(v);
    }

    private void updateName() {
        Bestiary.Species s = species();
        ChatFormatting c = rank == MobRank.CHAMPION ? ChatFormatting.GOLD : rank == MobRank.ELITE ? ChatFormatting.AQUA : ChatFormatting.WHITE;
        setCustomName(Component.literal("Ур. " + level + " · ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal((title != null ? title : s.name()) + (rank.mark.isEmpty() ? "" : " " + rank.mark)).withStyle(c)));
        setCustomNameVisible(true);
    }

    // ------------------------------------------------------------------ поведение

    @Override
    protected void registerGoals() {
        MobRole role = species().role();
        goalSelector.addGoal(0, new FloatGoal(this));
        switch (role) {
            case ARCHER, CASTER -> goalSelector.addGoal(2, new ShootGoal(role == MobRole.CASTER));
            case HEALER -> {
                goalSelector.addGoal(1, new MendAlliesGoal());
                goalSelector.addGoal(2, new ShootGoal(true));
            }
            default -> goalSelector.addGoal(2, new MeleeAttackGoal(this, role == MobRole.SKIRMISHER || role == MobRole.SWARM ? 1.2 : 1.0, true));
        }
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e.distanceTo(this) <= Balance.get(Balance.MOBS).aggroRadius() && !ThreatService.hidden(e)));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!setUp) {
            Player near = level().getNearestPlayer(this, 32);
            setup(near instanceof ServerPlayer sp ? com.andrey.litrpg.player.PlayerService.data(sp).level() : 1, MobRank.NORMAL);
        }
        if (home != null && tickCount % 10 == 0) leash();
        tryLeap();
        long now = level().getGameTime();
        hitDecoy(now);
        if (tickCount % 40 == 7) eyeProps();
        for (int i = pending.size() - 1; i >= 0; i--) {
            if (now >= pending.get(i)[0]) {
                pending.remove(i);
                Runnable r = pendingRun.remove(i);
                if (isAlive()) r.run();
            }
        }
        // Элитный громила: удар по кругу вокруг себя — сначала разметка на полу.
        if (species().role() == MobRole.BRUTE && rank != MobRank.NORMAL && getTarget() != null
                && distanceTo(getTarget()) < 4 && tickCount % 160 == 0) {
            slam();
        }
    }

    /**
     * Приманка — не игрок: двойник, зеркало, помощник. Ванильный ближний бой к таким целям ходит плохо
     * (путь строится к блоку, а не к фигуре, и монстр замирает рядом), поэтому подходим и бьём сами,
     * в том же темпе, что и обычный удар. Стрелки и чародеи бьют приманку своими снарядами как обычно.
     */
    private void hitDecoy(long now) {
        LivingEntity t = getTarget();
        MobRole role = species().role();
        if (t == null || t instanceof Player || !t.isAlive() || role == MobRole.ARCHER || role == MobRole.CASTER
                || role == MobRole.HEALER) return;
        double reach = (getBbWidth() + t.getBbWidth()) * 0.5 + 1.0;
        double dx = t.getX() - getX(), dz = t.getZ() - getZ();
        if (dx * dx + dz * dz > reach * reach || Math.abs(t.getY() - getY()) > 2.5) {
            if (tickCount % 10 == 0) getNavigation().moveTo(t.getX(), t.getY(), t.getZ(), 1.15);
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(t, 30, 30);
        if (now < nextDecoyHit) return;
        nextDecoyHit = now + 20;
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        t.invulnerableTime = 0;
        doHurtTarget(t);
    }

    /**
     * Объекты классов можно сломать: монстр, бьющий игрока, иногда отвлекается на тотем, знамя, турель,
     * печать или другой объект рядом и бьёт его 3 с. Рукопашные — в 3,5 блока, стрелки и чародеи — в 8.
     */
    private void eyeProps() {
        if (!(getTarget() instanceof Player) || ThreatService.decoyOf(this) != null) return;
        MobRole role = species().role();
        boolean ranged = role == MobRole.ARCHER || role == MobRole.CASTER || role == MobRole.HEALER;
        if (role == MobRole.HEALER || getRandom().nextFloat() > (ranged ? 0.15f : 0.3f)) return;
        double r = ranged ? 8 : 3.5;
        level().getEntitiesOfClass(com.andrey.litrpg.skill.prop.ClassProp.class, getBoundingBox().inflate(r),
                        p -> p.isAlive() && p.distanceTo(this) <= r)
                .stream().min(java.util.Comparator.comparingDouble(p -> p.distanceToSqr(this)))
                .ifPresent(p -> ThreatService.decoy(this, p, 60));
    }

    /** Поводок: монстра не утащить через полкарты — возвращается на место с полным здоровьем. */
    private void leash() {
        double d = position().distanceTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(home));
        if (d > Balance.get(Balance.RUN).world().mobLeash()) {
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            getNavigation().stop();
            if (hasData(ModAttachments.THREAT)) ThreatService.table(this).clear();
            setTarget(null);
            StatusService.clear(this);
            setHealth(getMaxHealth());
            if (level() instanceof ServerLevel sl) Fx.burst(sl, position().add(0, 1, 0), 1.5f, 10, Fx.ARCANE);
        } else if (getTarget() == null && d > 3 && getNavigation().isDone()) {
            getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
        }
    }

    private void later(int ticks, Runnable r) {
        pending.add(new long[]{level().getGameTime() + ticks});
        pendingRun.add(r);
    }

    private void slam() {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 c = position();
        Fx.disc(sl, c, 3, 20, Fx.DANGER, true);
        getNavigation().stop();
        triggerAnim("main", "attack");
        later(20, () -> {
            double dmg = getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.6;
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(3.2))) {
                if (p.position().distanceToSqr(c) <= 9.5) {
                    p.invulnerableTime = 0;
                    p.hurt(damageSources().mobAttack(this), (float) dmg);
                }
            }
            Fx.ring(sl, c, 3.5f, 8, Fx.DANGER);
            sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.5f, 1.4f);
        });
    }

    /** Стая: заметил игрока — рядом стоящие сородичи без цели бросаются тоже. */
    @Override
    public void setTarget(LivingEntity target) {
        LivingEntity before = getTarget();
        super.setTarget(target);
        if (!level().isClientSide && before == null && target instanceof ServerPlayer p && getTarget() == p) {
            MobsBalance mb = Balance.get(Balance.MOBS);
            // В прохождении стая — это отряд из схемы (соседний отряд в том же зале не бросается следом);
            // вне прохождения — все сородичи в радиусе стаи.
            String pack = packId();
            double r = pack != null ? 24 : mb.packRadius();
            for (DungeonMob m : level().getEntitiesOfClass(DungeonMob.class, getBoundingBox().inflate(r))) {
                if (pack != null && !pack.equals(m.packId())) continue;
                if (m != this && m.getTarget() == null && m.isAlive()) {
                    ThreatService.add(m, p, 1);
                    m.setTarget(p);
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim("main", "attack");
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity l) onHitTarget(l);
        return hit;
    }

    /**
     * Особенность вида при попадании (рукой или снарядом): {@code hit_root} — приковывает на столько секунд,
     * {@code hit_dot} — урон со временем (доля удара в секунду, {@code dot_seconds} секунд).
     */
    void onHitTarget(LivingEntity l) {
        long now = level().getGameTime();
        double root = p("hit_root");
        if (root > 0) {
            StatusService.apply(l, Status.of(StatusType.ROOT, species().id() + "_root", getUUID(), 1, now, (int) Math.round(root * 20)));
            if (level() instanceof ServerLevel sl) Fx.disc(sl, l.position(), 0.8f, (int) Math.round(root * 20), species().color(), false);
        }
        double pull = p("hit_pull");
        if (pull > 0) {
            // Гарпун: подтащить попавшего на {@code hit_pull} блоков к стрелку.
            Vec3 to = position().subtract(l.position()).multiply(1, 0, 1);
            if (to.length() > 2) {
                Vec3 d = to.normalize().scale(Math.min(pull, to.length() - 1.5) * 0.35);
                l.push(d.x, 0.2, d.z);
                l.hurtMarked = true;
            }
        }
        double dot = p("hit_dot");
        if (dot > 0) {
            double perSec = getAttributeValue(Attributes.ATTACK_DAMAGE) * dot;
            int secs = (int) Math.max(1, Math.round(p("dot_seconds")));
            StatusService.apply(l, Status.periodic(StatusType.DOT, species().id() + "_dot", getUUID(), perSec, now, secs * 20, 20));
        }
    }

    /** Прыгун: раз в {@code leap} секунд прыгает на цель, если она в 4–10 блоках. */
    private void tryLeap() {
        LivingEntity t = getTarget();
        double cd = p("leap");
        if (cd <= 0 || t == null || !onGround() || tickCount % Math.max(20, (int) Math.round(cd * 20)) != 0) return;
        double d = distanceTo(t);
        if (d < 4 || d > 10) return;
        Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
        setDeltaMovement(dir.x * Math.min(1.3, d * 0.16), 0.45, dir.z * Math.min(1.3, d * 0.16));
        hasImpulse = true;
        triggerAnim("main", "leap");
        level().playSound(null, blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1f, 1.4f);
    }

    private long lastFlinch;

    /** Страж: щит спереди — урон в лицо втрое меньше. Тень: урон не от хозяина почти не проходит. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean blocked = false;
        if (!level().isClientSide && getPersistentData().hasUUID("litrpg_owner") && source.getEntity() instanceof ServerPlayer p
                && !p.getUUID().equals(getPersistentData().getUUID("litrpg_owner"))) {
            amount *= 0.1f;
        }
        if (!level().isClientSide && species().role() == MobRole.GUARD && source.getEntity() != null
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            Vec3 to = source.getEntity().position().subtract(position()).multiply(1, 0, 1).normalize();
            Vec3 facing = Vec3.directionFromRotation(0, yBodyRot);
            if (to.dot(facing) > 0.35) {
                amount *= 0.33f;
                triggerAnim("main", "block");
                blocked = true;
                if (source.getEntity() instanceof LivingEntity att) com.andrey.litrpg.combat.Popups.blocked(this, att);
                if (level() instanceof ServerLevel sl) {
                    Fx.burst(sl, position().add(facing.scale(0.6)).add(0, 1.2, 0), 0.7f, 5, 0xC8D0E0);
                    sl.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.8f, 0.9f);
                }
            }
        }
        boolean hit = super.hurt(source, amount);
        // «Больно» — вздрогнуть (не чаще раза в полсекунды и не когда удар принял щит).
        if (hit && !blocked && !level().isClientSide && amount > 0 && level().getGameTime() - lastFlinch > 10) {
            lastFlinch = level().getGameTime();
            triggerAnim("main", "hurt");
        }
        return hit;
    }

    // ------------------------------------------------------------------ цели: стрельба и лечение

    /** Держит дистанцию 6–13 блоков, стреляет; заклинатель перед выстрелом показывает полосу разметки. */
    private final class ShootGoal extends Goal {
        private final boolean caster;
        private int cooldown;

        ShootGoal(boolean caster) {
            this.caster = caster;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30, 30);
            double d = distanceTo(t);
            if (d < 6) {
                Vec3 away = DefaultRandomPos.getPosAway(DungeonMob.this, 8, 4, t.position());
                if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 1.15);
            } else if (d > 13 || !hasLineOfSight(t)) {
                getNavigation().moveTo(t, 1.0);
            } else {
                getNavigation().stop();
            }
            if (--cooldown <= 0 && d <= 16 && hasLineOfSight(t)) {
                cooldown = caster ? 60 : 40;
                if (caster && level() instanceof ServerLevel sl) {
                    Fx.line(sl, position(), t.position(), 0.6f, 16, species().color(), true);
                    triggerAnim("main", "cast");
                    later(16, () -> shoot(t));
                } else {
                    triggerAnim("main", "shoot");
                    shoot(t);
                }
            }
        }
    }

    private void shoot(LivingEntity t) {
        if (!t.isAlive()) return;
        MagicBolt bolt = new MagicBolt(level(), this, getAttributeValue(Attributes.ATTACK_DAMAGE), species().color());
        bolt.onHit = this::onHitTarget;
        Vec3 from = getEyePosition();
        Vec3 dir = t.getEyePosition().subtract(from).add(0, -0.3, 0);
        bolt.setPos(from.x, from.y - 0.2, from.z);
        bolt.shoot(dir.x, dir.y, dir.z, 1.4f, 2.5f);
        level().addFreshEntity(bolt);
        level().playSound(null, blockPosition(), species().role() == MobRole.ARCHER ? SoundEvents.SKELETON_SHOOT : SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE, 0.8f, 1.2f);
    }

    /** Лекарь: раз в 6 с лечит самого раненого сородича рядом (зелёный луч). */
    private final class MendAlliesGoal extends Goal {
        private int timer = 60;

        @Override
        public boolean canUse() {
            return getTarget() != null && --timer <= 0;
        }

        @Override
        public void start() {
            timer = 120;
            DungeonMob hurt = level().getEntitiesOfClass(DungeonMob.class, getBoundingBox().inflate(12),
                            m -> m.isAlive() && m.getHealth() < m.getMaxHealth() * 0.85).stream()
                    .min(java.util.Comparator.comparingDouble(m -> m.getHealth() / m.getMaxHealth())).orElse(null);
            if (hurt == null || !(level() instanceof ServerLevel sl)) return;
            hurt.heal((float) (hurt.getMaxHealth() * p("heal_share")));
            Fx.beam(sl, DungeonMob.this, hurt, 0.2f, 14, species().color());
            Fx.pillar(sl, hurt.position(), 0.6f, 2.5f, 14, species().color());
            triggerAnim("main", "cast");
            sl.playSound(null, blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 0.8f, 1.4f);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    // ------------------------------------------------------------------ сохранение и анимации

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("mobLevel", level);
        tag.putString("rank", rank.name());
        tag.putBoolean("setUp", setUp);
        if (home != null) tag.putLong("home", home.asLong());
        if (title != null) tag.putString("title", title);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        level = Math.max(1, tag.getInt("mobLevel"));
        try {
            rank = MobRank.valueOf(tag.getString("rank"));
        } catch (IllegalArgumentException ex) {
            rank = MobRank.NORMAL;
        }
        setUp = tag.getBoolean("setUp");
        if (tag.contains("home")) home = net.minecraft.core.BlockPos.of(tag.getLong("home"));
        if (tag.contains("title")) title = tag.getString("title");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String m = "animation." + ModMobs.speciesOf(getType()).model() + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(m + "idle"), walk = RawAnimation.begin().thenLoop(m + "walk");
        controllers.add(new AnimationController<>(this, "main", 4,
                state -> state.setAndContinue(state.isMoving() ? walk : idle))
                .triggerableAnim("attack", RawAnimation.begin().thenPlay(m + "attack"))
                .triggerableAnim("shoot", RawAnimation.begin().thenPlay(m + "shoot"))
                .triggerableAnim("cast", RawAnimation.begin().thenPlay(m + "cast"))
                .triggerableAnim("block", RawAnimation.begin().thenPlay(m + "block"))
                .triggerableAnim("leap", RawAnimation.begin().thenPlay(m + "leap"))
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay(m + "hurt")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /** Есть ли у монстра таблица угрозы (участвовал в бою). */
    public boolean engaged() {
        return hasData(ModAttachments.THREAT) && !ThreatService.table(this).isEmpty();
    }
}
