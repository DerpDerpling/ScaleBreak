package derp.scalingbreak.client.mixin.compat.sodium;

import derp.scalingbreak.client.BlockBreakScaleController;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline", remap = false)
public abstract class SodiumSmoothLightPipelineMixin {

    @Shadow
    private long cachedPos;

    @Inject(method = "calculate", at = @At("HEAD"), remap = false)
    private void scalingbreak$invalidateLightingCache(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, Direction shadeDirectionOverride, boolean enhanced, CallbackInfo ci) {
        if (BlockBreakScaleController.isRenderingShrinkingBlock()) {
            cachedPos = Long.MIN_VALUE;
        }
    }
}