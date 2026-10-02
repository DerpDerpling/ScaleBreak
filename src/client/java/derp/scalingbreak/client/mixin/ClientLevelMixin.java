package derp.scalingbreak.client.mixin;

import derp.scalingbreak.client.BlockBreakScaleController;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "destroyBlockProgress", at = @At("HEAD"))
    private void scalingbreak$trackBreak(int id, BlockPos pos, int progress, CallbackInfo ci) {
        BlockBreakScaleController.set(id, pos, progress);
    }
}