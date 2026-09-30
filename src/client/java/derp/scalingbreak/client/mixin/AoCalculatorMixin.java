package derp.scalingbreak.client.mixin;

import derp.scalingbreak.client.BlockBreakScaleController;

import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.QuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AoCalculator.class)
public abstract class AoCalculatorMixin {

    @Shadow
    @Final
    private BlockRenderInfo blockInfo;

    @Shadow
    @Final
    public float[] ao;

    @Inject(method = "compute", at = @At("TAIL"), remap = false)
    private void scalingbreak$fixBreakingLighting(QuadViewImpl quad, boolean vanillaShade, CallbackInfo ci) {

        Direction face = quad.lightFace();

        BlockPos breakingPos = blockInfo.blockPos.relative(face);

        BlockBreakScaleController.BreakAnimation animation = BlockBreakScaleController.getForPos(breakingPos);

        if (animation == null) {
            return;
        }

        float scale = animation.scale();
        float openness = Mth.clamp(1.0F - scale, 0.0F, 1.0F);

        float faceShade = blockInfo.blockView.getShade(face, vanillaShade);

        float strength = (float) Mth.smoothstep(openness);

        for (int i = 0; i < 4; i++) {
            ao[i] = Mth.lerp(strength, ao[i], faceShade);
        }
    }
}
