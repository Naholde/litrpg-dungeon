package com.andrey.litrpg.combat;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.StatBreakdown;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.andrey.litrpg.core.ModAttachments;
import com.andrey.litrpg.skill.SkillService;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Конвейер урона. Любой урон в игре проходит через {@link #onIncoming}: усиление характеристиками атакующего,
 * крит, уклонение, стойкость, урон по своим, окружение в масштабе здоровья.
 * Урон навыков и посоха наносится через {@link #deal} — он помечает род урона и снимает ванильную неуязвимость.
 */
public final class DamageService {
    /** Что знает конвейер о текущем ударе, нанесённом через {@link #deal}. */
    private record Context(WeaponKind kind) {
    }

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();
    /** Удар был критическим — узнаём в {@code LivingDamageEvent.Post}, чтобы показать цифру крита. */
    private static final Map<LivingEntity, Boolean> CRIT = new WeakHashMap<>();

    private DamageService() {
    }

    /**
     * Нанести урон от имени атакующего (навык, посох). {@code base} — урон до усиления характеристиками.
     * Ванильная неуязвимость после удара (полсекунды) сбрасывается: иначе частые удары навыков «отскакивают».
     */
    public static boolean deal(LivingEntity attacker, Entity direct, LivingEntity target, double base, WeaponKind kind) {
        DamageSource src = kind == WeaponKind.MELEE
                ? (attacker instanceof net.minecraft.world.entity.player.Player p
                    ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker))
                : attacker.damageSources().indirectMagic(direct != null ? direct : attacker, attacker);
        target.invulnerableTime = 0;
        CONTEXT.set(new Context(kind));
        try {
            return target.hurt(src, (float) base);
        } finally {
            CONTEXT.remove();
        }
    }

    /** Автотесты: без случайных критов и уклонений, чтобы цифры сравнивались точно. */
    public static boolean deterministic;

    public static boolean wasCrit(LivingEntity victim) {
        return Boolean.TRUE.equals(CRIT.remove(victim));
    }

    /** Ноги в воде (по блоку, а не по флагу движения — так работает и у игрока без тика, в тестах). */
    public static boolean underWater(LivingEntity v) {
        return v.level().getFluidState(v.blockPosition()).is(net.minecraft.tags.FluidTags.WATER);
    }

    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource src = e.getSource();
        Entity attacker = src.getEntity();
        double amount = e.getAmount();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // /kill и падение в пустоту — как есть

        Context ctx = CONTEXT.get();
        boolean fixed = ctx != null && ctx.kind() == WeaponKind.FIXED;

        // 0. Оглушённый не бьёт (удар, начатый до оглушения, тоже не проходит).
        if (attacker instanceof LivingEntity la && ctx == null && src.getDirectEntity() == attacker
                && StatusService.has(la, StatusType.STUN)) {
            e.setCanceled(true);
            return;
        }

        // 0.5. «Удары мимо»: зеркала, тьма, пируэт — обычный удар по носителю проходит мимо.
        if (attacker instanceof LivingEntity && src.getDirectEntity() == attacker && !fixed && !deterministic
                && StatusService.has(victim, StatusType.BLUR)
                && victim.getRandom().nextDouble() < Math.min(0.9, StatusService.sum(victim, StatusType.BLUR))) {
            e.setCanceled(true);
            if (victim instanceof ServerPlayer vp) Popups.dodge(vp);
            else Popups.missed(victim, (LivingEntity) attacker);
            return;
        }

        // 1. Атакующий — игрок: усиление характеристиками и крит.
        if (attacker instanceof ServerPlayer p && attacker != victim && !fixed) {
            StatBreakdown b = PlayerService.breakdown(p);
            WeaponKind kind = ctx != null ? ctx.kind() : kindOf(src, p);
            amount = DamageMath.outgoing(amount, kind, b);
            if (!deterministic && p.getRandom().nextDouble() < b.get(Derived.CRIT_CHANCE)) {
                amount *= b.get(Derived.CRIT_DAMAGE);
                CRIT.put(victim, true);
            }
            amount *= com.andrey.litrpg.gear.GearEffects.outgoing(p, victim);
        }
        if (attacker instanceof LivingEntity la && !fixed) amount *= StatusService.product(la, StatusType.DAMAGE_DEALT);
        amount *= levelGap(attacker, victim);

        // 2. Метки жертвы: уязвимость/защита, затем «защитник» забирает долю себе.
        // Снижения урона перемножаются (купол × знамя × стойка…), но вместе — не больше −75%: бессмертия нет.
        amount *= Math.max(0.25, StatusService.product(victim, StatusType.DAMAGE_TAKEN));
        if (!REDIRECTING.get()) amount = redirectToGuardian(victim, src, amount);

        // 3. Жертва — игрок.
        if (victim instanceof ServerPlayer vp) {
            StatBreakdown b = PlayerService.breakdown(vp);
            // Сложность забега: удары хранителей и монстров (и их снарядов) на обычной слабее.
            if (attacker instanceof LivingEntity && !(attacker instanceof net.minecraft.world.entity.player.Player)) {
                amount *= com.andrey.litrpg.run.Difficulty.active(vp.server).damage();
            }
            if (attacker instanceof ServerPlayer && attacker != victim) {
                amount *= Balance.get(Balance.COMBAT).friendlyFireMult();
            } else if (attacker == null) {
                if (src.is(DamageTypeTags.IS_FALL) && SkillService.state(vp).leaping()) {
                    e.setCanceled(true); // приземление после прыжка-навыка не ранит
                    return;
                }
                if (src.is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR) && underWater(vp)) {
                    e.setCanceled(true); // магма под водой остужена: дно водоворота (этап 3) не жжёт, сухая магма — жжёт
                    return;
                }
                if (Balance.get(Balance.COMBAT).environmentScalesWithHealth()) {
                    amount = DamageMath.environment(amount, vp.getMaxHealth());
                }
            } else if (attacker instanceof LivingEntity && !isBoss(attacker) && vp.invulnerableTime <= 10
                    && !deterministic && vp.getRandom().nextDouble() < b.get(Derived.DODGE)) {
                // Уклонение — только от монстров; удары хранителей читаются по разметке, от них уходят ногами.
                e.setCanceled(true);
                Popups.dodge(vp);
                return;
            }
            if (attacker instanceof LivingEntity && attacker != vp && !fixed) {
                amount = DamageMath.armor(amount, b.get(Derived.ARMOR), PlayerService.data(vp).level(),
                        Balance.get(Balance.GEAR).armor());
            }
            amount = DamageMath.incoming(amount, b, isBoss(attacker));
            if (attacker instanceof LivingEntity la && la != vp && !fixed) amount *= com.andrey.litrpg.gear.GearEffects.incoming(vp, la);
        }

        // 4. Несокрушимость: смертельный удар оставляет 1 здоровья.
        if (StatusService.has(victim, StatusType.WARD) && amount >= victim.getHealth() + victim.getAbsorptionAmount()) {
            amount = Math.max(0, victim.getHealth() + victim.getAbsorptionAmount() - 1);
            Popups.ward(victim);
        }

        // 5. Щиты впитывают остаток.
        if (victim.hasData(ModAttachments.STATUSES)) {
            double before = amount;
            amount = StatusService.list(victim).absorb(amount, victim.level().getGameTime());
            if (amount < before && victim instanceof ServerPlayer vp) StatusService.sync(vp);
            if (amount <= 0) {
                e.setCanceled(true);
                Popups.absorbed(victim, attacker instanceof LivingEntity l ? l : null);
                return;
            }
        }
        e.setAmount((float) amount);
    }

    private static final ThreadLocal<Boolean> REDIRECTING = ThreadLocal.withInitial(() -> false);

    /** «Под защитой»: доля урона уходит защитнику и бьёт его по его собственным правилам (стойкость, роль). */
    private static double redirectToGuardian(LivingEntity victim, DamageSource src, double amount) {
        if (!victim.hasData(ModAttachments.STATUSES) || !(victim.level() instanceof ServerLevel sl)) return amount;
        long now = sl.getGameTime();
        for (var s : StatusService.list(victim).all()) {
            if (s.type() != StatusType.GUARD || s.expired(now) || s.source() == null) continue;
            if (!(sl.getEntity(s.source()) instanceof LivingEntity guard) || !guard.isAlive() || guard == victim) continue;
            double part = amount * s.magnitude();
            amount -= part;
            REDIRECTING.set(true);
            try {
                guard.invulnerableTime = 0;
                guard.hurt(src, (float) part);
            } finally {
                REDIRECTING.set(false);
            }
            break;
        }
        return amount;
    }

    /** После удара: вампиризм атакующего, отражение жертвы, клеймо, искупление и счёт урона. */
    static void afterHit(LivingEntity victim, DamageSource src, float dealt) {
        if (dealt > 0) {
            // Счёт урона: метка копит всё, что пришло по носителю (стекло зерцала, приговор, перемотка).
            if (StatusService.has(victim, StatusType.RECORD)) {
                StatusService.accumulate(victim, StatusType.RECORD, dealt);
                if (victim instanceof ServerPlayer vp) {
                    // Стекло зерцала считается в долях его здоровья: полная полоса — впитана целая жизнь.
                    com.andrey.litrpg.skill.SkillService.addIf(vp, com.andrey.litrpg.skill.Resource.GLASS,
                            dealt / Math.max(1, vp.getMaxHealth()) * 100);
                }
            }
            // Клеймо: доля урона по носителю лечит того, кто его поставил.
            siphon(victim, dealt);
        }
        if (!(src.getEntity() instanceof LivingEntity attacker) || attacker == victim || dealt <= 0) return;
        // Особые свойства вещей: кровопийца и искра у атакующего, шипы и второе дыхание у жертвы.
        if (attacker instanceof ServerPlayer sp && !(victim instanceof ServerPlayer)) com.andrey.litrpg.gear.GearEffects.afterDealt(sp, victim, dealt);
        if (victim instanceof ServerPlayer vp && !(attacker instanceof ServerPlayer)) {
            com.andrey.litrpg.gear.GearEffects.afterTaken(vp, attacker, src.getDirectEntity() == attacker && CONTEXT.get() == null, dealt);
        }
        double leech = StatusService.sum(attacker, StatusType.LEECH);
        if (leech > 0) HealService.healFixed(attacker instanceof ServerPlayer sp ? sp : null, attacker, dealt * leech);
        if (attacker instanceof ServerPlayer sp) atonement(sp, dealt);
        double reflect = StatusService.sum(victim, StatusType.REFLECT);
        // Отражается только удар вблизи (не снаряды и не сам отражённый урон — иначе бесконечный пинг-понг).
        if (reflect > 0 && src.getDirectEntity() == attacker && CONTEXT.get() == null) {
            deal(victim, null, attacker, dealt * reflect, WeaponKind.FIXED);
        }
    }

    /** Клеймо кровавого рыцаря: доля урона по клеймённому возвращается тому, кто клеймо поставил. */
    private static void siphon(LivingEntity victim, float dealt) {
        if (!victim.hasData(ModAttachments.STATUSES) || !(victim.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        for (var s : StatusService.list(victim).all()) {
            if (s.type() != StatusType.SIPHON || s.expired(now) || s.source() == null) continue;
            if (sl.getServer().getPlayerList().getPlayer(s.source()) instanceof ServerPlayer owner && owner.isAlive()) {
                HealService.healFixed(owner, owner, dealt * s.magnitude());
            }
        }
    }

    /** Искупление теневого жреца: его урон лечит того, на кого наложена метка. */
    private static void atonement(ServerPlayer attacker, float dealt) {
        if (!(attacker.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        for (ServerPlayer ally : sl.players()) {
            if (!ally.isAlive() || !ally.hasData(ModAttachments.STATUSES)) continue;
            for (var s : StatusService.list(ally).all()) {
                if (s.type() == StatusType.ATONEMENT && !s.expired(now) && attacker.getUUID().equals(s.source())) {
                    HealService.healFixed(attacker, ally, dealt * s.magnitude());
                }
            }
        }
    }

    /** Уровень монстра или хранителя (−1 — у сущности нет уровня Системы). */
    public static int levelOf(Entity e) {
        if (e instanceof com.andrey.litrpg.mob.DungeonMob m) return m.mobLevel();
        if (e instanceof com.andrey.litrpg.boss.BossEntity b) return b.bossLevel();
        if (e instanceof ServerPlayer p) return PlayerService.data(p).level();
        return -1;
    }

    /**
     * Разница уровней: монстр выше игрока — игрок бьёт слабее (до 40% урона), а монстр бьёт сильнее
     * (на 10 уровней выше — почти вдвое). Хранителей это не касается — их урон уже в долях здоровья.
     */
    static double levelGap(Entity attacker, LivingEntity victim) {
        var mb = Balance.get(Balance.MOBS);
        if (attacker instanceof ServerPlayer p && victim instanceof com.andrey.litrpg.mob.DungeonMob m) {
            int diff = m.mobLevel() - PlayerService.data(p).level();
            if (diff > 0) return Math.max(mb.gapMinDamage(), 1 - mb.gapPlayerDamage() * diff);
        }
        if (victim instanceof ServerPlayer p && attacker instanceof com.andrey.litrpg.mob.DungeonMob m) {
            int diff = m.mobLevel() - PlayerService.data(p).level();
            if (diff > 0) return 1 + mb.gapMobDamage() * diff;
        }
        return 1;
    }

    private static WeaponKind kindOf(DamageSource src, ServerPlayer p) {
        if (src.getDirectEntity() instanceof AbstractArrow) return WeaponKind.RANGED;
        if (src.getDirectEntity() == p) return WeaponKind.MELEE;
        return WeaponKind.PLAIN;
    }

    /** Хранитель — сущность из тега {@code litrpg:bosses} (появятся в фазе хранителей). */
    public static boolean isBoss(Entity e) {
        return e != null && e.getType().is(com.andrey.litrpg.core.ModTags.BOSSES);
    }
}
