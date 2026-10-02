package derp.scalingbreak.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import derp.scalingbreak.client.BlockBreakScaleController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Unique
    private static Vec3 scalingbreak$getShapeCenter(BlockState state, BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return new Vec3(0.5D, 0.5D, 0.5D);
        }

        VoxelShape shape = state.getShape(minecraft.level, pos);

        if (shape.isEmpty()) {
            return new Vec3(0.5D, 0.5D, 0.5D);
        }

        AABB bounds = shape.bounds();

        return new Vec3((bounds.minX + bounds.maxX) * 0.5D, (bounds.minY + bounds.maxY) * 0.5D, (bounds.minZ + bounds.maxZ) * 0.5D);
    }

    @WrapMethod(method = "submit")
    private <S extends BlockEntityRenderState> void scalingbreak$scaleBlockEntity(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, Operation<Void> original) {
        if (state instanceof ChestRenderState) {
            original.call(state, poseStack, submitNodeCollector, camera);
            return;
        }

        BlockBreakScaleController.BreakAnimation animation = BlockBreakScaleController.getForPos(state.blockPos);

        if (animation == null) {
            original.call(state, poseStack, submitNodeCollector, camera);
            return;
        }

        float scale = animation.scale();

        Vec3 pivot = animation.pivot();
        double pivotX = pivot.x - state.blockPos.getX();
        double pivotY = pivot.y - state.blockPos.getY();
        double pivotZ = pivot.z - state.blockPos.getZ();

        poseStack.pushPose();

        poseStack.translate(pivotX, pivotY, pivotZ);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-pivotX, -pivotY, -pivotZ);

        ModelFeatureRenderer.CrumblingOverlay oldBreakProgress = state.breakProgress;

        if (oldBreakProgress != null) {
            state.breakProgress = new ModelFeatureRenderer.CrumblingOverlay(oldBreakProgress.progress(), poseStack.last().copy());
        }

        try {
            original.call(state, poseStack, submitNodeCollector, camera);
        } finally {
            state.breakProgress = oldBreakProgress;
            poseStack.popPose();
        }
    }
}