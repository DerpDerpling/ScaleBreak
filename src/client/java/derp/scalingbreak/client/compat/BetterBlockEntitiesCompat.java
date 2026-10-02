package derp.scalingbreak.client.compat;

import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.Map;
import java.util.WeakHashMap;

public final class BetterBlockEntitiesCompat {

    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("betterblockentities");

    private static final Map<BlockEntity, InstancedBlockEntityManager> MANAGERS = new WeakHashMap<>();

    private BetterBlockEntitiesCompat() {
    }

    public static void registerManager(BlockEntity blockEntity, InstancedBlockEntityManager manager) {
        MANAGERS.put(blockEntity, manager);
    }

    public static void beginAnimation(BlockPos pos) {
        if (!LOADED) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);

        if (blockEntity == null) {
            return;
        }

        setBreaking(blockEntity, true);

        BlockState state = minecraft.level.getBlockState(pos);

        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE) && state.hasProperty(ChestBlock.FACING)) {

            ChestType type = state.getValue(ChestBlock.TYPE);

            if (type != ChestType.SINGLE) {
                Direction facing = state.getValue(ChestBlock.FACING);

                Direction towardPartner = type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();

                BlockPos partnerPos = pos.relative(towardPartner);

                BlockEntity partner = minecraft.level.getBlockEntity(partnerPos);

                if (partner != null) {
                    setBreaking(partner, true);
                }

                SectionUpdateDispatcher.queueRebuildAtBlockPos(pos);
                SectionUpdateDispatcher.queueRebuildAtBlockPos(partnerPos);
            }
        }
    }

    public static void endAnimation(BlockPos pos) {
        if (!LOADED) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);

        if (blockEntity != null) {
            setBreaking(blockEntity, false);
        }

        BlockState state = minecraft.level.getBlockState(pos);

        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE) && state.hasProperty(ChestBlock.FACING)) {

            ChestType type = state.getValue(ChestBlock.TYPE);

            if (type != ChestType.SINGLE) {
                Direction facing = state.getValue(ChestBlock.FACING);

                Direction towardPartner = type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();

                BlockPos partnerPos = pos.relative(towardPartner);

                BlockEntity partner = minecraft.level.getBlockEntity(partnerPos);

                if (partner != null) {
                    setBreaking(partner, false);
                }

                SectionUpdateDispatcher.queueRebuildAtBlockPos(pos);
                SectionUpdateDispatcher.queueRebuildAtBlockPos(partnerPos);
            }
        }
    }

    private static void setBreaking(BlockEntity blockEntity, boolean breaking) {
        if (!(blockEntity instanceof BlockEntityExt ext)) {
            return;
        }

        if (!ext.bbe$isSupportedBlockEntity()) {
            return;
        }

        InstancedBlockEntityManager manager = MANAGERS.get(blockEntity);

        if (manager == null) {
            manager = new InstancedBlockEntityManager(blockEntity);

            MANAGERS.put(blockEntity, manager);
        }

        ((ScaleBreakBBEManager) (Object) manager).scalingbreak$setBreaking(breaking);
    }
}
