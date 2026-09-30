package derp.scalingbreak.client.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import derp.scalingbreak.client.BlockBreakScaleController;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask", remap = false)
public abstract class SodiumChunkBuilderMeshingTaskMixin {

    @WrapWithCondition(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;" + "renderModel(" + "Lnet/minecraft/client/renderer/block/model/BlockStateModel;" + "Lnet/minecraft/world/level/block/state/BlockState;" + "Lnet/minecraft/core/BlockPos;" + "Lnet/minecraft/core/BlockPos;" + ")V", remap = false))
    private boolean scalingbreak$hideBreakingModel(BlockRenderer instance, BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin) {
        return !BlockBreakScaleController.shouldHideOriginal(pos);
    }
}