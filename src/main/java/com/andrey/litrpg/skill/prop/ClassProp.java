package com.andrey.litrpg.skill.prop;

import com.andrey.litrpg.fx.Fx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Объект класса в мире: тотем, знамя, турель, двойник, светило, печать. Стоит на месте, рисует на земле свой
 * радиус, живёт заданное время, имеет здоровье — и враги могут его сломать (сломали — его действие кончилось).
 * <p>Что объект делает, задаёт сам навык: {@link #onPulse} раз в {@code interval} тиков и {@link #onBreak}
 * при поломке. Эти действия живут только в текущей игре (перезапуск сервера объект убирает).
 */
public class ClassProp extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID owner;
    private long until;
    private double radius;
    private int interval = 20;
    private Consumer<ClassProp> pulse;
    private Consumer<ClassProp> broke;
    /** Счётчик для объектов, которым нужно состояние (турель — номер выстрела, печать — какая по счёту). */
    public int counter;

    public ClassProp(EntityType<? extends ClassProp> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoGravity(true);
        setInvulnerable(false);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.MOVEMENT_SPEED, 0);
    }

    /** Поставить объект: здоровье в долях единицы силы задаёт навык, радиус рисуется на земле. */
    public static ClassProp spawn(ServerLevel level, EntityType<? extends ClassProp> type, Vec3 at, LivingEntity owner,
                                  double hp, double radius, int durationTicks) {
        ClassProp p = type.create(level);
        if (p == null) throw new IllegalStateException("не создался объект класса " + type);
        p.moveTo(at.x, at.y, at.z, owner.getYRot(), 0);
        p.owner = owner.getUUID();
        p.until = level.getGameTime() + durationTicks;
        p.radius = radius;
        p.setHp(hp);
        level.addFreshEntity(p);
        p.ring();
        level.playSound(null, p.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);
        return p;
    }

    public ClassProp onPulse(int intervalTicks, Consumer<ClassProp> action) {
        this.interval = Math.max(1, intervalTicks);
        this.pulse = action;
        return this;
    }

    public ClassProp onBreak(Consumer<ClassProp> action) {
        this.broke = action;
        return this;
    }

    public void setHp(double hp) {
        var a = getAttribute(Attributes.MAX_HEALTH);
        if (a != null) a.setBaseValue(Math.max(1, hp));
        setHealth((float) Math.max(1, hp));
    }

    public UUID ownerId() {
        return owner;
    }

    /** Хозяин объекта, если он в игре и жив (иначе null — объект доживает сам). */
    public net.minecraft.server.level.ServerPlayer ownerPlayer() {
        if (owner == null || !(level() instanceof ServerLevel sl) || sl.getServer() == null) return null;
        var p = sl.getServer().getPlayerList().getPlayer(owner);
        return p != null && p.isAlive() ? p : null;
    }

    public double radius() {
        return radius;
    }

    public PropKind kind() {
        return PropKind.get(BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath());
    }

    private void ring() {
        if (!(level() instanceof ServerLevel sl) || radius <= 0) return;
        PropKind k = kind();
        Fx.disc(sl, position(), (float) radius, 21, k != null ? k.color() : Fx.ARCANE, false);
    }

    /** Ломают только враги: свой отряд объект не разбивает (и своими навыками по площади тоже). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player) return false;
        if (source.getEntity() instanceof ClassProp || source.getEntity() instanceof SummonEntity) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, 0, 0);
        if (!(level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        if (now >= until) {
            vanish();
            return;
        }
        if (tickCount % 20 == 0) ring();
        if (pulse != null && tickCount % interval == 0) pulse.accept(this);
    }

    /** Время вышло: объект тихо гаснет (это не поломка — действие просто кончилось). */
    public void vanish() {
        if (level() instanceof ServerLevel sl) {
            Fx.burst(sl, position().add(0, 0.5, 0), 0.8f, 8, kind() != null ? kind().color() : Fx.ARCANE);
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            PropKind k = kind();
            Fx.burst(sl, position().add(0, 0.8, 0), 1.6f, 12, k != null ? k.color() : Fx.ARCANE);
            sl.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 1.1f, 0.8f);
            var p = ownerPlayer();
            if (p != null && k != null) {
                com.andrey.litrpg.core.SystemChat.send(p, net.minecraft.network.chat.Component
                        .literal(k.title() + " сломан.").withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
        if (broke != null) broke.accept(this);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    /** Перезапуск сервера объекты классов не переживают: действие живёт в памяти. */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation."
                        + BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath() + ".idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
