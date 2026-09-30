package derp.scalingbreak.client.mixin.compat.bbe;

import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import betterblockentities.client.tasks.ManagerTasks;
import derp.scalingbreak.client.compat.BetterBlockEntitiesCompat;

import derp.scalingbreak.client.compat.ScaleBreakBBEManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = InstancedBlockEntityManager.class, remap = false)
public abstract class InstancedBlockEntityManagerMixin implements ScaleBreakBBEManager {

    @Unique
    private boolean scalingbreak$breaking;


    @Inject(method = "<init>", at = @At("TAIL"))
    private void scalingbreak$register(BlockEntity blockEntity, CallbackInfo ci) {
        BetterBlockEntitiesCompat.registerManager(blockEntity, (InstancedBlockEntityManager) (Object) this);
    }


    @Inject(method = "shouldBeImmediate", at = @At("HEAD"), cancellable = true)
    private void scalingbreak$forceImmediateWhileBreaking(CallbackInfoReturnable<Boolean> cir) {
        if (scalingbreak$breaking) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public void scalingbreak$setBreaking(boolean breaking) {
        if (scalingbreak$breaking == breaking) {
            return;
        }
        scalingbreak$breaking = breaking;

        ManagerTasks.schedule((InstancedBlockEntityManager) (Object) this);
    }
}