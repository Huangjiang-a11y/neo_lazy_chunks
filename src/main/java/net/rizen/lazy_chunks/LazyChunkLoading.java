package net.rizen.lazy_chunks;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.rizen.lazy_chunks.config.LazyChunksConfig;
import net.rizen.lazy_chunks.util.PacketRunnable;

import java.util.Queue;

public class LazyChunkLoading {

    private static final double MIN_WEIGHT_THRESHOLD = 5.0;

    private static int frameCounter = 0;
    private static final int FRAME_COUNTER_MAX = 10000;

    private static double smoothedFps = 60.0;
    private static final double FPS_SMOOTHING_FACTOR = 0.1;

    private static double emaFrameTime = 16.67;
    private static double emaFrameVariance = 0.0;
    private static final double EMA_ALPHA = 0.1;

    private static int previousQueueDepth = 0;
    private static double queueGrowthRate = 0.0;

    private static double cachedMaxWeight = 0;
    private static boolean cacheValid = false;
    private static double cachedFps = 60.0;
    private static int cachedQueueDepth = 0;

    private static double lastProcessingTimeMs = 0.0;
    private static int lastPendingTasks = 0;
    private static double lastWeight = 0;
    private static int lastFps = 0;
    private static double lastBudget = 0;
    private static int lastProcessed = 0;
    private static boolean lastThrottled = false;

    public static int getLastPendingTasks() { return lastPendingTasks; }
    public static double getLastWeight() { return lastWeight; }
    public static int getLastFps() { return lastFps; }
    public static double getLastBudget() { return lastBudget; }
    public static int getLastProcessed() { return lastProcessed; }
    public static boolean wasThrottled() { return lastThrottled; }
    public static double getSmoothedFps() { return smoothedFps; }
    public static double getFrameTimeVariance() { return emaFrameVariance; }
    public static double getQueueGrowthRate() { return queueGrowthRate; }
    public static double getLastProcessingTimeMs() { return lastProcessingTimeMs; }

    private static double getMinimumBudget() {
        LazyChunksConfig config = LazyChunksConfig.getInstance();
        return Math.max(config.weightChunkWithLight,
               Math.max(config.weightLightUpdate, config.weightForgetChunk));
    }

    private static void sampleFps() {
        int instantFps = Math.max(Minecraft.getInstance().getFps(), 1);
        smoothedFps = smoothedFps * (1.0 - FPS_SMOOTHING_FACTOR)
                    + instantFps * FPS_SMOOTHING_FACTOR;
    }

    public static void recordFrameTime(double frameTimeMs) {
        if (emaFrameTime <= 0) {
            emaFrameTime = frameTimeMs;
            emaFrameVariance = 0;
            return;
        }
        double prevAvg = emaFrameTime;
        emaFrameTime += EMA_ALPHA * (frameTimeMs - emaFrameTime);
        double diff = frameTimeMs - prevAvg;
        emaFrameVariance += EMA_ALPHA * (diff * diff - emaFrameVariance);
    }

    public static void recordProcessingTime(double timeMs) {
        lastProcessingTimeMs = timeMs;
    }

    private static void updateQueueMetrics(int currentDepth) {
        queueGrowthRate = queueGrowthRate * 0.8
                        + (currentDepth - previousQueueDepth) * 0.2;
        previousQueueDepth = currentDepth;
    }

    private static double getStabilityMultiplier() {
        if (emaFrameTime <= 0) return 1.0;
        double stdDev = Math.sqrt(Math.max(0, emaFrameVariance));
        double stabilityFactor = 1.0 / (1.0 + stdDev / emaFrameTime);
        return Math.max(0.5, Math.min(1.0, stabilityFactor));
    }

    private static double getQueueGrowthMultiplier() {
        if (queueGrowthRate > 10) return 0.5;
        else if (queueGrowthRate > 5) return 0.7;
        else if (queueGrowthRate < -5) return 1.2;
        return 1.0;
    }

    private static double calculateMaxWeight() {
        LazyChunksConfig config = LazyChunksConfig.getInstance();
        double maxWeight = config.baseWeightPerFrame
                         * smoothedFps / config.targetFps;
        if (config.proactiveThrottling) {
            maxWeight *= getStabilityMultiplier();
            maxWeight *= getQueueGrowthMultiplier();
        }
        if (config.teleportProtection) {
            maxWeight *= TeleportDetector.getBudgetMultiplier();
        }
        return Math.max(maxWeight, getMinimumBudget());
    }

    public static long getMaxProcessingTimeNanos() {
        LazyChunksConfig config = LazyChunksConfig.getInstance();
        double targetFrameTimeMs = 1000.0 / Math.max(config.targetFps, 1);
        double maxTimeMs = targetFrameTimeMs
                         * (config.maxFrameTimePercent / 100.0);
        return (long) (maxTimeMs * 1_000_000);
    }

    private static boolean isCacheValid(int currentQueueDepth) {
        if (!cacheValid) return false;
        if (Math.abs(smoothedFps - cachedFps) > 5.0) return false;
        if (cachedQueueDepth > 0) {
            double ratio = (double) currentQueueDepth / cachedQueueDepth;
            if (ratio > 1.5 || ratio < 0.5) return false;
        }
        return true;
    }

    private static void doFullSample(int taskCount) {
        sampleFps();
        updateQueueMetrics(taskCount);
        LazyChunksConfig config = LazyChunksConfig.getInstance();
        if (smoothedFps >= config.fpsThreshold) {
            cachedMaxWeight = Double.MAX_VALUE;
        } else {
            cachedMaxWeight = calculateMaxWeight();
        }
        cacheValid = true;
        cachedFps = smoothedFps;
        cachedQueueDepth = taskCount;
    }

    public static int getTaskCount(Queue<Runnable> pendingTasks) {
        LazyChunksConfig config = LazyChunksConfig.getInstance();

        if (!config.lazyChunkLoadingEnabled) {
            lastThrottled = false;
            return Integer.MAX_VALUE;
        }

        if (pendingTasks.isEmpty()) {
            lastPendingTasks = 0;
            lastWeight = 0;
            lastThrottled = false;
            cacheValid = false;
            return 0;
        }

        frameCounter++;
        if (frameCounter >= FRAME_COUNTER_MAX) frameCounter = 0;

        Runnable[] tasks = pendingTasks.toArray(new Runnable[0]);
        int taskCount = tasks.length;
        lastPendingTasks = taskCount;
        lastFps = Minecraft.getInstance().getFps();

        double totalWeight = getTotalChunkWeight(tasks);
        lastWeight = totalWeight;

        if (totalWeight < MIN_WEIGHT_THRESHOLD) {
            lastThrottled = false;
            lastBudget = totalWeight;
            lastProcessed = taskCount;
            cacheValid = false;
            return Integer.MAX_VALUE;
        }

        int sampleInterval = Math.max(1, config.sampleInterval);
        boolean shouldSample = (frameCounter % sampleInterval == 0);

        if (shouldSample) {
            doFullSample(taskCount);
            if (cachedMaxWeight == Double.MAX_VALUE) {
                lastThrottled = false;
                lastBudget = totalWeight;
                lastProcessed = taskCount;
                return Integer.MAX_VALUE;
            }
        } else {
            if (!isCacheValid(taskCount)) {
                doFullSample(taskCount);
            }
            if (!cacheValid) {
                doFullSample(taskCount);
            }
            if (cachedMaxWeight == Double.MAX_VALUE) {
                lastThrottled = false;
                lastBudget = totalWeight;
                lastProcessed = taskCount;
                return Integer.MAX_VALUE;
            }
        }

        int limit = getCountForWeight(tasks, cachedMaxWeight);
        lastThrottled = true;
        lastBudget = cachedMaxWeight;
        lastProcessed = limit;
        return limit;
    }

    private static int getCountForWeight(Runnable[] tasks, double maxWeight) {
        double currentWeight = 0.0;
        boolean hasUnload = false;
        boolean hasLoad = false;

        for (int i = 0; i < tasks.length; i++) {
            double taskWeight = getChunkUpdateWeight(tasks[i]);

            boolean isUnload = isUnloadTask(tasks[i]);
            boolean isLoad = isLoadTask(tasks[i]);

            if ((isLoad && hasUnload) || (isUnload && hasLoad)) {
                return i;
            }

            if (isUnload) hasUnload = true;
            if (isLoad) hasLoad = true;

            if (currentWeight + taskWeight > maxWeight && i > 0) {
                return i;
            }
            currentWeight += taskWeight;
        }
        return tasks.length;
    }

    private static boolean isUnloadTask(Runnable task) {
        if (task instanceof PacketRunnable pr) {
            return pr.getPacket() instanceof ClientboundForgetLevelChunkPacket;
        }
        return false;
    }

    private static boolean isLoadTask(Runnable task) {
        if (task instanceof PacketRunnable pr) {
            Packet<?> p = pr.getPacket();
            return p instanceof ClientboundLevelChunkWithLightPacket
                || p instanceof ClientboundLightUpdatePacket;
        }
        return false;
    }

    private static double getTotalChunkWeight(Runnable[] tasks) {
        double weight = 0.0;
        for (Runnable task : tasks) {
            weight += getChunkUpdateWeight(task);
        }
        return weight;
    }

    private static double getChunkUpdateWeight(Runnable task) {
        if (task instanceof PacketRunnable packetRunnable) {
            LazyChunksConfig config = LazyChunksConfig.getInstance();
            Packet<?> packet = packetRunnable.getPacket();
            if (packet instanceof ClientboundLevelChunkWithLightPacket) {
                return config.weightChunkWithLight;
            }
            if (packet instanceof ClientboundLightUpdatePacket) {
                return config.weightLightUpdate;
            }
            if (packet instanceof ClientboundForgetLevelChunkPacket) {
                return config.weightForgetChunk;
            }
        }
        return 0.0;
    }
}