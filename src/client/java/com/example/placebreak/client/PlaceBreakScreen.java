package com.example.placebreak.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;

public class PlaceBreakScreen extends Screen {
    private final Screen parent;

    public PlaceBreakScreen(Screen parent) {
        super(Text.literal("Place & Break"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 200;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 95;
        int step = 24;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(PlaceBreakClient.isEnabled() ? "Status: RUNNING (click to stop)" : "Status: STOPPED (click to start)"),
                b -> {
                    PlaceBreakClient.setEnabled(this.client, !PlaceBreakClient.isEnabled());
                    this.clearAndInit();
                }).dimensions(x, y, w, 20).build());

        this.addDrawableChild(new IntSlider(x, y + step, w, "Break delay (ticks)", 0, 20,
                PlaceBreakConfig.breakDelay, v -> PlaceBreakConfig.breakDelay = v));
        this.addDrawableChild(new IntSlider(x, y + step * 2, w, "Place delay (ticks)", 0, 20,
                PlaceBreakConfig.placeDelay, v -> PlaceBreakConfig.placeDelay = v));
        this.addDrawableChild(new IntSlider(x, y + step * 3, w, "Max cycles (0 = endless)", 0, 1000,
                PlaceBreakConfig.maxCycles, v -> PlaceBreakConfig.maxCycles = v));

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Swing hand: " + (PlaceBreakConfig.swingHand ? "ON" : "OFF")),
                b -> {
                    PlaceBreakConfig.swingHand = !PlaceBreakConfig.swingHand;
                    this.clearAndInit();
                }).dimensions(x, y + step * 4, w / 2 - 2, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Messages: " + (PlaceBreakConfig.showMessages ? "ON" : "OFF")),
                b -> {
                    PlaceBreakConfig.showMessages = !PlaceBreakConfig.showMessages;
                    this.clearAndInit();
                }).dimensions(x + w / 2 + 2, y + step * 4, w / 2 - 2, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Break with pickaxe: " + (PlaceBreakConfig.usePickaxe ? "ON" : "OFF")),
                b -> {
                    PlaceBreakConfig.usePickaxe = !PlaceBreakConfig.usePickaxe;
                    this.clearAndInit();
                }).dimensions(x, y + step * 5, w, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
                .dimensions(x, y + step * 6 + 6, w, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 120, 0xFFFFFF);

        Text toggle = Text.literal("Toggle: ").append(PlaceBreakClient.toggleKey.getBoundKeyLocalizedText());
        Text gui = Text.literal("This menu: ").append(PlaceBreakClient.guiKey.getBoundKeyLocalizedText());
        int by = this.height / 2 + 69;
        context.drawCenteredTextWithShadow(this.textRenderer, toggle, this.width / 2, by + 16, 0xAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, gui, this.width / 2, by + 28, 0xAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Block + pickaxe in hotbar, aim at a surface"), this.width / 2, by + 44, 0x888888);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public void removed() {
        PlaceBreakConfig.save();
    }

    private static class IntSlider extends SliderWidget {
        private final String label;
        private final int min;
        private final int max;
        private final IntConsumer setter;
        private int current;

        IntSlider(int x, int y, int width, String label, int min, int max, int value, IntConsumer setter) {
            super(x, y, width, 20, Text.empty(), (double) (value - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.current = value;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + current));
        }

        @Override
        protected void applyValue() {
            current = min + (int) Math.round(this.value * (max - min));
            setter.accept(current);
        }
    }
}
