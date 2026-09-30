package derp.scalingbreak.client.mixin.compat.sodium;

import derp.scalingbreak.client.BlockBreakScaleController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext", remap = false)
public abstract class SodiumBlockRenderContextMixin {

    @Shadow
    protected BlockPos pos;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, remap = false)
    private void scalingbreak$showFaceTowardBreakingBlock(Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (pos == null || facing == null) {
            return;
        }

        BlockPos neighborPos = pos.relative(facing);

        if (BlockBreakScaleController.shouldHideOriginal(neighborPos)) {
            cir.setReturnValue(true);
        }
    }
}