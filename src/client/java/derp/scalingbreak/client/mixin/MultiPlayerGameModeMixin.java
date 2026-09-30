package derp.scalingbreak.client.mixin;

import derp.scalingbreak.client.BlockBreakScaleController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    //This class fixes a bug that causes the animations to get in a stuck state when rolling over them with the mine button held down

    @Shadow
    private boolean isDestroying;

    @Shadow
    private BlockPos destroyBlockPos;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void scalingbreak$cancelOldAnimationOnTargetChange(BlockPos newPos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!isDestroying) {
            return;
        }

        if (destroyBlockPos.equals(newPos)) {
            return;
        }

        if (minecraft.player == null) {
            return;
        }

        BlockBreakScaleController.set(minecraft.player.getId(), destroyBlockPos, -1);
    }
}