package net.rizen.lazy_chunks.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.rizen.lazy_chunks.LazyChunksMod;

import java.nio.file.Files;
import java.nio.file.Path;

public class LazyChunksConfig {
    private static LazyChunksConfig INSTANCE;
    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("neo_lazy_chunks.toml");

    public boolean showF3Overlay = true;
    public boolean lazyChunkLoadingEnabled = true;
    public int targetFps = 60;
    public int fpsThreshold = 80;
    public double baseWeightPerFrame = 3.0;
    public double maxFrameTimePercent = 12.0;
    public boolean proactiveThrottling = true;
    public boolean teleportProtection = true;
    public int teleportProtectionDuration = 120;
    public double weightChunkWithLight = 1.0;
    public double weightLightUpdate = 0.2;
    public double weightForgetChunk = 2.6;
    public int sampleInterval = 5;
    public int teleportCheckInterval = 3;
    public boolean tpsThrottleEnabled = false;
    public double tpsThreshold = 15.0;
    public int tpsCheckInterval = 20;

    public static LazyChunksConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static LazyChunksConfig load() {
        LazyChunksConfig config = new LazyChunksConfig();

        if (Files.exists(CONFIG_PATH)) {
            try (CommentedFileConfig cfg = CommentedFileConfig
                    .builder(CONFIG_PATH)
                    .autosave()
                    .preserveInsertionOrder()
                    .build()) {
                cfg.load();
                config.showF3Overlay =
                        cfg.getOrElse("showF3Overlay", true);
                config.lazyChunkLoadingEnabled =
                        cfg.getOrElse("lazyChunkLoadingEnabled", true);
                config.targetFps =
                        cfg.getIntOrElse("targetFps", 60);
                config.fpsThreshold =
                        cfg.getIntOrElse("fpsThreshold", 80);
                config.baseWeightPerFrame =
                        cfg.<Double>getOrElse("baseWeightPerFrame", 3.0);
                config.maxFrameTimePercent =
                        cfg.<Double>getOrElse("maxFrameTimePercent", 12.0);
                config.proactiveThrottling =
                        cfg.getOrElse("proactiveThrottling", true);
                config.teleportProtection =
                        cfg.getOrElse("teleportProtection", true);
                config.teleportProtectionDuration =
                        cfg.getIntOrElse("teleportProtectionDuration", 120);
                config.weightChunkWithLight =
                        cfg.<Double>getOrElse("weightChunkWithLight", 1.0);
                config.weightLightUpdate =
                        cfg.<Double>getOrElse("weightLightUpdate", 0.2);
                config.weightForgetChunk =
                        cfg.<Double>getOrElse("weightForgetChunk", 2.6);
                config.sampleInterval =
                        cfg.getIntOrElse("sampleInterval", 5);
                config.teleportCheckInterval =
                        cfg.getIntOrElse("teleportCheckInterval", 3);
                config.tpsThrottleEnabled =
                        cfg.getOrElse("tpsThrottleEnabled", false);
                config.tpsThreshold =
                        cfg.<Double>getOrElse("tpsThreshold", 15.0);
                config.tpsCheckInterval =
                        cfg.getIntOrElse("tpsCheckInterval", 20);
                LazyChunksMod.LOGGER.info("Loaded config from {}", CONFIG_PATH);
            } catch (Exception e) {
                LazyChunksMod.LOGGER.error(
                        "Failed to load config, using defaults", e);
            }
        } else {
            config.save();
        }

        clampConfig(config);
        return config;
    }

    private static void clampConfig(LazyChunksConfig c) {
        if (c.sampleInterval < 1) c.sampleInterval = 1;
        if (c.sampleInterval > 100) c.sampleInterval = 100;
        if (c.teleportCheckInterval < 1) c.teleportCheckInterval = 1;
        if (c.teleportCheckInterval > 100) c.teleportCheckInterval = 100;
        if (c.teleportProtectionDuration < 20) c.portProtectionDuration = 20;
        if (c.teleportProtectionDuration > 600) c.teleportProtectionDuration = 600;
        if (c.tpsThreshold < 5.0) c.tpsThreshold = 5.0;
        if (c.tpsThreshold > 20.0) c.tpsThreshold = 20.0;
        if (c.tpsCheckInterval < 1) c.tpsCheckInterval = 1;
        if (c.tpsCheckInterval > 200) c.tpsCheckInterval = 200;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (Exception ignored) {}

        try (CommentedFileConfig cfg = CommentedFileConfig
                .builder(CONFIG_PATH)
                .autosave()
                .preserveInsertionOrder()
                .build()) {

            cfg.setComment("",
                    "\n Neo Lazy Chunks — Client-side chunk-loading throttle" +
                    "\n Config file: .minecraft/config/neo_lazy_chunks.toml" +
                    "\n Changes take effect on the next game launch.\n");

            cfg.setComment("showF3Overlay",
                    " Show mod status in the F3 debug screen.");
            cfg.set("showF3Overlay", showF3Overlay);

            cfg.setComment("lazyChunkLoadingEnabled",
                    " Master switch. Set to false to disable entirely.");
            cfg.set("lazyChunkLoadingEnabled", lazyChunkLoadingEnabled);

            cfg.setComment("targetFps",
                    "\n\n ---------- FPS-driven budget ----------");
            cfg.setComment("targetFps", cfg.getComment("targetFps") +
                    "\n Baseline framerate (render frames per second)." +
                    "\n   budget = baseWeightPerFrame * (smoothedFps / targetFps)");
            cfg.set("targetFps", targetFps);

            cfg.setComment("fpsThreshold",
                    " FPS above which throttling is fully disabled.");
            cfg.set("fpsThreshold", fpsThreshold);

            cfg.setComment("baseWeightPerFrame",
                    " Base work per frame at targetFps.\n Higher = faster loading, Lower = smoother.");
            cfg.set("baseWeightPerFrame", baseWeightPerFrame);

            cfg.setComment("maxFrameTimePercent",
                    " Max % of a single render frame for chunk processing (1–20).");
            cfg.set("maxFrameTimePercent", maxFrameTimePercent);

            cfg.setComment("proactiveThrottling",
                    "\n\n ---------- Adaptive throttling ----------");
            cfg.setComment("proactiveThrottling", cfg.getComment("proactiveThrottling") +
                    "\n Reduce budget preemptively when frame times are unstable.");
            cfg.set("proactiveThrottling", proactiveThrottling);

            cfg.setComment("teleportProtection",
                    " Extra conservative budget after teleport / dimension change.");
            cfg.set("teleportProtection", teleportProtection);

            cfg.setComment("teleportProtectionDuration",
                    " Duration in render frames (not game ticks).\n" +
                    " At 60 FPS: 120 frames ≈ 2s. Range 20–600.");
            cfg.set("teleportProtectionDuration", teleportProtectionDuration);

            cfg.setComment("weightChunkWithLight",
                    "\n\n ---------- Packet weights ----------");
            cfg.setComment("weightChunkWithLight", cfg.getComment("weightChunkWithLight") +
                    "\n Full chunk section with light data.");
            cfg.set("weightChunkWithLight", weightChunkWithLight);

            cfg.setComment("weightLightUpdate",
                    " Standalone light update.");
            cfg.set("weightLightUpdate", weightLightUpdate);

            cfg.setComment("weightForgetChunk",
                    " Chunk unload. Higher than load weights to" +
                    "\n force unload and load into separate frames.");
            cfg.set("weightForgetChunk", weightForgetChunk);

            cfg.setComment("sampleInterval",
                    "\n\n ---------- Sampling rates (render frames) ----------");
            cfg.setComment("sampleInterval", cfg.getComment("sampleInterval") +
                    "\n Frames between budget recalculations.\n" +
                    " At 60 FPS: 1 = 60 Hz, 5 = 12 Hz (default), 20 = 3 Hz");
            cfg.set("sampleInterval", sampleInterval);

            cfg.setComment("teleportCheckInterval",
                    " Frames between teleport checks.\n" +
                    " At 60 FPS: 1 = 60 Hz, 3 = 20 Hz (default), 10 = 6 Hz");
            cfg.set("teleportCheckInterval", teleportCheckInterval);

            cfg.setComment("tpsThrottleEnabled",
                    "\n\n ---------- TPS throttle (single-player only) ----------");
            cfg.setComment("tpsThrottleEnabled", cfg.getComment("tpsThrottleEnabled") +
                    "\n When server TPS drops below threshold, budget is" +
                    "\n clamped to minimum." +
                    "\n Only affects single-player (integrated server).");
            cfg.set("tpsThrottleEnabled", tpsThrottleEnabled);

            cfg.setComment("tpsThreshold",
                    " TPS threshold. Range 5.0–20.0. Default 15.0.");
            cfg.set("tpsThreshold", tpsThreshold);

            cfg.setComment("tpsCheckInterval",
                    " Frames between TPS checks (render frames, not game ticks).\n" +
                    " At 60 FPS: 20 frames ≈ 3 checks per second.\n" +
                    " Range 1–200. Default 20.");
            cfg.set("tpsCheckInterval", tpsCheckInterval);

            LazyChunksMod.LOGGER.info("Saved config to {}", CONFIG_PATH);
        } catch (Exception e) {
            LazyChunksMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static void reload() {
        INSTANCE = load();
        LazyChunksMod.LOGGER.info("Config reloaded");
    }
}