package com.andrey.litrpg.client;

import com.andrey.litrpg.client.fx.ClientAggro;
import com.andrey.litrpg.client.fx.ClientPopups;
import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.net.PartySyncPayload;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.Role;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Свой HUD: здоровье числом и полосой, блок уровня, рамки группы (Ctrl+1…6), рамка цели под прицелом,
 * предупреждение «на вас агро». Панель навыков добавится в фазе навыков.
 */
public final class HudOverlay {
    /** «Призрак» полосы здоровья: после удара светлая часть тает — видно, сколько сняли. */
    private static float ghost = 1f;
    private static final int HB_W = 182, HB_H = 10;

    private HudOverlay() {
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.options.hideGui || p.isSpectator()) return;
        Font f = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        if (!p.isCreative()) health(g, f, p, w, h);
        if (ClientPlayerData.received() && ClientPlayerData.role().chosen()) {
            SkillBar.render(g, f, w, h, delta.getGameTimeDeltaPartialTick(true));
        }
        levelBlock(g, f, mc);
        partyFrames(g, f, mc);
        int below = ClientBoss.render(g, f, w, delta.getGameTimeDeltaPartialTick(true));
        targetFrame(g, f, mc, w, delta.getGameTimeDeltaPartialTick(true), below);
    }

    // ------------------------------------------------------------------ здоровье

    private static void health(GuiGraphics g, Font f, Player p, int w, int h) {
        float max = p.getMaxHealth(), hp = Math.min(p.getHealth(), max), abs = p.getAbsorptionAmount();
        float ratio = max > 0 ? hp / max : 0;
        ghost = ratio > ghost ? ratio : Math.max(ratio, ghost - 0.01f);
        int x = (w - HB_W) / 2, y = h - 41;
        g.fill(x - 1, y - 1, x + HB_W + 1, y + HB_H + 1, 0xA0000000);
        g.fill(x, y, x + HB_W, y + HB_H, 0xFF1A0B0E);
        int inner = HB_W - 2;
        int gw = Math.round(ghost * inner), fw = Math.round(ratio * inner);
        if (gw > fw) g.fill(x + 1 + fw, y + 1, x + 1 + gw, y + HB_H - 1, 0xFFFFE0B0);
        boolean low = ratio < 0.3f;
        int top = low ? 0xFFFF8A80 : UiKit.HEALTH_TOP, bottom = low ? 0xFFB71C1C : UiKit.HEALTH;
        if (fw > 0) g.fillGradient(x + 1, y + 1, x + 1 + fw, y + HB_H - 1, top, bottom);
        if (abs > 0) {
            int aw = Math.min(inner, Math.round(abs / max * inner));
            g.fillGradient(x + 1, y + 1, x + 1 + aw, y + 3, 0xFFFFF176, 0xFFFBC02D);
        }
        UiKit.border(g, x, y, HB_W, HB_H, low && (p.tickCount / 8) % 2 == 0 ? 0xFFFF6B6B : 0xFF7A2A30);
        String text = Derived.MAX_HEALTH.format(Math.round(hp)) + " / " + Derived.MAX_HEALTH.format(Math.round(max))
                + (abs > 0 ? "  +" + Math.round(abs) : "");
        UiKit.textCenter(g, f, text, w / 2, y + 1, 0xFFFFFFFF);
        int aggro = ClientAggro.countTargeting(p.getId());
        if (aggro > 0) {
            String warn = "⚠ На вас: " + aggro;
            int c = (p.tickCount / 6) % 2 == 0 ? 0xFFFF5050 : 0xFFFF9A9A;
            UiKit.textRight(g, f, warn, (w + SkillBar.WIDTH) / 2, SkillBar.top(h) - 12, c);
        }
        ClientPopups.renderHud(g, f, x, y, HB_W);
    }

    // ------------------------------------------------------------------ уровень

    private static void levelBlock(GuiGraphics g, Font f, Minecraft mc) {
        if (!ClientPlayerData.received()) return;
        int x = 4, y = 4, bw = 124, bh = 31;
        UiKit.panel(g, x, y, bw, bh);
        Role role = ClientPlayerData.role();
        int lv = ClientPlayerData.level();
        UiKit.text(g, f, "Ур. " + lv, x + 5, y + 5, UiKit.GOLD);
        int lvw = f.width("Ур. " + lv);
        var cls = ClientPlayerData.playerClass();
        String who = cls != null ? cls.icon() + " " + cls.name() : role.icon + " " + role.title;
        UiKit.text(g, f, f.plainSubstrByWidth(who, bw - 18 - lvw), x + 10 + lvw, y + 5, cls != null ? cls.color() : role.color);
        long need = ClientPlayerData.xpToNext();
        double ratio = need > 0 ? (double) ClientPlayerData.xp() / need : 1;
        UiKit.bar(g, x + 5, y + 16, bw - 10, 4, ratio, 0xFFC6B5FF, UiKit.XP);
        int free = ClientPlayerData.freeStatPoints();
        String info = need > 0 ? Math.round(ratio * 100) + "% опыта" : "Макс. уровень";
        UiKit.text(g, f, info, x + 5, y + 21, UiKit.MUTED);
        if (free > 0) {
            int c = (mc.player.tickCount / 10) % 2 == 0 ? UiKit.GOLD : 0xFFFFE9A8;
            UiKit.textRight(g, f, "+" + free + " [K]", x + bw - 5, y + 21, c);
        }
    }

    // ------------------------------------------------------------------ рамки группы

    private static void partyFrames(GuiGraphics g, Font f, Minecraft mc) {
        var members = ClientParty.members();
        if (members.isEmpty()) return;
        int x = 4, y = 40, fw = 124, fh = members.size() > 5 ? 21 : 23;
        UiKit.text(g, f, "ГРУППА", x + 2, y, UiKit.FRAME_BRIGHT);
        UiKit.textRight(g, f, "Ctrl+1…" + members.size(), x + fw - 2, y, 0xFF5F7790);
        y += 11;
        for (int i = 0; i < members.size(); i++) {
            PartySyncPayload.Member m = members.get(i);
            boolean sel = m.id().equals(ClientParty.selected());
            boolean me = mc.player != null && m.id().equals(mc.player.getUUID());
            g.fill(x, y, x + fw, y + fh, sel ? 0xE0182B45 : UiKit.GLASS);
            UiKit.border(g, x, y, fw, fh, sel ? UiKit.GOLD : me ? UiKit.FRAME : UiKit.FRAME_DIM);
            UiKit.text(g, f, String.valueOf(i + 1), x + 3, y + 3, 0xFF5F7790);
            if (!m.online()) {
                UiKit.text(g, f, "не в сети", x + 12, y + 3, UiKit.MUTED);
                y += fh + 2;
                continue;
            }
            UiKit.text(g, f, m.role().icon, x + 12, y + 3, m.role().color);
            String name = f.plainSubstrByWidth(m.name(), 62);
            UiKit.text(g, f, name, x + 22, y + 3, me ? 0xFFFFFFFF : UiKit.TEXT);
            float ratio = m.maxHealth() > 0 ? m.health() / m.maxHealth() : 0;
            UiKit.textRight(g, f, Math.round(ratio * 100) + "%", x + fw - 4, y + 3,
                    ratio < 0.3f ? UiKit.BAD : UiKit.TEXT);
            if (ClientAggro.countTargeting(m.entityId()) > 0) {
                UiKit.textRight(g, f, "⚠", x + fw - 28, y + 3, 0xFFFF5050);
            }
            int bx = x + 12, by = y + fh - 9, bw = fw - 16;
            g.fill(bx, by, bx + bw, by + 5, 0xC0000000);
            int filled = Math.round(ratio * (bw - 2));
            int top = ratio < 0.3f ? 0xFFFF8A80 : 0xFF7BE38E, bot = ratio < 0.3f ? 0xFFB71C1C : 0xFF2E9E4A;
            if (filled > 0) g.fillGradient(bx + 1, by + 1, bx + 1 + filled, by + 4, top, bot);
            y += fh + 2;
        }
    }

    // ------------------------------------------------------------------ рамка цели

    private static void targetFrame(GuiGraphics g, Font f, Minecraft mc, int w, float partial, int below) {
        LivingEntity t = lookedAt(mc, 40, partial);
        // У хранителя своя рамка; торговец — не враг.
        if (t == null || t instanceof com.andrey.litrpg.boss.BossEntity || t instanceof com.andrey.litrpg.shop.TraderEntity) return;
        int fw = 170, fh = 30, x = (w - fw) / 2, y = Math.max(4, below);
        UiKit.panel(g, x, y, fw, fh, t instanceof Player ? UiKit.FRAME : 0xFFE0564F);
        String name = f.plainSubstrByWidth(t.getDisplayName().getString(), fw - 12);
        UiKit.textCenter(g, f, name, x + fw / 2, y + 4, t instanceof Player ? UiKit.TEXT : 0xFFFFB0A8);
        float max = t.getMaxHealth(), hp = t.getHealth();
        double ratio = max > 0 ? hp / max : 0;
        int bx = x + 6, by = y + 14, bw = fw - 12;
        g.fill(bx, by, bx + bw, by + 7, 0xC0000000);
        int filled = (int) Math.round(ratio * (bw - 2));
        if (filled > 0) g.fillGradient(bx + 1, by + 1, bx + 1 + filled, by + 6, UiKit.HEALTH_TOP, UiKit.HEALTH);
        String hpText = Derived.MAX_HEALTH.format(Math.round(hp)) + " / " + Derived.MAX_HEALTH.format(Math.round(max));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + fw / 2f, by + 1, 0);
        pose.scale(0.75f, 0.75f, 1);
        g.drawString(f, hpText, -f.width(hpText) / 2, 0, 0xFFFFFFFF, true);
        pose.popPose();
        int target = ClientAggro.targetOf(t.getId());
        if (target >= 0 && mc.level != null) {
            Entity te = mc.level.getEntity(target);
            if (te != null) {
                boolean me = te == mc.player;
                String s = me ? "⚠ Цель: вы" : "Цель: " + te.getName().getString();
                UiKit.textCenter(g, f, s, x + fw / 2, y + 22, me ? 0xFFFF6060 : 0xFFFFD36B);
            }
        }
    }

    /** Живая сущность под прицелом до {@code range} блоков (с учётом стен). */
    public static LivingEntity lookedAt(Minecraft mc, double range, float partial) {
        Player p = mc.player;
        if (p == null || mc.level == null) return null;
        Vec3 eye = p.getEyePosition(partial);
        Vec3 end = eye.add(p.getViewVector(partial).scale(range));
        HitResult block = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, p.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
                e -> e instanceof LivingEntity l && l.isAlive() && !e.isSpectator() && e != p, eye.distanceToSqr(end));
        return hit != null ? (LivingEntity) hit.getEntity() : null;
    }
}
