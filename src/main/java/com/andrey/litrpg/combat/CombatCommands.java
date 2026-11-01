package com.andrey.litrpg.combat;

import com.andrey.litrpg.combat.entity.TrainingDummy;
import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.party.PartyService;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Comparator;

/** Команды боя для проверки: манекен, лечение, провокация, таблица угрозы, отряд. */
public final class CombatCommands {
    private CombatCommands() {
    }

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("dummy").requires(s -> s.hasPermission(2))
                .executes(c -> dummy(c, false))
                .then(Commands.argument("aggressive", BoolArgumentType.bool())
                        .executes(c -> dummy(c, BoolArgumentType.getBool(c, "aggressive")))));
        root.then(Commands.literal("heal").requires(s -> s.hasPermission(2))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    var target = HealService.resolveTarget(p, 30);
                    float real = HealService.heal(p, target, DoubleArgumentType.getDouble(c, "amount"));
                    c.getSource().sendSuccess(() -> Component.literal("Вылечено " + Math.round(real) + " → "
                            + target.getName().getString()), false);
                    return 1;
                })));
        root.then(Commands.literal("taunt").requires(s -> s.hasPermission(2)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Mob m = nearestMob(p);
            if (m == null) return 0;
            ThreatService.taunt(m, p);
            c.getSource().sendSuccess(() -> Component.literal("Провокация: " + m.getName().getString()), false);
            return 1;
        }));
        root.then(Commands.literal("threat").requires(s -> s.hasPermission(2)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Mob m = nearestMob(p);
            if (m == null) return 0;
            c.getSource().sendSuccess(() -> Component.literal("Угроза — " + m.getName().getString() + ", цель: "
                    + (m.getTarget() != null ? m.getTarget().getName().getString() : "нет")), false);
            for (var e : ThreatService.sorted(m)) {
                ServerPlayer who = p.server.getPlayerList().getPlayer(e.getKey());
                String name = who != null ? who.getGameProfile().getName() : e.getKey().toString().substring(0, 8);
                c.getSource().sendSuccess(() -> Component.literal("  " + name + ": " + Math.round(e.getValue())), false);
            }
            return 1;
        }));
        root.then(Commands.literal("fx").requires(s -> s.hasPermission(2)).executes(c -> {
            // Витрина эффектов вокруг игрока — для проверки отрисовки.
            ServerPlayer p = c.getSource().getPlayerOrException();
            var l = p.serverLevel();
            var o = p.position();
            var fwd = p.getViewVector(1).multiply(1, 0, 1).normalize();
            var side = new net.minecraft.world.phys.Vec3(-fwd.z, 0, fwd.x);
            Fx.disc(l, o.add(fwd.scale(6)), 2.5f, 80, Fx.DANGER, true);
            Fx.donut(l, o.add(fwd.scale(6)).add(side.scale(7)), 1.5f, 3.5f, 80, Fx.DANGER, true);
            Fx.cone(l, o.add(fwd.scale(3)).add(side.scale(-6)), fwd, 5, 60, 80, Fx.FIRE, true);
            Fx.line(l, o.add(side.scale(3)), o.add(side.scale(3)).add(fwd.scale(10)), 2, 80, Fx.STORM, true);
            Fx.ring(l, o, 8, 30, Fx.ICE);
            Fx.pillar(l, o.add(fwd.scale(11)), 0.8f, 6, 80, Fx.HOLY);
            Fx.burst(l, o.add(fwd.scale(4)).add(0, 1.5, 0), 1.5f, 20, Fx.ARCANE);
            Fx.slash(l, p, 2.5f, 120, 10, Fx.BLOOD);
            Fx.mark(l, p, "⛨", 80, Fx.SHIELD);
            return 1;
        }));
        root.then(Commands.literal("polygon").requires(s -> s.hasPermission(2)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var center = p.blockPosition().offset(0, 0, 12);
            var boss = com.andrey.litrpg.boss.Polygon.build(p.serverLevel(), center);
            c.getSource().sendSuccess(() -> Component.literal("Полигон построен, Испытатель ждёт в центре."), true);
            return boss != null ? 1 : 0;
        }));
        root.then(Commands.literal("boss").requires(s -> s.hasPermission(2))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.serverLevel().getEntitiesOfClass(com.andrey.litrpg.boss.BossEntity.class, p.getBoundingBox().inflate(48))
                            .forEach(com.andrey.litrpg.boss.BossEntity::resetFight);
                    return 1;
                }))
                .then(Commands.literal("hp").then(Commands.argument("percent", DoubleArgumentType.doubleArg(1, 100)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    double pct = DoubleArgumentType.getDouble(c, "percent") / 100;
                    p.serverLevel().getEntitiesOfClass(com.andrey.litrpg.boss.BossEntity.class, p.getBoundingBox().inflate(48))
                            .forEach(b -> b.setHealth((float) (b.getMaxHealth() * pct)));
                    return 1;
                }))));
        root.then(Commands.literal("gear").requires(s -> s.hasPermission(2))
                .then(Commands.argument("rarity", StringArgumentType.word())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                java.util.Arrays.stream(com.andrey.litrpg.gear.Rarity.values()).map(r -> r.id), b))
                        .executes(c -> giveGear(c, null))
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(com.andrey.litrpg.gear.GearKind.values()).map(k -> k.id), b))
                                .executes(c -> giveGear(c, StringArgumentType.getString(c, "kind"))))));
        root.then(Commands.literal("coins").requires(s -> s.hasPermission(2))
                .then(Commands.argument("copper", com.mojang.brigadier.arguments.LongArgumentType.longArg(1)).executes(c -> {
                    com.andrey.litrpg.gear.Wallet.give(c.getSource().getPlayerOrException(),
                            com.mojang.brigadier.arguments.LongArgumentType.getLong(c, "copper"));
                    return 1;
                })));
        root.then(Commands.literal("trader").requires(s -> s.hasPermission(2))
                .then(Commands.argument("stage", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 5)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    var t = ModEntities.TRADER.get().create(p.serverLevel());
                    if (t == null) return 0;
                    int stage = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "stage");
                    t.setStage(stage, (stage - 1) * 10 + 5);
                    var look = p.getViewVector(1).multiply(1, 0, 1).normalize().scale(3);
                    t.moveTo(p.getX() + look.x, p.getY(), p.getZ() + look.z, p.getYRot() + 180, 0);
                    p.serverLevel().addFreshEntity(t);
                    return 1;
                })));
        root.then(Commands.literal("shop").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var t = p.serverLevel().getEntitiesOfClass(com.andrey.litrpg.shop.TraderEntity.class, p.getBoundingBox().inflate(8))
                    .stream().findFirst().orElse(null);
            if (t == null) return 0;
            com.andrey.litrpg.shop.ShopService.open(p, t);
            return 1;
        }));
        root.then(Commands.literal("mob").requires(s -> s.hasPermission(2))
                .then(Commands.argument("species", StringArgumentType.word())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                com.andrey.litrpg.mob.Bestiary.all().stream().map(com.andrey.litrpg.mob.Bestiary.Species::id), b))
                        .executes(c -> spawnMob(c, 0, "normal", 1))
                        .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 60))
                                .executes(c -> spawnMob(c, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "level"), "normal", 1))
                                .then(Commands.argument("rank", StringArgumentType.word())
                                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                java.util.List.of("normal", "elite", "champion"), b))
                                        .executes(c -> spawnMob(c, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "level"),
                                                StringArgumentType.getString(c, "rank"), 1))
                                        .then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 12))
                                                .executes(c -> spawnMob(c, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "level"),
                                                        StringArgumentType.getString(c, "rank"),
                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "count"))))))));
        root.then(Commands.literal("party")
                .then(Commands.literal("form").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("players", EntityArgument.players()).executes(c -> {
                            var list = new ArrayList<>(EntityArgument.getPlayers(c, "players"));
                            PartyService.form(list);
                            return list.size();
                        })))
                .then(Commands.literal("leave").executes(c -> {
                    PartyService.leave(c.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private static int dummy(CommandContext<CommandSourceStack> c, boolean aggressive) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        TrainingDummy d = ModEntities.TRAINING_DUMMY.get().create(p.serverLevel());
        if (d == null) return 0;
        var look = p.getViewVector(1).multiply(1, 0, 1).normalize().scale(3);
        d.moveTo(p.getX() + look.x, p.getY(), p.getZ() + look.z, p.getYRot() + 180, 0);
        d.setYHeadRot(p.getYRot() + 180);
        d.setYBodyRot(p.getYRot() + 180);
        d.setAggressive(aggressive);
        p.serverLevel().addFreshEntity(d);
        return 1;
    }

    private static int giveGear(CommandContext<CommandSourceStack> c, String kindId) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        var rarity = java.util.Arrays.stream(com.andrey.litrpg.gear.Rarity.values())
                .filter(r -> r.id.equals(StringArgumentType.getString(c, "rarity"))).findFirst()
                .orElse(com.andrey.litrpg.gear.Rarity.COMMON);
        var d = com.andrey.litrpg.player.PlayerService.data(p);
        var stack = kindId == null
                ? com.andrey.litrpg.gear.GearGen.roll(p.getRandom(), d.role(), d.level(), rarity)
                : com.andrey.litrpg.gear.GearGen.make(java.util.Arrays.stream(com.andrey.litrpg.gear.GearKind.values())
                        .filter(k -> k.id.equals(kindId)).findFirst().orElse(com.andrey.litrpg.gear.GearKind.SWORD),
                d.level(), rarity, d.role(), p.getRandom());
        if (!p.getInventory().add(stack)) p.drop(stack, false);
        return 1;
    }

    private static int spawnMob(CommandContext<CommandSourceStack> c, int level, String rank, int count) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        var holder = com.andrey.litrpg.mob.ModMobs.TYPES.get(StringArgumentType.getString(c, "species"));
        if (holder == null) return 0;
        int lv = level > 0 ? level : com.andrey.litrpg.player.PlayerService.data(p).level();
        var r = switch (rank) {
            case "elite" -> com.andrey.litrpg.mob.MobRank.ELITE;
            case "champion" -> com.andrey.litrpg.mob.MobRank.CHAMPION;
            default -> com.andrey.litrpg.mob.MobRank.NORMAL;
        };
        var fwd = p.getViewVector(1).multiply(1, 0, 1).normalize();
        for (int i = 0; i < count; i++) {
            var m = holder.get().create(p.serverLevel());
            if (m == null) continue;
            m.moveTo(p.getX() + fwd.x * 7 + (i % 4) * 1.2 - 1.8, p.getY(), p.getZ() + fwd.z * 7 + (i / 4) * 1.2,
                    p.getYRot() + 180, 0);
            m.setYBodyRot(p.getYRot() + 180);
            m.setYHeadRot(p.getYRot() + 180);
            m.setup(lv, r);
            p.serverLevel().addFreshEntity(m);
        }
        return count;
    }

    private static Mob nearestMob(ServerPlayer p) {
        return p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16), Mob::isAlive).stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(p))).orElse(null);
    }
}
