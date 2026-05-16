package net.rizen.lazy_chunks;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rizen.lazy_chunks.config.LazyChunksConfig;

@Mod("lazy_chunks")
public class LazyChunksMod {
    public static final String MOD_ID = "lazy_chunks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = "2.0+mc1.21.1";

    public LazyChunksMod() {
        LazyChunksConfig config = LazyChunksConfig.getInstance();
        LOGGER.info("LazyChunks {} loaded", VERSION);
        LOGGER.info("Lazy Chunk Loading: {} | targetFps={} | fpsThreshold={} | baseWeight={} | sampleInterval={} | teleportCheckInterval={}",
                config.lazyChunkLoadingEnabled ? "enabled" : "disabled",
                config.targetFps,
                config.fpsThreshold,
                config.baseWeightPerFrame,
                config.sampleInterval,
                config.teleportCheckInterval);
    }
}