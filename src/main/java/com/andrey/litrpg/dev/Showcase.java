package com.andrey.litrpg.dev;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;

/**
 * Витрина моделей для проверки без боя: {@code /litrpg show <сущность>} ставит модель в 6 блоках перед игроком
 * лицом к нему — без ИИ, навыков и урона; {@code /litrpg anim <анимация>} проигрывает анимацию у всех моделей витрины
 * рядом; {@code /litrpg show clear} убирает витрину.
 */
public final class Showcase {
    public static final String TAG = "litrpg_show";

    private Showcase() {
    }

    /** Модель витрины: ничего не делает, только стоит и проигрывает анимации. */
    public static boolean is(Entity e) {
        return e.getPersistentData().getBoolean(TAG);
    }

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("show").requires(s -> s.hasPermission(2))
                .then(Commands.literal("clear").executes(Showcase::clear))
                .then(Commands.argument("entity", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                                .filter(k -> k.getNamespace().equals("litrpg")).map(ResourceLocation::getPath), b))
                        .executes(Showcase::show)));
        root.then(Commands.literal("anim").requires(s -> s.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word()).executes(c -> anim(c, 0))
                        .then(Commands.argument("every", com.mojang.brigadier.arguments.IntegerArgumentType.integer(5, 200))
                                .executes(c -> anim(c, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "every"))))));
    }

    private static int show(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        var id = ResourceLocation.fromNamespaceAndPath("litrpg", StringArgumentType.getString(c, "entity"));
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            c.getSource().sendFailure(Component.literal("Нет сущности " + id));
            return 0;
        }
        Entity e = type.create(p.serverLevel());
        if (e == null) return 0;
        var fwd = p.getViewVector(1).multiply(1, 0, 1).normalize();
        float yaw = p.getYRot() + 180;
        e.moveTo(p.getX() + fwd.x * 6, p.getY(), p.getZ() + fwd.z * 6, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.getPersistentData().putBoolean(TAG, true);
        e.setInvulnerable(true);
        e.setSilent(true);
        if (e instanceof Mob m) {
            m.setNoAi(true);
            m.setPersistenceRequired();
        }
        p.serverLevel().addFreshEntity(e);
        c.getSource().sendSuccess(() -> Component.literal("Витрина: " + id), false);
        return 1;
    }

    /** {@code every} > 0 — повторять каждые столько тиков 12 секунд: снимок в любой момент попадёт в движение. */
    private static int anim(CommandContext<CommandSourceStack> c, int every) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "name");
        int n = play(p, name);
        if (every > 0) {
            for (int t = every; t < 240; t += every) com.andrey.litrpg.skill.Later.run(p.serverLevel(), t, () -> play(p, name));
        }
        int count = n;
        c.getSource().sendSuccess(() -> Component.literal("Анимация " + name + ": " + count), false);
        return n;
    }

    private static int play(ServerPlayer p, String name) {
        int n = 0;
        for (Entity e : p.serverLevel().getEntities(p, p.getBoundingBox().inflate(32), Showcase::is)) {
            if (e instanceof GeoEntity g) {
                g.stopTriggeredAnim("main", name);
                g.triggerAnim("main", name);
                n++;
            }
        }
        return n;
    }

    private static int clear(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        int n = 0;
        for (Entity e : p.serverLevel().getEntities(p, p.getBoundingBox().inflate(64), Showcase::is)) {
            e.discard();
            n++;
        }
        int count = n;
        c.getSource().sendSuccess(() -> Component.literal("Витрина убрана: " + count), false);
        return n;
    }
}
