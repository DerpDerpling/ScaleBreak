package derp.scalingbreak.client.mixin.compat.bbe;

import betterblockentities.client.chunk.pipeline.BBEBlockRenderer;
import derp.scalingbreak.client.BlockBreakScaleController;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Pseudo
@Mixin(value = BBEBlockRenderer.class, remap = false)
public abstract class BBEBlockRendererMixin {

    @Inject(method = "emitBlockModel", at = @At("HEAD"), cancellable = true)
    private void scalingbreak$skipBbeTerrainForAnimatedBlock(PlatformModelEmitter sodiumPlatformEmitter, BlockStateModel model, Predicate<Direction> isFaceCulled, MutableQuadViewImpl sodiumEmitter, RandomSource random, BlockAndTintGetter level, LevelSlice slice, BlockPos pos, BlockState state, PlatformModelEmitter.Bufferer bufferer, CallbackInfo ci) {
        if (BlockBreakScaleController.shouldHideOriginal(pos)) {
            ci.cancel();
        }
    }
}