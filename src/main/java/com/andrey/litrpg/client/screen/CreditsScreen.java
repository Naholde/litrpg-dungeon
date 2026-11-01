package com.andrey.litrpg.client.screen;

import com.andrey.litrpg.client.music.MusicController;
import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.net.CreditsPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Титры после последнего хранителя: отряд, время, вайпы, кто сделал игру, чья музыка. Текст плывёт вверх. */
public final class CreditsScreen extends Screen {
    private final List<String[]> lines = new ArrayList<>();
    private float offset;

    public CreditsScreen(CreditsPayload p) {
        super(Component.literal("Титры"));
        line("ПОДЗЕМЕЛЬЕ ПРОЙДЕНО", UiKit.GOLD);
        line("", 0);
        line("Система: «Испытание окончено. Вы дошли туда, куда не дошёл никто до вас.»", UiKit.TEXT);
        line("", 0);
        line("Отряд", UiKit.GOLD);
        for (String n : p.party()) line(n, UiKit.TEXT);
        line("", 0);
        long s = p.seconds();
        line("Время: " + s / 3600 + " ч " + s / 60 % 60 + " мин · вайпов: " + p.wipes(), UiKit.MUTED);
        line("", 0);
        line("Идея и мир", UiKit.GOLD);
        line("Андрей", UiKit.TEXT);
        line("", 0);
        line("Код, карта, хранители", UiKit.GOLD);
        line("Claude", UiKit.TEXT);
        line("", 0);
        line("Музыка хранителей", UiKit.GOLD);
        for (String m : MUSIC) line(m, UiKit.TEXT);
        line("", 0);
        line("Спасибо, что играли.", UiKit.GOLD);
    }

    private static final String[] MUSIC = {
            "SLXUGHTER — fragment", "The Dust Brothers — Fight Club", "Dj Univxrsel — Kurayami No Naka",
            "blud (Ultra Slowed)", "Qura — Jane Hoodtrap", "Hensonn — Fear", "Bountyhunter — Woops", "Vessel — Red",
            "Eru Flow Flame — Aura of Glory", "Schwarzer Engel — Ring frei", "Brian Tyler — Formula 1 Theme",
            "NUEKI, TOLCHONOV — BLUE LOCK", "ugolki soznaniya — Your Next Opponent Is You",
            "Hans Zimmer — No Time for Caution", "wutiwant", "Tys Music — As Time Flies", "Ennio Morricone — The Ecstasy of Gold"};

    private void line(String text, int color) {
        lines.add(new String[]{text, Integer.toString(color)});
    }

    @Override
    protected void init() {
        MusicController.force("ecstasy");
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xFF050508);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        offset += pt * 0.6f;
        float y = height - offset;
        for (String[] l : lines) {
            if (!l[0].isEmpty() && y > -12 && y < height) {
                int c = Integer.parseInt(l[1]);
                for (var part : font.split(Component.literal(l[0]), width - 40)) {
                    g.drawCenteredString(font, part, width / 2, (int) y, c);
                    y += 11;
                }
            } else {
                y += 11;
            }
            y += 3;
        }
        if (y < 0) offset = -height * 0.2f; // по кругу, пока не закроют
        UiKit.textCenter(g, font, "Esc — закрыть", width / 2, height - 12, UiKit.MUTED);
    }

    @Override
    public void onClose() {
        MusicController.force("");
        super.onClose();
    }
}
