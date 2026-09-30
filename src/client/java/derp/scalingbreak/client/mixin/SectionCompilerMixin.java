package derp.scalingbreak.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import derp.scalingbreak.client.BlockBreakScaleController;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {


    //Hide the original block by pretending it is air, but preserve the real state for block entities so their BER can continue to exist.
    @Redirect(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState scalingbreak$hideNormalBreakingBlock(RenderSectionRegion region, BlockPos pos) {
        BlockState state = region.getBlockState(pos);

        if (BlockBreakScaleController.shouldHideOriginal(pos) && !state.hasBlockEntity()) {
            return Blocks.AIR.defaultBlockState();
        }

        return state;
    }


    @WrapOperation(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLjava/util/List;)V"))
    private void scalingbreak$hideBlockEntityBakedModel(BlockRenderDispatcher dispatcher, BlockState state, BlockPos pos, BlockAndTintGetter blockView, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, List<BlockModelPart> parts, Operation<Void> original) {
        if (BlockBreakScaleController.shouldHideOriginal(pos) && state.hasBlockEntity()) {
            return;
        }

        original.call(dispatcher, state, pos, blockView, poseStack, consumer, checkSides, parts);
    }
}