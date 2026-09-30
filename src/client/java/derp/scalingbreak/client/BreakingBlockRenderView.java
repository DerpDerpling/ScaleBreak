package derp.scalingbreak.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class BreakingBlockRenderView implements BlockAndTintGetter {

    private final ClientLevel level;
    private final BlockPos hiddenPos;

    public BreakingBlockRenderView(ClientLevel level, BlockPos hiddenPos) {
        this.level = level;
        this.hiddenPos = hiddenPos.immutable();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (pos.equals(hiddenPos)) {
            return Blocks.AIR.defaultBlockState();
        }

        return level.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        if (pos.equals(hiddenPos)) {
            return Blocks.AIR.defaultBlockState().getFluidState();
        }

        return level.getFluidState(pos);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        if (pos.equals(hiddenPos)) {
            return null;
        }

        return level.getBlockEntity(pos);
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return level.getBlockTint(pos, resolver);
    }

    @Override
    public float getShade(Direction direction, boolean shaded) {
        float vanilla = level.getShade(direction, shaded);

        float scale = BlockBreakScaleController.scale(hiddenPos);
        float openness = Math.clamp(1.0F - scale, 0.0F, 1.0F);

        float strength = openness * 0.9F;

        return vanilla + (1.0F - vanilla) * strength;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinY() {
        return level.getMinY();
    }
}
