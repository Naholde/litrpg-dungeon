package com.andrey.litrpg.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Кнопка в стиле «Системы». Подсвечивается при наведении; {@code selected} — для вкладок. */
public class SysButton extends AbstractButton {
    private final Runnable onPress;
    public boolean selected;
    public int accent = UiKit.FRAME;

    public SysButton(int x, int y, int w, int h, String text, Runnable onPress) {
        super(x, y, w, h, Component.literal(text));
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hot = active && (isHovered() || selected);
        g.fill(getX(), getY(), getX() + width, getY() + height,
                selected ? UiKit.GLASS_HOVER : hot ? UiKit.GLASS_HOVER : UiKit.GLASS_LIGHT);
        int frame = !active ? UiKit.FRAME_DIM : hot ? UiKit.FRAME_BRIGHT : accent;
        UiKit.border(g, getX(), getY(), width, height, frame);
        if (selected) g.fill(getX() + 1, getY() + height - 2, getX() + width - 1, getY() + height - 1, UiKit.FRAME_BRIGHT);
        var font = Minecraft.getInstance().font;
        int color = !active ? UiKit.MUTED : hot ? 0xFFFFFFFF : UiKit.TEXT;
        g.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2,
                getY() + (height - 8) / 2, color, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        defaultButtonNarrationText(out);
    }
}
