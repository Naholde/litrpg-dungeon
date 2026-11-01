package com.andrey.litrpg.client.anim;

import com.andrey.litrpg.anim.Anim;
import com.andrey.litrpg.anim.Anim.Part;
import com.andrey.litrpg.anim.AnimPose;
import com.andrey.litrpg.anim.Anims;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.mojang.math.Axis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Движения игроков на клиенте: что кто сейчас делает и как от этого гнутся руки, ноги и всё тело.
 * <p>Части тела поворачивает {@link #pose} — его зовёт примесь в модель игрока сразу после обычной позы,
 * поэтому за руками идут и рукава, и броня, и оружие. Всё тело двигает {@link #pushRoot} при отрисовке,
 * а {@link #firstPerson} повторяет движение руки от первого лица.
 */
public final class ClientAnims {
    private record Active(Anim anim, float speed, long start) {
    }

    private static final Map<Integer, Active> ACTIVE = new HashMap<>();
    private static final AnimPose POSE = new AnimPose();
    private static final AnimPose ROOT = new AnimPose();
    private static final AnimPose HAND = new AnimPose();
    private static int pushed;
    private static float[] appliedToHand;

    private ClientAnims() {
    }

    public static void play(int entity, String anim, float speed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (anim == null || anim.isEmpty()) {
            ACTIVE.remove(entity);
            return;
        }
        Anim a = Anims.get(anim);
        if (a == null) return;
        ACTIVE.put(entity, new Active(a, speed, mc.level.getGameTime()));
    }

    public static void clear() {
        ACTIVE.clear();
        pushed = 0;
        appliedToHand = null;
    }

    /** Раз в тик убрать то, что уже доиграло (иначе карта растёт). */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            clear();
            return;
        }
        long now = mc.level.getGameTime();
        for (Iterator<Map.Entry<Integer, Active>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            Active a = it.next().getValue();
            if (!a.anim.loop && now - a.start > a.anim.ticks * a.speed + 2) it.remove();
        }
    }

    private static float now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
    }

    /** Снять позу движения сущности; {@code false} — движения нет. */
    private static boolean sample(int entity, AnimPose out) {
        Active a = ACTIVE.get(entity);
        if (a == null || Minecraft.getInstance().level == null) return false;
        float t = now() - a.start;
        if (!a.anim.loop && t > a.anim.ticks * a.speed) return false;
        out.sample(a.anim, t, a.speed);
        return out.weight > 0.001f;
    }

    // ------------------------------------------------------------------ части тела (из примеси)

    /** Доворот частей модели под текущее движение. Зовётся сразу после обычной позы игрока. */
    public static void pose(PlayerModel<?> model, LivingEntity entity, float ageInTicks) {
        if (!(entity instanceof Player) || !sample(entity.getId(), POSE)) return;
        float w = POSE.weight;
        apply(model.head, Part.HEAD, w);
        apply(model.body, Part.BODY, w);
        apply(model.rightArm, Part.ARM_R, w);
        apply(model.leftArm, Part.ARM_L, w);
        apply(model.rightLeg, Part.LEG_R, w);
        apply(model.leftLeg, Part.LEG_L, w);
        // Верхний слой скина копируется с частей тела до нас — повторяем, иначе рукава отстанут от рук.
        model.hat.copyFrom(model.head);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
        model.jacket.copyFrom(model.body);
    }

    private static void apply(ModelPart part, Part p, float w) {
        if (!POSE.has(p)) return;
        float x = (float) Math.toRadians(POSE.x(p)), y = (float) Math.toRadians(POSE.y(p)), z = (float) Math.toRadians(POSE.z(p));
        if (POSE.additive(p)) {
            part.xRot += x * w;
            part.yRot += y * w;
            part.zRot += z * w;
        } else {
            part.xRot = Mth.lerp(w, part.xRot, x);
            part.yRot = Mth.lerp(w, part.yRot, y);
            part.zRot = Mth.lerp(w, part.zRot, z);
        }
        part.x += POSE.ox(p) * w;
        part.y += POSE.oy(p) * w;
        part.z += POSE.oz(p) * w;
    }

    // ------------------------------------------------------------------ всё тело (при отрисовке игрока)

    /**
     * Сдвиг и наклон всего игрока. Оси берутся по его развороту: {@code oz} — вперёд, {@code ox} — вправо,
     * {@code oy} — вверх; {@code x} — наклон вперёд, {@code y} — оборот вокруг себя, {@code z} — на правый бок.
     */
    public static void pushRoot(Player player, PoseStack ps, float partial) {
        if (!sample(player.getId(), ROOT) || !ROOT.has(Part.ROOT)) return;
        float w = ROOT.weight;
        float ox = ROOT.ox(Part.ROOT) * w, oy = ROOT.oy(Part.ROOT) * w, oz = ROOT.oz(Part.ROOT) * w;
        float rx = ROOT.x(Part.ROOT) * w, ry = ROOT.y(Part.ROOT) * w, rz = ROOT.z(Part.ROOT) * w;
        if (ox == 0 && oy == 0 && oz == 0 && rx == 0 && ry == 0 && rz == 0) return;
        float yaw = 180 - Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        // В этой системе отсчёта вперёд — это −z, вправо — +x (как у самой игры после разворота по телу).
        ps.translate(ox, oy, -oz);
        if (ry != 0) ps.mulPose(Axis.YP.rotationDegrees(ry));
        if (rx != 0) ps.mulPose(Axis.XP.rotationDegrees(-rx));
        if (rz != 0) ps.mulPose(Axis.ZP.rotationDegrees(-rz));
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        pushed++;
    }

    public static void popRoot(PoseStack ps) {
        if (pushed > 0) {
            ps.popPose();
            pushed--;
        }
    }

    /** Крутится ли игрок сейчас (тогда ник над головой лучше спрятать — он крутился бы вместе с ним). */
    public static boolean spinning(Entity e) {
        return sample(e.getId(), ROOT) && ROOT.has(Part.ROOT) && Math.abs(ROOT.y(Part.ROOT)) > 25;
    }

    // ------------------------------------------------------------------ вид от первого лица

    /** Повторить движение руки на оружии, которое игрок видит перед собой. */
    public static void firstPerson(InteractionHand hand, PoseStack ps) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (hand == InteractionHand.OFF_HAND && appliedToHand != null) {
            undo(ps, appliedToHand);
            appliedToHand = null;
        }
        float[] t = handTransform(mc.player.getId(), hand == InteractionHand.MAIN_HAND ? Part.ARM_R : Part.ARM_L);
        if (t != null) apply(ps, t);
        if (hand == InteractionHand.MAIN_HAND) appliedToHand = t;
    }

    private static float[] handTransform(int entity, Part arm) {
        if (!sample(entity, HAND) || !HAND.has(arm)) return null;
        float w = HAND.weight * 0.5f;
        float[] t = {HAND.x(arm) * w, HAND.y(arm) * w, HAND.z(arm) * w,
                HAND.ox(arm) * 0.03f * w, HAND.oy(arm) * 0.03f * w, HAND.oz(arm) * 0.03f * w};
        for (float v : t) if (Math.abs(v) > 0.0001f) return t;
        return null;
    }

    private static void apply(PoseStack ps, float[] t) {
        ps.translate(-t[3], -t[4], t[5]);
        ps.mulPose(Axis.XP.rotationDegrees(t[0]));
        ps.mulPose(Axis.YP.rotationDegrees(t[1]));
        ps.mulPose(Axis.ZP.rotationDegrees(t[2]));
    }

    private static void undo(PoseStack ps, float[] t) {
        ps.mulPose(Axis.ZP.rotationDegrees(-t[2]));
        ps.mulPose(Axis.YP.rotationDegrees(-t[1]));
        ps.mulPose(Axis.XP.rotationDegrees(-t[0]));
        ps.translate(t[3], t[4], -t[5]);
    }
}
