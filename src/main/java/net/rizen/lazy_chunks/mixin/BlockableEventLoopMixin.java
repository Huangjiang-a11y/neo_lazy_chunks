package net.rizen.lazy_chunks.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.util.thread.BlockableEventLoop;
import net.rizen.lazy_chunks.LazyChunkLoading;
import net.rizen.lazy_chunks.TeleportDetector;
import net.rizen.lazy_chunks.config.LazyChunksConfig;
import net.rizen.lazy_chunks.mixin.accessor.IBlockableEventLoopAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockableEventLoop.class)
public abstract class BlockableEventLoopMixin {

    @Unique
    private static long lazychunks$lastFrameStartTime = 0;

    @Unique
    private static int lazychunks$localFrameCount = 0;

    @Unique
    private static final int lazychunks$FRAME_COUNTER_MAX = 10000;

    @Inject(method = "runAllTasks", at = @At("HEAD"), cancellable = true)
    private void lazychunks$throttleChunkLoading(CallbackInfo ci) {
        BlockableEventLoop<?> self = (BlockableEventLoop<?>) (Object) this;
        if (self != Minecraft.getInstance()) {
            return;
        }

        // 帧计数器 + 防溢出
        lazychunks$localFrameCount++;
        if (lazychunks$localFrameCount >= lazychunks$FRAME_COUNTER_MAX) {
            lazychunks$localFrameCount = 0;
        }

        long currentTime = System.nanoTime();

        // 记录帧时间（每帧都记录，EMA O(1) 开销极低）
        if (lazychunks$lastFrameStartTime > 0) {
            double frameTimeMs = (currentTime - lazychunks$lastFrameStartTime) / 1_000_000.0;
            LazyChunkLoading.recordFrameTime(frameTimeMs);
        }
        lazychunks$lastFrameStartTime = currentTime;

        // 传送检测 — 间隔从配置读取
        int teleportInterval = LazyChunksConfig.getInstance().teleportCheckInterval;
        if (lazychunks$localFrameCount % teleportInterval == 0) {
            TeleportDetector.tick();
        }

        // 获取任务限制（内部已按 sampleInterval 降频）
        int taskLimit = LazyChunkLoading.getTaskCount(
                ((IBlockableEventLoopAccessor) this).lazychunks$pendingRunnables()
        );

        long maxTimeNanos = LazyChunkLoading.getMaxProcessingTimeNanos();
        long startTime = System.nanoTime();

        while (self.pollTask()) {
            --taskLimit;
            if (taskLimit <= 0) break;
            if (System.nanoTime() - startTime > maxTimeNanos) break;
        }

        double processingTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        LazyChunkLoading.recordProcessingTime(processingTimeMs);

        ci.cancel();
    }
}