package derp.scalingbreak.client;

import derp.scalingbreak.client.compat.BetterBlockEntitiesCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockBreakScaleController {

    private static final Map<Integer, BreakAnimation> ACTIVE_ANIMATIONS = new ConcurrentHashMap<>();

    private static final Map<BlockPos, BreakAnimation> RECOVERING_ANIMATIONS = new ConcurrentHashMap<>();

    private static final ThreadLocal<Boolean> RENDERING_SHRINKING_BLOCK = ThreadLocal.withInitial(() -> false);


    private BlockBreakScaleController() {
    }

    public static final class BreakAnimation {

        private final BlockPos pos;
        private final Set<BlockPos> linkedPositions;
        private final Vec3 pivot;

        private int stage = -1;

        private float currentScale = 1.0F;
        private float targetScale = 1.0F;

        private boolean recovering;
        private volatile boolean hideOriginal;
        private boolean restoreRequested;

        private BreakAnimation(BlockPos pos, Set<BlockPos> linkedPositions, Vec3 pivot) {
            this.pos = pos.immutable();
            this.linkedPositions = Set.copyOf(linkedPositions);
            this.pivot = pivot;
            this.hideOriginal = true;
        }

        public BlockPos pos() {
            return pos;
        }

        public float scale() {
            return currentScale;
        }

        public Vec3 pivot() {
            return pivot;
        }

        public boolean affects(BlockPos blockPos) {
            return linkedPositions.contains(blockPos);
        }

        public Set<BlockPos> linkedPositions() {
            return linkedPositions;
        }


    }

    public record RenderPart(BlockPos pos, BreakAnimation animation) {
    }

    public static void set(int entityId, BlockPos newPos, int newStage) {
        ScaleBreakConfig config = ScaleBreakConfig.get();

        if (!config.enabled) {
            return;
        }

        BreakAnimation active = ACTIVE_ANIMATIONS.get(entityId);

        if (newStage < 0 || newStage > 9) {
            if (active != null && active.pos.equals(newPos)) {
                ACTIVE_ANIMATIONS.remove(entityId, active);
                startRecovery(active);
            }

            return;
        }


        if (active != null && !active.pos.equals(newPos)) {
            ACTIVE_ANIMATIONS.remove(entityId, active);
            startRecovery(active);
            active = null;
        }

        if (active == null) {
            BreakAnimation recovering = removeRecoveringForPos(newPos);

            Minecraft minecraft = Minecraft.getInstance();
            Set<BlockPos> linkedPositions = minecraft.level != null && config.multiblocks ? LinkedBlockResolver.resolve(minecraft.level, newPos) : Set.of(newPos.immutable());

            active = new BreakAnimation(newPos, linkedPositions, calculatePivot(linkedPositions));

            if (recovering != null) {
                active.currentScale = recovering.currentScale;
            }

            ACTIVE_ANIMATIONS.put(entityId, active);

            active.hideOriginal = true;
            active.restoreRequested = false;

            for (BlockPos linkedPos : active.linkedPositions) {
                BetterBlockEntitiesCompat.beginAnimation(linkedPos);
                markDirty(linkedPos);
            }
        }

        // Mining resumed while this block was in the restore handoff.
        if (!active.hideOriginal || active.restoreRequested) {
            active.hideOriginal = true;
            active.restoreRequested = false;
            markDirtyAll(active);
        }

        active.stage = newStage;
        active.recovering = false;

        float progress = (newStage + 1) / 10.0F;

        active.targetScale = 1.0F - progress * config.shrinkAmount;
    }

    private static void startRecovery(BreakAnimation animation) {
        animation.stage = -1;
        animation.targetScale = 1.0F;
        animation.recovering = true;

        animation.hideOriginal = true;
        animation.restoreRequested = false;

        RECOVERING_ANIMATIONS.merge(animation.pos, animation, (existing, incoming) -> incoming.currentScale < existing.currentScale ? incoming : existing);
    }

    public static void update(float deltaTicks) {
        float dt = deltaTicks / 20.0F;

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        for (Map.Entry<Integer, BreakAnimation> entry : ACTIVE_ANIMATIONS.entrySet()) {
            BreakAnimation animation = entry.getValue();

            if (minecraft.level.getBlockState(animation.pos).isAir()) {
                ACTIVE_ANIMATIONS.remove(entry.getKey(), animation);
                markDirtyAll(animation);
                continue;
            }

            updateAnimation(animation, dt);
        }

        for (Map.Entry<BlockPos, BreakAnimation> entry : RECOVERING_ANIMATIONS.entrySet()) {
            BreakAnimation animation = entry.getValue();

            if (minecraft.level.getBlockState(animation.pos).isAir()) {
                RECOVERING_ANIMATIONS.remove(entry.getKey(), animation);
                for (BlockPos linkedPos : animation.linkedPositions) {
                    BetterBlockEntitiesCompat.endAnimation(linkedPos);
                }
                markDirtyAll(animation);
                continue;
            }

            if (updateAnimation(animation, dt)) {
                for (BlockPos linkedPos : animation.linkedPositions) {
                    BetterBlockEntitiesCompat.endAnimation(linkedPos);
                }
                RECOVERING_ANIMATIONS.remove(entry.getKey(), animation);
            }
        }
    }

    private static boolean updateAnimation(BreakAnimation animation, float dt) {
        ScaleBreakConfig config = ScaleBreakConfig.get();
        float speed = animation.recovering ? config.recoverySpeed : config.shrinkSpeed;

        float factor = 1.0F - (float) Math.exp(-speed * dt);

        animation.currentScale += (animation.targetScale - animation.currentScale) * factor;


        if (animation.recovering && !animation.restoreRequested && animation.currentScale >= 0.99F) {

            animation.restoreRequested = true;
            animation.hideOriginal = false;

            markDirtyAll(animation);
        }


        if (animation.recovering && animation.restoreRequested && animation.currentScale >= 0.999F) {

            animation.currentScale = 1.0F;
            animation.targetScale = 1.0F;
            animation.hideOriginal = false;

            return true;
        }

        return false;
    }

    public static @Nullable BreakAnimation getForPos(BlockPos pos) {
        BreakAnimation best = null;

        for (BreakAnimation animation : ACTIVE_ANIMATIONS.values()) {
            if (!animation.affects(pos)) {
                continue;
            }

            if (best == null || animation.currentScale < best.currentScale) {
                best = animation;
            }
        }

        for (BreakAnimation recovering : RECOVERING_ANIMATIONS.values()) {
            if (!recovering.affects(pos)) {
                continue;
            }

            if (best == null || recovering.currentScale < best.currentScale) {
                best = recovering;
            }
        }

        return best;
    }

    public static boolean shouldHideOriginal(BlockPos pos) {
        for (BreakAnimation animation : ACTIVE_ANIMATIONS.values()) {
            if (animation.affects(pos) && animation.hideOriginal) {
                return true;
            }
        }

        for (BreakAnimation recovering : RECOVERING_ANIMATIONS.values()) {
            if (recovering.affects(pos) && recovering.hideOriginal) {
                return true;
            }
        }

        return false;
    }

    public static float scale(BlockPos pos) {
        BreakAnimation animation = getForPos(pos);

        return animation != null ? animation.currentScale : 1.0F;
    }

    public static Collection<BreakAnimation> renderAnimationGroups() {
        Map<BreakAnimation, BreakAnimation> unique = new java.util.IdentityHashMap<>();

        for (BreakAnimation animation : ACTIVE_ANIMATIONS.values()) {
            unique.put(animation, animation);
        }

        for (BreakAnimation animation : RECOVERING_ANIMATIONS.values()) {
            unique.put(animation, animation);
        }

        return new ArrayList<>(unique.values());
    }

    public static Collection<RenderPart> renderAnimations() {
        Map<BlockPos, BreakAnimation> byPos = new HashMap<>();

        for (BreakAnimation animation : ACTIVE_ANIMATIONS.values()) {
            putBestParts(byPos, animation);
        }

        for (BreakAnimation animation : RECOVERING_ANIMATIONS.values()) {
            putBestParts(byPos, animation);
        }

        Collection<RenderPart> parts = new ArrayList<>(byPos.size());

        for (Map.Entry<BlockPos, BreakAnimation> entry : byPos.entrySet()) {
            parts.add(new RenderPart(entry.getKey(), entry.getValue()));
        }

        return parts;
    }

    private static void putBestParts(Map<BlockPos, BreakAnimation> map, BreakAnimation animation) {
        for (BlockPos linkedPos : animation.linkedPositions) {
            BreakAnimation existing = map.get(linkedPos);

            if (existing == null || animation.currentScale < existing.currentScale) {
                map.put(linkedPos, animation);
            }
        }
    }

    private static @Nullable BreakAnimation removeRecoveringForPos(BlockPos pos) {
        for (Map.Entry<BlockPos, BreakAnimation> entry : RECOVERING_ANIMATIONS.entrySet()) {
            BreakAnimation animation = entry.getValue();

            if (!animation.affects(pos)) {
                continue;
            }

            if (RECOVERING_ANIMATIONS.remove(entry.getKey(), animation)) {
                return animation;
            }
        }

        return null;
    }

    private static Vec3 calculatePivot(Set<BlockPos> positions) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || positions.isEmpty()) {
            BlockPos pos = positions.isEmpty() ? BlockPos.ZERO : positions.iterator().next();
            return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        }

        AABB combined = null;

        for (BlockPos pos : positions) {
            BlockState state = minecraft.level.getBlockState(pos);
            AABB bounds;

            if (state.getShape(minecraft.level, pos).isEmpty()) {
                bounds = new AABB(pos);
            } else {
                bounds = state.getShape(minecraft.level, pos).bounds().move(pos);
            }

            if (combined == null) {
                combined = bounds;
            } else {
                combined = new AABB(Math.min(combined.minX, bounds.minX), Math.min(combined.minY, bounds.minY), Math.min(combined.minZ, bounds.minZ), Math.max(combined.maxX, bounds.maxX), Math.max(combined.maxY, bounds.maxY), Math.max(combined.maxZ, bounds.maxZ));
            }
        }

        if (combined == null) {
            BlockPos pos = positions.iterator().next();
            return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        }

        return new Vec3((combined.minX + combined.maxX) * 0.5D, (combined.minY + combined.maxY) * 0.5D, (combined.minZ + combined.maxZ) * 0.5D);
    }

    private static void markDirtyAll(BreakAnimation animation) {
        for (BlockPos linkedPos : animation.linkedPositions) {
            markDirty(linkedPos);
        }
    }


    public static void clearAnimations() {
        for (BreakAnimation animation : ACTIVE_ANIMATIONS.values()) {
            for (BlockPos linkedPos : animation.linkedPositions) {
                BetterBlockEntitiesCompat.endAnimation(linkedPos);
            }
            markDirtyAll(animation);
        }

        for (BreakAnimation animation : RECOVERING_ANIMATIONS.values()) {
            for (BlockPos linkedPos : animation.linkedPositions) {
                BetterBlockEntitiesCompat.endAnimation(linkedPos);
            }
            markDirtyAll(animation);
        }

        ACTIVE_ANIMATIONS.clear();
        RECOVERING_ANIMATIONS.clear();
    }

    public static void setRenderingShrinkingBlock(boolean value) {
        RENDERING_SHRINKING_BLOCK.set(value);
    }

    public static boolean isRenderingShrinkingBlock() {
        return RENDERING_SHRINKING_BLOCK.get();
    }

    private static void markDirty(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        int sectionX = pos.getX() >> 4;
        int sectionY = pos.getY() >> 4;
        int sectionZ = pos.getZ() >> 4;

        minecraft.levelRenderer.setSectionDirtyWithNeighbors(sectionX, sectionY, sectionZ);
    }
}
