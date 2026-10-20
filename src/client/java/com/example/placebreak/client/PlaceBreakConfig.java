package com.example.placebreak.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PlaceBreakConfig {
    /** Ticks to wait after a block is broken before placing the next one (20 ticks = 1 second). */
    public static int placeDelay = 2;
    /** Ticks to wait after a block is placed before breaking it. */
    public static int breakDelay = 2;
    /** Stop automatically after this many place+break cycles. 0 = never stop. */
    public static int maxCycles = 0;
    /** Switch to a hotbar pickaxe to break the placed block, then switch back to the block. */
    public static boolean usePickaxe = true;
    public static boolean swingHand = true;
    public static boolean showMessages = true;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("placebreak.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(f)) {
            p.load(r);
            placeDelay = clamp(parse(p, "placeDelay", placeDelay), 0, 20);
            breakDelay = clamp(parse(p, "breakDelay", breakDelay), 0, 20);
            maxCycles = clamp(parse(p, "maxCycles", maxCycles), 0, 1000);
            usePickaxe = Boolean.parseBoolean(p.getProperty("usePickaxe", String.valueOf(usePickaxe)));
            swingHand = Boolean.parseBoolean(p.getProperty("swingHand", String.valueOf(swingHand)));
            showMessages = Boolean.parseBoolean(p.getProperty("showMessages", String.valueOf(showMessages)));
        } catch (IOException ignored) {
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("placeDelay", String.valueOf(placeDelay));
        p.setProperty("breakDelay", String.valueOf(breakDelay));
        p.setProperty("maxCycles", String.valueOf(maxCycles));
        p.setProperty("usePickaxe", String.valueOf(usePickaxe));
        p.setProperty("swingHand", String.valueOf(swingHand));
        p.setProperty("showMessages", String.valueOf(showMessages));
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "Place & Break settings");
        } catch (IOException ignored) {
        }
    }

    private static int parse(Properties p, String key, int def) {
        try {
            return Integer.parseInt(p.getProperty(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private PlaceBreakConfig() {
    }
}
