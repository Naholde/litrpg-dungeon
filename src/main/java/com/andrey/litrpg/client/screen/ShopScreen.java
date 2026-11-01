package com.andrey.litrpg.client.screen;

import com.andrey.litrpg.client.ClientPlayerData;
import com.andrey.litrpg.client.ui.SysButton;
import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.gear.Wallet;
import com.andrey.litrpg.net.ShopPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Лавка торговца привала: товар под вашу роль, наковальня (вещь в руке), услуги класса. */
public final class ShopScreen extends Screen {
    private static final int W = 310, H = 226, ROW = 21;
    private enum Tab { SHOP, ANVIL, CLASS }

    private static Tab lastTab = Tab.SHOP;
    private ShopPayload data;
    private Tab tab = lastTab;
    private int left, top;
    private final List<SysButton> tabButtons = new ArrayList<>();
    private final List<SysButton> buy = new ArrayList<>();
    private SysButton reroll, upgrade, classChange, rankReset;

    private ShopScreen(ShopPayload p) {
        super(Component.literal("Лавка"));
        this.data = p;
    }

    /** Открыть или обновить уже открытую лавку (после покупки сервер присылает новое состояние). */
    public static void open(ShopPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ShopScreen s && s.data.traderId() == p.traderId()) {
            s.data = p;
            s.rebuildWidgets();
        } else {
            mc.setScreen(new ShopScreen(p));
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        tabButtons.clear();
        buy.clear();
        String[] names = {"Лавка", "Наковальня", "Класс"};
        for (int i = 0; i < names.length; i++) {
            Tab t = Tab.values()[i];
            tabButtons.add(addRenderableWidget(new SysButton(left + 8 + i * 74, top + 6, 70, 16, names[i], () -> {
                tab = t;
                lastTab = t;
                refresh();
            })));
        }
        for (int i = 0; i < data.entries().size(); i++) {
            final int idx = i;
            SysButton b = addRenderableWidget(new SysButton(left + W - 66, rowY(i) + 2, 56, 14, "Купить",
                    () -> act(ShopPayload.Action.BUY, idx)));
            b.accent = UiKit.GOLD;
            buy.add(b);
        }
        reroll = addRenderableWidget(new SysButton(left + 20, top + 120, 130, 16, "Перековать", () -> act(ShopPayload.Action.REROLL, 0)));
        upgrade = addRenderableWidget(new SysButton(left + 160, top + 120, 130, 16, "Поднять уровень", () -> act(ShopPayload.Action.UPGRADE, 0)));
        classChange = addRenderableWidget(new SysButton(left + 20, top + 110, 130, 16, "Сменить класс", () -> act(ShopPayload.Action.CLASS_CHANGE, 0)));
        rankReset = addRenderableWidget(new SysButton(left + 160, top + 110, 130, 16, "Забыть ранги", () -> act(ShopPayload.Action.RANK_RESET, 0)));
        refresh();
    }

    private int rowY(int i) {
        return top + 30 + i * ROW;
    }

    private void act(ShopPayload.Action a, int index) {
        PacketDistributor.sendToServer(new ShopPayload.Act(data.traderId(), a, index));
    }

    private void refresh() {
        for (int i = 0; i < tabButtons.size(); i++) tabButtons.get(i).selected = tab.ordinal() == i;
        for (int i = 0; i < buy.size(); i++) {
            var e = data.entries().get(i);
            buy.get(i).visible = tab == Tab.SHOP;
            buy.get(i).active = !e.bought() && data.wallet() >= e.price();
        }
        reroll.visible = upgrade.visible = tab == Tab.ANVIL;
        reroll.active = data.reroll() >= 0 && data.wallet() >= data.reroll();
        upgrade.active = data.upgrade() >= 0 && data.wallet() >= data.upgrade();
        classChange.visible = rankReset.visible = tab == Tab.CLASS;
        classChange.active = !ClientPlayerData.classId().isEmpty() && data.wallet() >= data.classChange();
        rankReset.active = data.wallet() >= data.rankReset();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        UiKit.panel(g, left, top, W, H, UiKit.GOLD);
        UiKit.textRight(g, font, "Кошелёк: " + Wallet.format(data.wallet()), left + W - 10, top + H - 13, UiKit.GOLD);
        super.render(g, mx, my, pt);
        switch (tab) {
            case SHOP -> renderShop(g, mx, my);
            case ANVIL -> renderAnvil(g);
            case CLASS -> renderClass(g);
        }
    }

    private void renderShop(GuiGraphics g, int mx, int my) {
        ItemStack hovered = null;
        for (int i = 0; i < data.entries().size(); i++) {
            var e = data.entries().get(i);
            int y = rowY(i), x = left + 10;
            boolean hover = UiKit.inside(mx, my, x, y, W - 80, ROW - 2);
            g.fill(x, y, left + W - 10, y + ROW - 2, e.bought() ? 0x40000000 : hover ? 0x303FB6FF : 0x50000000);
            g.renderItem(e.stack(), x + 2, y + 1);
            Component name = e.stack().getHoverName();
            g.drawString(font, font.plainSubstrByWidth(name.getString(), 160), x + 22, y + 2,
                    e.bought() ? 0xFF5F6F80 : name.getStyle().getColor() != null ? 0xFF000000 | name.getStyle().getColor().getValue() : UiKit.TEXT, true);
            UiKit.text(g, font, e.bought() ? "куплено" : Wallet.format(e.price()), x + 22, y + 10,
                    e.bought() ? 0xFF5F6F80 : data.wallet() >= e.price() ? UiKit.GOLD : UiKit.BAD);
            if (hover) hovered = e.stack();
        }
        var pose = g.pose();
        pose.pushPose();
        pose.translate(left + 10, top + H - 12, 0);
        pose.scale(0.75f, 0.75f, 1);
        g.drawString(font, "Shift+ПКМ по торговцу — продать вещь", 0, 0, 0xFF5F7790, true);
        pose.popPose();
        if (hovered != null) g.renderTooltip(font, hovered, mx, my);
    }

    private void renderAnvil(GuiGraphics g) {
        int x = left + 20, y = top + 34;
        ItemStack held = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getMainHandItem() : ItemStack.EMPTY;
        UiKit.heading(g, font, "Вещь в руке", x, y, W - 40);
        if (held.isEmpty()) {
            UiKit.text(g, font, "Возьмите вещь Системы в руку.", x, y + 18, UiKit.MUTED);
        } else {
            g.renderItem(held, x, y + 15);
            g.drawString(font, held.getHoverName(), x + 20, y + 19, UiKit.TEXT, true);
        }
        lines(g, "Перековка: все характеристики и свойства выпадают заново (каждая следующая дороже).", x, y + 40);
        lines(g, "Подъём уровня: до вашего уровня, шагом 5 — урон, броня и характеристики растут.", x, y + 62);
        UiKit.text(g, font, data.reroll() >= 0 ? Wallet.format(data.reroll()) : "—", left + 20, top + 140, UiKit.GOLD);
        UiKit.text(g, font, data.upgrade() >= 0 ? Wallet.format(data.upgrade()) : "уровень уже ваш", left + 160, top + 140, UiKit.GOLD);
    }

    private void renderClass(GuiGraphics g) {
        int x = left + 20, y = top + 34;
        var c = ClientPlayerData.playerClass();
        UiKit.heading(g, font, "Класс: " + (c != null ? c.name() : "ещё не выбран"), x, y, W - 40);
        lines(g, "Смена класса: Система предложит классы заново; уровень и вещи остаются, ранги сбрасываются.", x, y + 16);
        lines(g, "Забыть ранги: все очки класса возвращаются — можно вложить по-другому.", x, y + 44);
        UiKit.text(g, font, Wallet.format(data.classChange()), left + 20, top + 130, UiKit.GOLD);
        UiKit.text(g, font, Wallet.format(data.rankReset()), left + 160, top + 130, UiKit.GOLD);
    }

    private void lines(GuiGraphics g, String text, int x, int y) {
        for (FormattedCharSequence l : font.split(Component.literal(text), W - 40)) {
            g.drawString(font, l, x, y, UiKit.MUTED, true);
            y += 10;
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
