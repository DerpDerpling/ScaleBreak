package derp.scalingbreak.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import derp.scalingbreak.client.BlockBreakScaleController;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin {

    @Shadow
    @Final
    private MaterialSet materials;

    @Shadow
    @Final
    private ChestModel singleModel;

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void scalingbreak$renderShrinkingChest(ChestRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        BlockBreakScaleController.BreakAnimation thisAnimation = BlockBreakScaleController.getForPos(state.blockPos);

        if (state.type == ChestType.SINGLE) {
            if (thisAnimation == null) {
                return;
            }

            ci.cancel();

            scalingbreak$renderSingle(state, poseStack, collector, thisAnimation);

            return;
        }

        //render double chests as single chests
        BlockPos partnerPos = scalingbreak$getPartnerPos(state.blockPos, state.blockState);

        if (partnerPos == null) {
            return;
        }

        BlockBreakScaleController.BreakAnimation partnerAnimation = BlockBreakScaleController.getForPos(partnerPos);

        if (thisAnimation == null && partnerAnimation == null) {
            return;
        }

        ci.cancel();


        scalingbreak$renderSingle(state, poseStack, collector, thisAnimation);
    }

    @Unique
    private void scalingbreak$renderSingle(ChestRenderState state, PoseStack poseStack, SubmitNodeCollector collector, @Nullable BlockBreakScaleController.BreakAnimation animation) {
        poseStack.pushPose();

        if (animation != null) {
            float scale = animation.scale();

            poseStack.translate(0.5F, 0.5F, 0.5F);

            poseStack.scale(scale, scale, scale);

            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }

        poseStack.translate(0.5F, 0.5F, 0.5F);

        poseStack.mulPose(Axis.YP.rotationDegrees(-state.angle));

        poseStack.translate(-0.5F, -0.5F, -0.5F);

        float open = state.open;

        open = 1.0F - open;

        open = 1.0F - open * open * open;


        Material material = Sheets.chooseMaterial(state.material, ChestType.SINGLE);

        RenderType renderType = material.renderType(RenderTypes::entityCutout);

        TextureAtlasSprite sprite = materials.get(material);

        ModelFeatureRenderer.CrumblingOverlay breakProgress = state.breakProgress;

        if (animation != null && breakProgress != null) {
            breakProgress = new ModelFeatureRenderer.CrumblingOverlay(breakProgress.progress(), poseStack.last().copy());
        }

        collector.submitModel(singleModel, open, poseStack, renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, sprite, 0, breakProgress);

        poseStack.popPose();
    }

    @Unique
    private static @Nullable BlockPos scalingbreak$getPartnerPos(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ChestBlock)) {
            return null;
        }

        if (!state.hasProperty(ChestBlock.TYPE) || !state.hasProperty(ChestBlock.FACING)) {
            return null;
        }

        ChestType type = state.getValue(ChestBlock.TYPE);

        if (type == ChestType.SINGLE) {
            return null;
        }

        Direction facing = state.getValue(ChestBlock.FACING);

        Direction towardPartner = type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();

        BlockPos partnerPos = pos.relative(towardPartner);

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return null;
        }

        BlockState partnerState = minecraft.level.getBlockState(partnerPos);

        if (partnerState.getBlock() != state.getBlock()) {
            return null;
        }

        if (!partnerState.hasProperty(ChestBlock.TYPE) || !partnerState.hasProperty(ChestBlock.FACING)) {
            return null;
        }

        ChestType expectedType = type == ChestType.LEFT ? ChestType.RIGHT : ChestType.LEFT;

        if (partnerState.getValue(ChestBlock.TYPE) != expectedType) {
            return null;
        }

        if (partnerState.getValue(ChestBlock.FACING) != facing) {
            return null;
        }

        return partnerPos;
    }
}
