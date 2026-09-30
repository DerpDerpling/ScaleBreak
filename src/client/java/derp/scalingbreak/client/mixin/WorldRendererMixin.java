package derp.scalingbreak.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import derp.scalingbreak.client.BlockBreakScaleController;
import derp.scalingbreak.client.BreakingBlockRenderView;
import net.fabricmc.fabric.api.renderer.v1.render.BlockVertexConsumerProvider;
import net.fabricmc.fabric.api.renderer.v1.render.FabricBlockModelRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererMixin {
	@Mutable
    @Final
    @Shadow
	private final Minecraft minecraft;

    protected WorldRendererMixin(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Inject(method = "destroyBlockProgress", at = @At("HEAD"))
	private void immersive$trackBreak(int entityId, BlockPos pos, int stage, CallbackInfo ci) {
		BlockBreakScaleController.set(entityId, pos, stage);
	}

    @Inject(method = "renderBlockDestroyAnimation", at = @At("HEAD"))
	private void immersive$renderShrinkingBlocks(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LevelRenderState levelRenderState, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.level == null) {
			return;
		}

		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.position();

		for (BlockBreakScaleController.BreakAnimation animation : BlockBreakScaleController.renderAnimationGroups()) {
			scalingbreak$renderShrinkingGroup(animation, poseStack, bufferSource, cameraPos);
		}
	}

	@Unique
	private void scalingbreak$renderShrinkingGroup(BlockBreakScaleController.BreakAnimation animation, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 cameraPos) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.level == null) {
			return;
		}

		Vec3 pivot = animation.pivot();
		float scale = animation.scale();


		poseStack.pushPose();
		poseStack.translate(pivot.x - cameraPos.x, pivot.y - cameraPos.y, pivot.z - cameraPos.z);
		poseStack.scale(scale, scale, scale);

		for (BlockPos pos : animation.linkedPositions()) {
			BlockState state = minecraft.level.getBlockState(pos);

			if (state.isAir()) {
				continue;
			}

			poseStack.pushPose();
			poseStack.translate(pos.getX() - pivot.x, pos.getY() - pivot.y, pos.getZ() - pivot.z);

			scalingbreak$renderShrinkingBlockModel(pos, state, poseStack, bufferSource);

			poseStack.popPose();
		}

		poseStack.popPose();
	}

	@Unique
	private void scalingbreak$renderShrinkingBlockModel(BlockPos pos, BlockState state, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.level == null) {
			return;
		}

		BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
		BlockStateModel model = dispatcher.getBlockModel(state);

		BreakingBlockRenderView renderView = new BreakingBlockRenderView(minecraft.level, pos);
		BlockVertexConsumerProvider vertexConsumers = layer -> bufferSource.getBuffer(scalingbreak$getMovingRenderType(layer));

		BlockBreakScaleController.setRenderingShrinkingBlock(true);
		FabricBlockModelRenderer fabricRenderer = dispatcher.getModelRenderer();

		try {
			fabricRenderer.render(renderView, model, state, pos, poseStack, vertexConsumers, false, state.getSeed(pos), OverlayTexture.NO_OVERLAY);
		} finally {
			BlockBreakScaleController.setRenderingShrinkingBlock(false);
		}
	}

	@Unique
	private static RenderType scalingbreak$getMovingRenderType(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> RenderTypes.solidMovingBlock();

			case CUTOUT -> RenderTypes.cutoutMovingBlock();

			case TRANSLUCENT -> RenderTypes.translucentMovingBlock();

			case TRIPWIRE -> RenderTypes.tripwireMovingBlock();
		};
	}

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void scalingbreak$updateAnimations(CallbackInfo ci) {
		float deltaTicks = minecraft.getDeltaTracker().getGameTimeDeltaTicks();
		BlockBreakScaleController.update(deltaTicks);
	}

	@Inject(method = "renderBlockDestroyAnimation", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;last()Lcom/mojang/blaze3d/vertex/PoseStack$Pose;" ) )
	private void immersive$scaleBreakingTexture(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LevelRenderState levelRenderState, CallbackInfo ci, @Local BlockPos blockPos) {
		BlockBreakScaleController.BreakAnimation animation = BlockBreakScaleController.getForPos(blockPos);
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.level == null) {
			return;
		}

		if (animation == null) {
			return;
		}

		float scale = animation.scale();
        Vec3 pivot = animation.pivot();
        double pivotX = pivot.x - blockPos.getX();
        double pivotY = pivot.y - blockPos.getY();
        double pivotZ = pivot.z - blockPos.getZ();

        poseStack.translate(pivotX, pivotY, pivotZ);

		poseStack.scale(scale, scale, scale);

        poseStack.translate(-pivotX, -pivotY, -pivotZ);
	}
}