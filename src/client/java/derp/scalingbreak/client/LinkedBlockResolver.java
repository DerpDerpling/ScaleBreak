package derp.scalingbreak.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;


public final class LinkedBlockResolver {

    private static final int MAX_LINKED_BLOCKS = 64;

    private LinkedBlockResolver() {
    }

    public static Set<BlockPos> resolve(ClientLevel level, BlockPos origin) {
        Set<BlockPos> result = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        BlockPos immutableOrigin = origin.immutable();
        result.add(immutableOrigin);
        queue.add(immutableOrigin);

        while (!queue.isEmpty() && result.size() < MAX_LINKED_BLOCKS) {
            BlockPos pos = queue.removeFirst();
            BlockState state = level.getBlockState(pos);

            for (BlockPos candidate : findCandidates(pos, state)) {
                if (result.size() >= MAX_LINKED_BLOCKS) {
                    break;
                }

                BlockPos immutableCandidate = candidate.immutable();

                if (result.contains(immutableCandidate)) {
                    continue;
                }

                if (!isActuallyLinked(level, pos, state, immutableCandidate)) {
                    continue;
                }

                result.add(immutableCandidate);
                queue.add(immutableCandidate);
            }
        }

        return Set.copyOf(result);
    }

    private static Set<BlockPos> findCandidates(BlockPos pos, BlockState state) {
        Set<BlockPos> candidates = new LinkedHashSet<>();

        if (state.getBlock() instanceof DoorBlock && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            candidates.add(half == DoubleBlockHalf.LOWER ? pos.above() : pos.below());
        }

        if (state.getBlock() instanceof BedBlock && state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            BedPart part = state.getValue(BlockStateProperties.BED_PART);
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

            candidates.add(part == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite()));
        }

        if (state.getBlock() instanceof PistonBaseBlock && state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED) && state.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            candidates.add(pos.relative(facing));
        }

        if (state.getBlock() instanceof PistonHeadBlock && state.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            candidates.add(pos.relative(facing.getOpposite()));
        }

        return candidates;
    }

    private static boolean isActuallyLinked(ClientLevel level, BlockPos fromPos, BlockState fromState, BlockPos candidatePos) {
        BlockState candidateState = level.getBlockState(candidatePos);

        if (candidateState.isAir()) {
            return false;
        }

        if (fromState.getBlock() instanceof DoorBlock && candidateState.getBlock() == fromState.getBlock() && fromState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && candidateState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf fromHalf = fromState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            DoubleBlockHalf candidateHalf = candidateState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);

            if (fromHalf == candidateHalf) {
                return false;
            }

            if (fromState.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && candidateState.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && fromState.getValue(BlockStateProperties.HORIZONTAL_FACING) != candidateState.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                return false;
            }

            return fromPos.getX() == candidatePos.getX() && fromPos.getZ() == candidatePos.getZ() && Math.abs(fromPos.getY() - candidatePos.getY()) == 1;
        }

        if (fromState.getBlock() instanceof BedBlock && candidateState.getBlock() == fromState.getBlock() && fromState.hasProperty(BlockStateProperties.BED_PART) && candidateState.hasProperty(BlockStateProperties.BED_PART) && fromState.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && candidateState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            BedPart fromPart = fromState.getValue(BlockStateProperties.BED_PART);
            BedPart candidatePart = candidateState.getValue(BlockStateProperties.BED_PART);

            return fromPart != candidatePart && fromState.getValue(BlockStateProperties.HORIZONTAL_FACING) == candidateState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        if (fromState.getBlock() instanceof PistonBaseBlock && candidateState.getBlock() instanceof PistonHeadBlock) {
            return pistonDirectionsMatch(fromState, candidateState);
        }

        if (fromState.getBlock() instanceof PistonHeadBlock && candidateState.getBlock() instanceof PistonBaseBlock) {
            return candidateState.hasProperty(BlockStateProperties.EXTENDED) && candidateState.getValue(BlockStateProperties.EXTENDED) && pistonDirectionsMatch(fromState, candidateState);
        }

        return false;
    }

    private static boolean pistonDirectionsMatch(BlockState first, BlockState second) {
        return first.hasProperty(BlockStateProperties.FACING) && second.hasProperty(BlockStateProperties.FACING) && first.getValue(BlockStateProperties.FACING) == second.getValue(BlockStateProperties.FACING);
    }
}
