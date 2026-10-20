package com.example.placebreak.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

public class PlaceBreakClient implements ClientModInitializer {
    public static KeyBinding toggleKey;
    public static KeyBinding guiKey;
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("placebreak", "main"));

    private enum Phase { PLACE, BREAK }

    private static boolean enabled = false;
    private static Phase phase = Phase.PLACE;
    private static int wait = 0;
    private static int breakTicks = 0;
    private static boolean breakStarted = false;
    private static int cycles = 0;
    private static int blockSlot = -1;
    private static BlockPos placedPos = null;
    private static Direction placedSide = Direction.UP;

    @Override
    public void onInitializeClient() {
        PlaceBreakConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.placebreak.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.placebreak.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(PlaceBreakClient::tick);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(MinecraftClient mc, boolean on) {
        if (on == enabled) return;
        enabled = on;
        if (on) {
            phase = Phase.PLACE;
            wait = 0;
            cycles = 0;
            breakStarted = false;
            breakTicks = 0;
            placedPos = null;
            blockSlot = -1;
        } else if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        message(mc, on ? "Place & Break: ON" : "Place & Break: OFF");
    }

    private static void message(MinecraftClient mc, String text) {
        if (PlaceBreakConfig.showMessages && mc.player != null) {
            mc.player.sendMessage(Text.literal(text), true);
        }
    }

    private static void tick(MinecraftClient mc) {
        while (toggleKey.wasPressed()) {
            if (mc.currentScreen == null) setEnabled(mc, !enabled);
        }
        while (guiKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new PlaceBreakScreen(null));
        }

        if (!enabled) return;

        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;
        ClientPlayerInteractionManager im = mc.interactionManager;
        if (player == null || world == null || im == null) {
            enabled = false;
            return;
        }
        if (mc.currentScreen != null) return;

        if (wait > 0) {
            wait--;
            return;
        }

        switch (phase) {
            case PLACE -> doPlace(mc, player, im);
            case BREAK -> doBreak(mc, player, world, im);
        }
    }

    private static void doPlace(MinecraftClient mc, ClientPlayerEntity player, ClientPlayerInteractionManager im) {
        int selected = player.getInventory().getSelectedSlot();
        if (player.getMainHandStack().getItem() instanceof BlockItem) {
            blockSlot = selected;
        } else if (PlaceBreakConfig.usePickaxe && blockSlot >= 0 && blockSlot != selected) {
            // We're still holding the pickaxe from the last break: go back to the block.
            player.getInventory().setSelectedSlot(blockSlot);
            wait = 1;
            return;
        }
        if (!(player.getMainHandStack().getItem() instanceof BlockItem)) {
            message(mc, "Hold a block in your main hand");
            setEnabled(mc, false);
            return;
        }
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK
                || !(mc.crosshairTarget instanceof BlockHitResult hit)) {
            return; // not aiming at a block, keep waiting
        }

        BlockPos target = hit.getBlockPos().offset(hit.getSide());
        ActionResult result = im.interactBlock(player, Hand.MAIN_HAND, hit);
        if (result.isAccepted()) {
            if (PlaceBreakConfig.swingHand) player.swingHand(Hand.MAIN_HAND);
            placedPos = target;
            placedSide = hit.getSide();
            phase = Phase.BREAK;
            wait = PlaceBreakConfig.breakDelay;
            breakStarted = false;
            breakTicks = 0;
        }
    }

    private static void doBreak(MinecraftClient mc, ClientPlayerEntity player, ClientWorld world,
                                ClientPlayerInteractionManager im) {
        BlockState state = world.getBlockState(placedPos);

        if (state.isAir()) {
            // Block is gone (broken, or the placement never went through). Count it and go again.
            if (breakStarted) {
                cycles++;
                int max = PlaceBreakConfig.maxCycles;
                if (max > 0 && cycles >= max) {
                    message(mc, "Place & Break: finished " + cycles + " cycles");
                    setEnabled(mc, false);
                    return;
                }
            }
            phase = Phase.PLACE;
            wait = PlaceBreakConfig.placeDelay;
            breakStarted = false;
            return;
        }

        if (PlaceBreakConfig.usePickaxe && !player.getMainHandStack().isIn(ItemTags.PICKAXES)) {
            int slot = findPickaxeSlot(player);
            if (slot < 0) {
                message(mc, "No pickaxe in your hotbar");
                setEnabled(mc, false);
                return;
            }
            player.getInventory().setSelectedSlot(slot);
            wait = 1; // give the server a tick to see the slot change
            return;
        }

        if (!breakStarted) {
            im.attackBlock(placedPos, placedSide);
            breakStarted = true;
        } else {
            im.updateBlockBreakingProgress(placedPos, placedSide);
        }
        if (PlaceBreakConfig.swingHand) player.swingHand(Hand.MAIN_HAND);

        // Safety valve: give up on a block that won't break (e.g. wrong tool, adventure mode).
        if (++breakTicks > 200) {
            im.cancelBlockBreaking();
            message(mc, "Couldn't break the block, stopping");
            setEnabled(mc, false);
        }
    }

    private static int findPickaxeSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isIn(ItemTags.PICKAXES)) return i;
        }
        return -1;
    }
}
