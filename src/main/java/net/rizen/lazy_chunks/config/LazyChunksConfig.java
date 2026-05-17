package net.rizen.lazy_chunks.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.rizen.lazy_chunks.LazyChunksMod;

import java.nio.file.Files;
import java.nio.file.Path;

public class LazyChunksConfig {
    private static LazyChunksConfig INSTANCE;
    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("lazy_chunks.toml");

    public boolean lazyChunkLoadingEnabled = true;
    public int targetFps = 60;
    public int fpsThreshold = 80;
    public double baseWeightPerFrame = 3.0;
    public double maxFrameTimePercent = 12.0;
    public boolean proactiveThrottling = true;
    public boolean teleportProtection = true;
    public double weightChunkWithLight = 1.0;
    public double weightLightUpdate = 0.2;
    public double weightForgetChunk = 2.6;
    public double minimumBudget = 3.0;
    public int sampleInterval = 5;
    public int teleportCheckInterval = 3;

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
                config.lazyChunkLoadingEnabled =
                        cfg.getOrElse("lazyChunkLoadingEnabled", true);
                config.targetFps =
                        cfg.getIntOrElse("targetFps", 60);
                config.fpsThreshold =
                        cfg.getIntOrElse("fpsThreshold", 80);
                config.baseWeightPerFrame =
                        cfg.getDoubleOrElse("baseWeightPerFrame", 3.0);
                config.maxFrameTimePercent =
                        cfg.getDoubleOrElse("maxFrameTimePercent", 12.0);
                config.proactiveThrottling =
                        cfg.getOrElse("proactiveThrottling", true);
                config.teleportProtection =
                        cfg.getOrElse("teleportProtection", true);
                config.weightChunkWithLight =
                        cfg.getDoubleOrElse("weightChunkWithLight", 1.0);
                config.weightLightUpdate =
                        cfg.getDoubleOrElse("weightLightUpdate", 0.2);
                config.weightForgetChunk =
                        cfg.getDoubleOrElse("weightForgetChunk", 2.6);
                config.minimumBudget =
                        cfg.getDoubleOrElse("minimumBudget", 3.0);
                config.sampleInterval =
                        cfg.getIntOrElse("sampleInterval", 5);
                config.teleportCheckInterval =
                        cfg.getIntOrElse("teleportCheckInterval", 3);
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
        double maxWeight = Math.max(c.weightChunkWithLight,
                           Math.max(c.weightLightUpdate, c.weightForgetChunk));
        if (c.minimumBudget < maxWeight) c.minimumBudget = maxWeight;
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
                    "\n LazyChunks — Client-side chunk-loading throttle" +
                    "\n Config file: .minecraft/config/lazy_chunks.toml" +
                    "\n Changes take effect on the next game launch.\n");

            cfg.setComment("lazyChunkLoadingEnabled",
                    " Master switch. Set to false to disable the mod " +
                    "entirely (vanilla behaviour).");
            cfg.set("lazyChunkLoadingEnabled", lazyChunkLoadingEnabled);

            cfg.setComment("targetFps",
                    "\n\n ---------- FPS-driven budget ----------");
            cfg.setComment("targetFps", cfg.getComment("targetFps") +
                    "\n Baseline framerate used by the budget formula." +
                    "\n The budget scales linearly with current FPS " +
                    "relative to this value:" +
                    "\n   budget = baseWeightPerFrame * (smoothedFps " +
                    "/ targetFps)");
            cfg.set("targetFps", targetFps);

            cfg.setComment("fpsThreshold",
                    " FPS threshold above which throttling is fully " +
                    "disabled.\n When smoothed FPS >= this value, all " +
                    "pending chunk packets are processed\n immediately " +
                    "(same as vanilla).");
            cfg.set("fpsThreshold", fpsThreshold);

            cfg.setComment("baseWeightPerFrame",
                    " Base amount of \"work\" allowed per frame at " +
                    "targetFps.\n Each packet type consumes a configurable " +
                    "amount of this budget.\n Higher = faster terrain " +
                    "loading but more frame-time pressure.\n Lower  = " +
                    "smoother frames but slower chunk population.");
            cfg.set("baseWeightPerFrame", baseWeightPerFrame);

            cfg.setComment("maxFrameTimePercent",
                    " Maximum percentage of a single frame-time that may " +
                    "be spent on chunk\n processing, regardless of the " +
                    "weight budget. Range 1–20.\n At 60 FPS (~16.7 ms per " +
                    "frame), 12 % ≈ 2 ms ceiling.");
            cfg.set("maxFrameTimePercent", maxFrameTimePercent);

            cfg.setComment("proactiveThrottling",
                    "\n\n ---------- Adaptive throttling ----------");
            cfg.setComment("proactiveThrottling", cfg.getComment(
                    "proactiveThrottling") +
                    "\n Analyses frame-time variance (jitter) and queue " +
                    "pressure in addition\n to the raw FPS number. Unstable " +
                    "frame-times reduce the budget proactively,\n before a " +
                    "visible FPS drop occurs.");
            cfg.set("proactiveThrottling", proactiveThrottling);

            cfg.setComment("teleportProtection",
                    " Clamps the budget to a conservative fraction for 120 " +
                    "frames (≈ 2 s) after\n a teleport or dimension change. " +
                    "Prevents the burst of chunk packets that\n follows a " +
                    "dimension switch from causing a severe hitch.");
            cfg.set("teleportProtection", teleportProtection);

            cfg.setComment("weightChunkWithLight",
                    "\n\n ---------- Packet weights — how much budget " +
                    "each packet type consumes ----------");
            cfg.setComment("weightChunkWithLight", cfg.getComment(
                    "weightChunkWithLight") +
                    "\n Full chunk section with accompanying light data.");
            cfg.set("weightChunkWithLight", weightChunkWithLight);

            cfg.setComment("weightLightUpdate",
                    " Standalone light update (no chunk geometry; sent " +
                    "when lighting changes\n in an already-loaded chunk).");
            cfg.set("weightLightUpdate", weightLightUpdate);

            cfg.setComment("weightForgetChunk",
                    " Chunk unload (ClientboundForgetLevelChunkPacket).\n" +
                    " Deliberately set higher than the load weight so " +
                    "that, when a movement\n tick queues both unloads and " +
                    "loads, the unloads consume the budget first.\n This " +
                    "forces \"delete geometry\" and \"create geometry\" " +
                    "phases into separate frames,\n avoiding the single-" +
                    "frame spike where both happen at once.");
            cfg.set("weightForgetChunk", weightForgetChunk);

            cfg.setComment("minimumBudget",
                    "\n\n ---------- Safety floor ----------");
            cfg.setComment("minimumBudget", cfg.getComment("minimumBudget") +
                    "\n Absolute minimum budget, applied after all " +
                    "multipliers.\n Must be >= the heaviest single-packet " +
                    "weight; otherwise the mod could\n deadlock when a " +
                    "packet heavier than the floor arrives.\n The mod will " +
                    "auto-clamp this value on load if it is too low.");
            cfg.set("minimumBudget", minimumBudget);

            cfg.setComment("sampleInterval",
                    "\n\n ---------- Internal sampling rates " +
                    "(CPU trade-off) ----------");
            cfg.setComment("sampleInterval", cfg.getComment("sampleInterval") +
                    "\n How many frames between full budget recalculations." +
                    "\n   1  — every frame (most responsive, highest " +
                    "overhead)" +
                    "\n   5  — default balance (~12 Hz at 60 FPS)" +
                    "\n   20 — least overhead, budget reacts ~330 ms late");
            cfg.set("sampleInterval", sampleInterval);

            cfg.setComment("teleportCheckInterval",
                    " How many frames between teleport / dimension-change " +
                    "checks.\n Uses a cheap Manhattan-distance pre-filter; " +
                    "only falls back to the\n expensive Euclidean distance " +
                    "when the fast check passes.\n   1  — every frame\n   " +
                    "3  — default (~20 Hz at 60 FPS)\n   10 — minimal " +
                    "overhead");
            cfg.set("teleportCheckInterval", teleportCheckInterval);

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