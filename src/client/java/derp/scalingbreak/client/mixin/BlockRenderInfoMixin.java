package derp.scalingbreak.client.mixin;

import derp.scalingbreak.client.BlockBreakScaleController;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("UnstableApiUsage")
@Mixin(AltModelBlockRendererImpl.class)
public abstract class BlockRenderInfoMixin {

    @Redirect(method = "shouldCullFace", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;" + "getBlockState(Lnet/minecraft/core/BlockPos;)" + "Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState scalingbreak$treatBreakingNeighborAsAir(BlockAndTintGetter level, BlockPos pos) {
        if (BlockBreakScaleController.shouldHideOriginal(pos)) {
            return Blocks.AIR.defaultBlockState();
        }

        return level.getBlockState(pos);
    }
}