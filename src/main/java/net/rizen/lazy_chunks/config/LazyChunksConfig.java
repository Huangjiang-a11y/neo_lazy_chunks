package net.rizen.lazy_chunks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import net.rizen.lazy_chunks.LazyChunksMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LazyChunksConfig {
    private static LazyChunksConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("lazy_chunks.json");

    /** 启用/禁用模组 */
    public boolean lazyChunkLoadingEnabled = true;

    /** 基准FPS */
    public int targetFps = 60;

    /** 超过此FPS时停止节流 */
    public int fpsThreshold = 80;

    /** 每帧基础区块处理权重 */
    public double baseWeightPerFrame = 3.0;

    /** 最大帧时间占比 (1-20) */
    public double maxFrameTimePercent = 12.0;

    /** 主动节流 */
    public boolean proactiveThrottling = true;

    /** 传送保护 */
    public boolean teleportProtection = true;

    /** 最小预算 */
    public double minimumBudget = 3.0;

    /**
     * 采样间隔（单位：帧）
     * 每 N 帧做一次完整的 FPS/预算计算。
     */
    public int sampleInterval = 5;

    /**
     * 传送检测间隔（单位：帧）
     * 每 N 帧检测一次玩家是否传送/切换维度。
     */
    public int teleportCheckInterval = 3;

    public static LazyChunksConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static LazyChunksConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                LazyChunksConfig config = GSON.fromJson(json, LazyChunksConfig.class);
                if (config != null) {
                    clampConfig(config);
                    LazyChunksMod.LOGGER.info("Loaded config from {}", CONFIG_PATH);
                    return config;
                }
            } catch (IOException e) {
                LazyChunksMod.LOGGER.error("Failed to load config", e);
            }
        }
        LazyChunksConfig config = new LazyChunksConfig();
        config.save();
        return config;
    }

    private static void clampConfig(LazyChunksConfig config) {
        if (config.sampleInterval < 1) config.sampleInterval = 1;
        if (config.sampleInterval > 100) config.sampleInterval = 100;
        if (config.teleportCheckInterval < 1) config.teleportCheckInterval = 1;
        if (config.teleportCheckInterval > 100) config.teleportCheckInterval = 100;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
            LazyChunksMod.LOGGER.info("Saved config to {}", CONFIG_PATH);
        } catch (IOException e) {
            LazyChunksMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static void reload() {
        INSTANCE = load();
        LazyChunksMod.LOGGER.info("Config reloaded");
    }
}