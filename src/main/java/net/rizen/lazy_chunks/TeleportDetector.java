package net.rizen.lazy_chunks;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class TeleportDetector {

    private static Vec3 lastPosition = null;
    private static String lastDimension = null;
    private static int teleportCooldown = 0;

    private static final double TELEPORT_DISTANCE_THRESHOLD = 64.0;
    private static final int TELEPORT_COOLDOWN_FRAMES = 120;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            lastPosition = null;
            lastDimension = null;
            return;
        }

        Vec3 currentPos = mc.player.position();
        String currentDim = mc.level.dimension().location().toString();

        if (lastPosition != null && lastDimension != null) {
            if (!currentDim.equals(lastDimension)) {
                teleportCooldown = TELEPORT_COOLDOWN_FRAMES;
            } else {
                // 先用曼哈顿距离快速判断（无平方根，更快）
                double manhattanDist = Math.abs(currentPos.x - lastPosition.x)
                                     + Math.abs(currentPos.y - lastPosition.y)
                                     + Math.abs(currentPos.z - lastPosition.z);
                if (manhattanDist > TELEPORT_DISTANCE_THRESHOLD * 1.5) {
                    teleportCooldown = TELEPORT_COOLDOWN_FRAMES;
                } else if (manhattanDist > TELEPORT_DISTANCE_THRESHOLD * 0.5) {
                    if (lastPosition.distanceTo(currentPos) > TELEPORT_DISTANCE_THRESHOLD) {
                        teleportCooldown = TELEPORT_COOLDOWN_FRAMES;
                    }
                }
            }
        }

        lastPosition = currentPos;
        lastDimension = currentDim;

        if (teleportCooldown > 0) {
            teleportCooldown--;
        }
    }

    public static boolean isTeleportRecovery() {
        return teleportCooldown > 0;
    }

    public static int getCooldownRemaining() {
        return teleportCooldown;
    }

    public static double getBudgetMultiplier() {
        if (teleportCooldown <= 0) return 1.0;
        double progress = 1.0 - ((double) teleportCooldown / TELEPORT_COOLDOWN_FRAMES);
        return 0.3 + (0.7 * progress);
    }

    public static void reset() {
        lastPosition = null;
        lastDimension = null;
        teleportCooldown = 0;
    }
}