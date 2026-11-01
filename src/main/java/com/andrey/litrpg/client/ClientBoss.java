package com.andrey.litrpg.client;

import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.net.BossSyncPayload;
import com.andrey.litrpg.player.Derived;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Рамка хранителя вверху экрана: имя, здоровье, щит, неистовство, полоса подготовки навыка
 * («можно сбить» — голубым), таблица угрозы (для танка — видно, кто близок к срыву агро).
 */
public final class ClientBoss {
    private static BossSyncPayload last;
    private static long receivedAt;
    private static long clientAtReceive;

    private ClientBoss() {
    }

    public static void update(BossSyncPayload p) {
        if (p.name().isEmpty()) {
            if (last != null && last.entityId() == p.entityId()) last = null;
            return;
        }
        last = p;
        receivedAt = p.serverTime();
        var mc = Minecraft.getInstance();
        clientAtReceive = mc.level != null ? mc.level.getGameTime() : 0;
    }

    public static boolean active() {
        var mc = Minecraft.getInstance();
        return last != null && mc.level != null && mc.level.getGameTime() - clientAtReceive < 60;
    }

    public static void reset() {
        last = null;
    }

    public static int render(GuiGraphics g, Font f, int w, float partial) {
        if (!active()) return 0;
        BossSyncPayload b = last;
        var mc = Minecraft.getInstance();
        int fw = 220, x = (w - fw) / 2, y = 4, fh = 34;
        int frame = b.shielded() ? 0xFF7FD8FF : 0xFFE0564F;
        UiKit.panel(g, x, y, fw, fh, frame);
        String title = b.name() + (b.enrage() > 0 ? "  · неистовство " + b.enrage() : "");
        UiKit.textCenter(g, f, title, w / 2, y + 4, b.enrage() > 0 ? 0xFFFF7060 : 0xFFFFD0C8);
        double ratio = b.maxHealth() > 0 ? b.health() / b.maxHealth() : 0;
        int bx = x + 6, by = y + 15, bw = fw - 12;
        g.fill(bx, by, bx + bw, by + 8, 0xC0000000);
        int filled = (int) Math.round(ratio * (bw - 2));
        if (filled > 0) g.fillGradient(bx + 1, by + 1, bx + 1 + filled, by + 7, 0xFFFF6B5E, 0xFFB71C1C);
        if (b.shielded()) g.fill(bx + 1, by + 1, bx + bw - 1, by + 3, 0xC07FD8FF);
        String hp = Derived.MAX_HEALTH.format(Math.round(b.health())) + " / " + Derived.MAX_HEALTH.format(b.maxHealth())
                + "  (" + Math.round(ratio * 100) + "%)" + (b.shielded() ? "  ЩИТ" : "");
        var pose = g.pose();
        pose.pushPose();
        pose.translate(w / 2f, by + 1, 0);
        pose.scale(0.75f, 0.75f, 1);
        g.drawString(f, hp, -f.width(hp) / 2, 0, 0xFFFFFFFF, true);
        pose.popPose();
        // Подготовка навыка: «Имя|можно_сбить|длина|тик_конца».
        if (!b.cast().isEmpty()) {
            String[] parts = b.cast().split("\\|");
            if (parts.length == 4) {
                boolean interrupt = parts[1].equals("1");
                int total = Integer.parseInt(parts[2]);
                long end = Long.parseLong(parts[3]);
                long nowServer = receivedAt + (mc.level.getGameTime() - clientAtReceive);
                double left = Math.max(0, end - nowServer - partial);
                double prog = total > 0 ? 1 - left / total : 1;
                int cy = by + 11;
                g.fill(bx, cy, bx + bw, cy + 7, 0xC0000000);
                int c = interrupt ? 0xFF7FD8FF : 0xFFFFC857;
                g.fill(bx + 1, cy + 1, bx + 1 + (int) Math.round((bw - 2) * prog), cy + 6, c);
                String ct = parts[0] + (interrupt ? "  — сбить!" : "") + "  " + Derived.trim(Math.ceil(left / 2.0) / 10) + " с";
                pose.pushPose();
                pose.translate(w / 2f, cy, 0);
                pose.scale(0.75f, 0.75f, 1);
                g.drawString(f, ct, -f.width(ct) / 2, 0, 0xFF101010, false);
                pose.popPose();
            }
        }
        // Угроза: справа от рамки.
        if (!b.threat().isEmpty()) {
            int tx = x + fw + 4, ty = y;
            UiKit.text(g, f, "Угроза", tx, ty, UiKit.MUTED);
            for (int i = 0; i < b.threat().size(); i++) {
                var r = b.threat().get(i);
                int ry = ty + 10 + i * 9;
                boolean me = mc.player != null && r.name().equals(mc.player.getGameProfile().getName());
                g.fill(tx, ry, tx + (int) (60 * r.share()), ry + 8, i == 0 ? 0x80E0564F : 0x603FB6FF);
                pose.pushPose();
                pose.translate(tx + 2, ry + 1, 0);
                pose.scale(0.75f, 0.75f, 1);
                g.drawString(f, r.name() + " " + Math.round(r.share() * 100) + "%", 0, 0, me ? 0xFFFFE27A : 0xFFFFFFFF, true);
                pose.popPose();
            }
        }
        return y + fh + 2;
    }
}
