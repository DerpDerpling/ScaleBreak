package derp.scalingbreak.client.mixin.compat.bbe;

import betterblockentities.client.BBE;
import betterblockentities.client.render.immediate.blockentity.renderers.BBEAbstractSignRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import derp.scalingbreak.client.BlockBreakScaleController;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(value = BBEAbstractSignRenderer.class, remap = false)
public abstract class BBEAbstractSignRendererMixin {

    @WrapMethod(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V")
    private void scalingbreak$renderFullSignWhileAnimated(SignRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, Operation<Void> original) {
        if (BlockBreakScaleController.getForPos(state.blockPos) == null) {
            original.call(state, poseStack, submitNodeCollector, cameraRenderState);
            return;
        }

        boolean old = BBE.GlobalScope.limitVanillaSignRendering;

        BBE.GlobalScope.limitVanillaSignRendering = false;

        try {
            original.call(state, poseStack, submitNodeCollector, cameraRenderState);
        } finally {
            BBE.GlobalScope.limitVanillaSignRendering = old;
        }
    }
}