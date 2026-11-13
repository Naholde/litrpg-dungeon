package com.andrey.litrpg.client.screen;

import com.andrey.litrpg.client.ClientLobby;
import com.andrey.litrpg.client.ui.SysButton;
import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.net.LobbyActionPayload;
import com.andrey.litrpg.net.LobbySyncPayload;
import com.andrey.litrpg.run.Difficulty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Окно «Сбор отряда» (обновление 15; ПКМ по Печати или клавиша P). Слева — сложность (две карточки с цифрами),
 * справа — кто в лобби и готов ли; внизу — «Готов» и лучшие забеги. Пока забег идёт — отряд, этап, время, бой,
 * «Наблюдать» / «В лобби» и голос «Покинуть забег» для участников.
 */
public final class LobbyScreen extends Screen {
    private static final int W = 340, H = 232;
    private int left, top;
    private SysButton ready, normal, heroic, watch, back, abandon;

    public LobbyScreen() {
        super(Component.literal("Сбор отряда"));
    }

    private static void act(LobbyActionPayload.Action a, String arg) {
        PacketDistributor.sendToServer(new LobbyActionPayload(a, arg));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int cw = 150;
        normal = addRenderableWidget(new SysButton(left + 12, top + 100, cw, 14, "Выбрать",
                () -> act(LobbyActionPayload.Action.DIFFICULTY, Difficulty.NORMAL.id)));
        normal.accent = Difficulty.NORMAL.color;
        heroic = addRenderableWidget(new SysButton(left + 12, top + 186, cw, 14, "Выбрать",
                () -> act(LobbyActionPayload.Action.DIFFICULTY, Difficulty.HEROIC.id)));
        heroic.accent = Difficulty.HEROIC.color;
        ready = addRenderableWidget(new SysButton(left + 178, top + H - 46, 150, 18, "Готов",
                () -> act(LobbyActionPayload.Action.READY, "")));
        ready.accent = UiKit.GOOD;
        watch = addRenderableWidget(new SysButton(left + 178, top + H - 46, 72, 18, "Наблюдать",
                () -> {
                    act(LobbyActionPayload.Action.SPECTATE, "");
                    onClose();
                }));
        back = addRenderableWidget(new SysButton(left + 256, top + H - 46, 72, 18, "В лобби",
                () -> {
                    act(LobbyActionPayload.Action.RETURN, "");
                    onClose();
                }));
        abandon = addRenderableWidget(new SysButton(left + 178, top + H - 46, 150, 18, "Покинуть забег",
                () -> act(LobbyActionPayload.Action.ABANDON, "")));
        abandon.accent = UiKit.BAD;
        refresh();
    }

    private boolean member() {
        var mc = Minecraft.getInstance();
        return mc.player != null && ClientLobby.data().run().party().contains(mc.player.getUUID());
    }

    private void refresh() {
        LobbySyncPayload d = ClientLobby.data();
        boolean run = d.run().active();
        boolean here = ClientLobby.inLobby();
        normal.visible = heroic.visible = !run;
        normal.active = heroic.active = here;
        normal.selected = d.difficulty().equals(Difficulty.NORMAL.id);
        heroic.selected = d.difficulty().equals(Difficulty.HEROIC.id);
        ready.visible = !run;
        ready.active = here;
        ready.setMessage(Component.literal(ClientLobby.meReady() ? "Не готов" : "Готов"));
        ready.selected = ClientLobby.meReady();
        boolean spectating = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isSpectator();
        watch.visible = run && !member() && !spectating;
        back.visible = run && !member() && spectating;
        abandon.visible = run && member();
        abandon.setMessage(Component.literal("Покинуть забег (" + d.run().votes() + "/" + d.run().votesNeed() + ")"));
    }

    @Override
    public void tick() {
        refresh();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        LobbySyncPayload d = ClientLobby.data();
        UiKit.panel(g, left, top, W, H);
        UiKit.textCenter(g, font, "Система: сбор отряда", left + W / 2, top + 8, UiKit.FRAME_BRIGHT);
        if (d.run().active()) renderRun(g, d);
        else renderDifficulty(g, d);
        renderPlayers(g, d);
        int y = top + H - 22;
        for (String r : d.records()) {
            UiKit.text(g, font, font.plainSubstrByWidth("★ " + r, W - 24), left + 12, y, UiKit.MUTED);
            y += 9;
        }
        super.render(g, mx, my, pt);
    }

    private void renderDifficulty(GuiGraphics g, LobbySyncPayload d) {
        var bal = Balance.get(Balance.RUN).difficulty();
        card(g, Difficulty.NORMAL, top + 24, bal.normal(), d.difficulty().equals(Difficulty.NORMAL.id));
        card(g, Difficulty.HEROIC, top + 110, bal.heroic(), d.difficulty().equals(Difficulty.HEROIC.id));
    }

    private void card(GuiGraphics g, Difficulty diff, int y, com.andrey.litrpg.core.balance.RunBalance.Mode m, boolean picked) {
        int x = left + 8, w = 158, h = 80;
        g.fill(x, y, x + w, y + h, picked ? 0xE0182B45 : 0xC00D1828);
        UiKit.border(g, x, y, w, h, picked ? diff.color : (diff.color & 0x00FFFFFF) | 0x70000000);
        UiKit.text(g, font, (picked ? "▶ " : "") + diff.title, x + 6, y + 5, diff.color);
        int ly = y + 17;
        for (FormattedCharSequence line : font.split(Component.literal(diff.tagline), w - 12)) {
            g.drawString(font, line, x + 6, ly, UiKit.TEXT, true);
            ly += 9;
        }
        ly += 2;
        UiKit.text(g, font, "Здоровье врагов: " + Math.round(m.hp() * 100) + "%", x + 6, ly, UiKit.MUTED);
        UiKit.text(g, font, "Урон врагов: " + Math.round(m.damage() * 100) + "%", x + 6, ly + 9, UiKit.MUTED);
        if (m.lootFind() > 0) UiKit.text(g, font, "Добыча: +" + Math.round(m.lootFind() * 100) + "% находки", x + 6, ly + 18, UiKit.GOLD);
    }

    private void renderRun(GuiGraphics g, LobbySyncPayload d) {
        var r = d.run();
        int x = left + 12, y = top + 26;
        Difficulty diff = Difficulty.byId(r.difficulty());
        UiKit.heading(g, font, "Идёт забег", x, y, 154);
        y += 14;
        UiKit.text(g, font, "Сложность: " + diff.title, x, y, diff.color);
        y += 11;
        UiKit.text(g, font, "Этап " + r.stage() + " · " + r.stageName(), x, y, UiKit.GOLD);
        y += 11;
        UiKit.text(g, font, "Время: " + com.andrey.litrpg.run.Chronicle.time(r.seconds() * 20), x, y, UiKit.TEXT);
        y += 11;
        UiKit.text(g, font, "Вайпов: " + r.wipes(), x, y, UiKit.TEXT);
        y += 11;
        if (!r.fighting().isEmpty()) {
            UiKit.text(g, font, "Бой: " + r.fighting(), x, y, UiKit.BAD);
            y += 11;
        }
        y += 6;
        String hint = member()
                ? "Если отряд решит уйти — нужны голоса всех в сети. Забег запишется в летопись как брошенный."
                : "Можно наблюдать за отрядом — или подождать в лобби: паркур, полигон, Зал славы.";
        for (FormattedCharSequence line : font.split(Component.literal(hint), 154)) {
            g.drawString(font, line, x, y, UiKit.MUTED, true);
            y += 9;
        }
    }

    private void renderPlayers(GuiGraphics g, LobbySyncPayload d) {
        int x = left + 178, y = top + 26, w = 150;
        boolean run = d.run().active();
        UiKit.heading(g, font, run ? "Отряд" : "В лобби (" + d.players().size() + ", нужно " + d.minPlayers() + "–" + d.maxPlayers() + ")",
                x, y, w);
        y += 14;
        for (LobbySyncPayload.Member m : d.players()) {
            String mark = run ? (m.inLobby() ? "●" : "○") : (m.ready() ? "✔" : "…");
            int col = run ? (m.inLobby() ? UiKit.TEXT : UiKit.MUTED) : (m.ready() ? UiKit.GOOD : UiKit.MUTED);
            UiKit.text(g, font, mark + " " + m.name() + (run && m.ready() ? "  (за выход)" : ""), x, y, col);
            y += 11;
        }
        if (!run) {
            y += 4;
            String status;
            if (d.countdownTicks() >= 0) status = "Все готовы — старт через " + (d.countdownTicks() + 19) / 20 + " с";
            else if (d.players().size() < d.minPlayers()) status = "Ждём ещё " + (d.minPlayers() - d.players().size()) + " игрока(ов).";
            else status = "Старт, когда готовы все в лобби.";
            for (FormattedCharSequence line : font.split(Component.literal(status), w)) {
                g.drawString(font, line, x, y, d.countdownTicks() >= 0 ? UiKit.GOOD : UiKit.GOLD, true);
                y += 9;
            }
            y += 3;
            for (FormattedCharSequence line : font.split(Component.literal(
                    "Забег — с 1 уровня. Роль выберете в подземелье, в Зале ролей."), w)) {
                g.drawString(font, line, x, y, UiKit.MUTED, true);
                y += 9;
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
