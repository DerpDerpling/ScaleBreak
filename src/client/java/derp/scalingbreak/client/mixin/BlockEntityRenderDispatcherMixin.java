package derp.scalingbreak.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import derp.scalingbreak.client.BlockBreakScaleController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

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

    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;" + "submit(" + "Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;" + "Lcom/mojang/blaze3d/vertex/PoseStack;" + "Lnet/minecraft/client/renderer/SubmitNodeCollector;" + "Lnet/minecraft/client/renderer/state/CameraRenderState;" + ")V"))
    private <S extends BlockEntityRenderState> void scalingbreak$scaleBlockEntity(BlockEntityRenderer<?, S> renderer, S renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, Operation<Void> original) {
        if (renderState instanceof ChestRenderState) {
            original.call(renderer, renderState, poseStack, submitNodeCollector, cameraRenderState);

            return;
        }

        BlockBreakScaleController.BreakAnimation animation = BlockBreakScaleController.getForPos(renderState.blockPos);

        if (animation == null) {
            original.call(renderer, renderState, poseStack, submitNodeCollector, cameraRenderState);

            return;
        }

        float scale = animation.scale();

        // Block entity renderers receive a pose stack whose origin is this block's
        // world position. Convert the animation's shared world-space pivot into
        // that local space so every linked block entity (notably both halves of
        // a bed) scales around the exact same point.
        Vec3 pivot = animation.pivot();
        double pivotX = pivot.x - renderState.blockPos.getX();
        double pivotY = pivot.y - renderState.blockPos.getY();
        double pivotZ = pivot.z - renderState.blockPos.getZ();

        poseStack.pushPose();

        poseStack.translate(pivotX, pivotY, pivotZ);

        poseStack.scale(scale, scale, scale);

        poseStack.translate(-pivotX, -pivotY, -pivotZ);

        ModelFeatureRenderer.CrumblingOverlay oldBreakProgress = renderState.breakProgress;

        if (oldBreakProgress != null) {
            renderState.breakProgress = new ModelFeatureRenderer.CrumblingOverlay(oldBreakProgress.progress(), poseStack.last().copy());
        }

        try {
            original.call(renderer, renderState, poseStack, submitNodeCollector, cameraRenderState);
        } finally {
            renderState.breakProgress = oldBreakProgress;

            poseStack.popPose();
        }
    }
}