package derp.scalingbreak.client.mixin;

import derp.scalingbreak.client.BlockBreakScaleController;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRenderInfo.class)
public abstract class BlockRenderInfoMixin {

    @Shadow
    public BlockPos blockPos;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void scalingbreak$showFaceTowardBreakingBlock(Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (side == null || blockPos == null) {
            return;
        }

        BlockPos neighborPos = blockPos.relative(side);

        if (BlockBreakScaleController.shouldHideOriginal(neighborPos)) {
            cir.setReturnValue(true);
        }
    }
}