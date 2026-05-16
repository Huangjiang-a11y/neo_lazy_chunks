package net.rizen.lazy_chunks.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.rizen.lazy_chunks.LazyChunksMod;
import net.rizen.lazy_chunks.LazyChunkLoading;
import net.rizen.lazy_chunks.TeleportDetector;
import net.rizen.lazy_chunks.config.LazyChunksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {

    @Unique
    private static long lazychunks$lastUpdateTime = 0;

    @Unique
    private static final long lazychunks$UPDATE_INTERVAL_MS = 100;

    @Unique
    private static int lazychunks$cachedPending = 0;

    @Unique
    private static double lazychunks$cachedWeight = 0;

    @Unique
    private static double lazychunks$cachedBudget = 0;

    @Unique
    private static int lazychunks$cachedProcessed = 0;

    @Unique
    private static double lazychunks$cachedSmoothedFps = 0;

    @Unique
    private static double lazychunks$cachedVariance = 0;

    @Unique
    private static double lazychunks$cachedProcessingTime = 0;

    @Unique
    private static double lazychunks$cachedQueueGrowth = 0;

    @Unique
    private static boolean lazychunks$cachedTeleportRecovery = false;

    @Inject(method = "getSystemInformation", at = @At("RETURN"))
    private void lazychunks$addDebugInfo(CallbackInfoReturnable<List<String>> cir) {
        List<String> info = cir.getReturnValue();
        LazyChunksConfig config = LazyChunksConfig.getInstance();

        long now = System.currentTimeMillis();
        if (now - lazychunks$lastUpdateTime > lazychunks$UPDATE_INTERVAL_MS) {
            lazychunks$lastUpdateTime = now;
            lazychunks$cachedPending = LazyChunkLoading.getLastPendingTasks();
            lazychunks$cachedWeight = LazyChunkLoading.getLastWeight();
            lazychunks$cachedBudget = LazyChunkLoading.getLastBudget();
            lazychunks$cachedProcessed = LazyChunkLoading.getLastProcessed();
            lazychunks$cachedSmoothedFps = LazyChunkLoading.getSmoothedFps();
            lazychunks$cachedVariance = LazyChunkLoading.getFrameTimeVariance();
            lazychunks$cachedProcessingTime = LazyChunkLoading.getLastProcessingTimeMs();
            lazychunks$cachedQueueGrowth = LazyChunkLoading.getQueueGrowthRate();
            lazychunks$cachedTeleportRecovery = TeleportDetector.isTeleportRecovery();
        }

        int insertIndex = 0;
        info.add(insertIndex++, ChatFormatting.YELLOW.toString() + ChatFormatting.BOLD
                + "LazyChunks " + LazyChunksMod.VERSION);

        if (config.lazyChunkLoadingEnabled) {
            info.add(insertIndex++, String.format("Pending: %d | Weight: %.1f | Budget: %.1f",
                    lazychunks$cachedPending,
                    lazychunks$cachedWeight,
                    lazychunks$cachedBudget));

            info.add(insertIndex++, String.format("Smoothed FPS: %.0f | Process: %.2fms | Var: %.1f",
                    lazychunks$cachedSmoothedFps,
                    lazychunks$cachedProcessingTime,
                    lazychunks$cachedVariance));

            StringBuilder status = new StringBuilder();
            if (LazyChunkLoading.wasThrottled()) {
                status.append(ChatFormatting.GREEN).append("Throttling");
            } else {
                status.append(ChatFormatting.GRAY).append("Idle");
            }
            if (lazychunks$cachedQueueGrowth > 5) {
                status.append(ChatFormatting.GOLD).append(" [Queue+]");
            }
            if (lazychunks$cachedTeleportRecovery) {
                status.append(ChatFormatting.RED).append(" [Teleport]");
            }
            info.add(insertIndex++, status.toString());
        } else {
            info.add(insertIndex++, ChatFormatting.GRAY + "Disabled");
        }

        info.add(insertIndex, "");
    }
}