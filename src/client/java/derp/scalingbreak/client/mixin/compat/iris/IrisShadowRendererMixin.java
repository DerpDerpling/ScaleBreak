package derp.scalingbreak.client.mixin.compat.iris;

import com.mojang.blaze3d.vertex.PoseStack;
import derp.scalingbreak.client.BlockBreakScaleController;
import derp.scalingbreak.client.BreakingBlockRenderView;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.shadows.ShadowRenderer", remap = false)
public abstract class IrisShadowRendererMixin {

    @Shadow
    @Final
    private RenderBuffers buffers;


    @Inject(method = "renderShadows", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;" + "renderGroup(" + "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;" + "Lcom/mojang/blaze3d/textures/GpuSampler;" + ")V", shift = At.Shift.AFTER))
    private void scalingbreak$renderShrinkingBlockShadows(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();

        Vec3 cameraPos = camera.position();

        PoseStack poseStack = new PoseStack();

        MultiBufferSource.BufferSource bufferSource = buffers.bufferSource();

        boolean renderedAnything = false;

        for (BlockBreakScaleController.RenderPart part : BlockBreakScaleController.renderAnimations()) {

            BlockPos pos = part.pos();
            BlockBreakScaleController.BreakAnimation animation = part.animation();

            BlockState state = minecraft.level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            if (state.hasBlockEntity()) {
                continue;
            }

            scalingbreak$renderShadowBlock(pos, animation, state, poseStack, bufferSource, cameraPos);

            renderedAnything = true;
        }

        if (renderedAnything) {
            bufferSource.endBatch();
        }
    }

    @Unique
    private static void scalingbreak$renderShadowBlock(BlockPos pos, BlockBreakScaleController.BreakAnimation animation, BlockState state, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 cameraPos) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        float scale = animation.scale();

        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();

        RandomSource random = RandomSource.create();

        List<BlockModelPart> parts = new ObjectArrayList<>();

        random.setSeed(state.getSeed(pos));

        dispatcher.getBlockModel(state).collectParts(random, parts);

        ChunkSectionLayer layer = ItemBlockRenderTypes.getChunkRenderType(state);

        RenderType renderType = switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();

            case CUTOUT -> RenderTypes.cutoutMovingBlock();

            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();

            case TRIPWIRE -> RenderTypes.tripwireMovingBlock();
        };

        BreakingBlockRenderView renderView = new BreakingBlockRenderView(minecraft.level, pos);

        poseStack.pushPose();

        poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

        Vec3 pivot = animation.pivot();
        double pivotX = pivot.x - pos.getX();
        double pivotY = pivot.y - pos.getY();
        double pivotZ = pivot.z - pos.getZ();

        poseStack.translate(pivotX, pivotY, pivotZ);

        poseStack.scale(scale, scale, scale);

        poseStack.translate(-pivotX, -pivotY, -pivotZ);

        try {
            dispatcher.renderBatched(state, pos, renderView, poseStack, bufferSource.getBuffer(renderType), false, parts);
        } finally {
            poseStack.popPose();
        }
    }
}