package com.andrey.litrpg.combat;

import com.andrey.litrpg.combat.entity.TrainingDummy;
import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.party.PartyService;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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

    private static Mob nearestMob(ServerPlayer p) {
        return p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16), Mob::isAlive).stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(p))).orElse(null);
    }
}
