package me.trueprotocol.end_remastered_dimension_fix;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.block.BlockStatePredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.function.Predicate;

public class End_remastered_dimension_fix implements ModInitializer {

    private static final Predicate<Object> hasEyePredicate = (property) ->  property.equals(true);

    private static final BlockPattern portalPattern = BlockPatternBuilder.start()
            .aisle("?vvv?", ">???<", ">???<", ">???<", "?^^^?")
            .where('?', block -> true)
            .where('v', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME)
                    .with(Properties.HORIZONTAL_FACING, dir -> dir == Direction.SOUTH)
                    .with(EndPortalFrameBlock.EYE, hasEyePredicate)))
            .where('^', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME)
                    .with(Properties.HORIZONTAL_FACING, dir -> dir == Direction.NORTH)
                    .with(EndPortalFrameBlock.EYE, hasEyePredicate)))
            .where('>', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME)
                    .with(Properties.HORIZONTAL_FACING, dir -> dir == Direction.WEST)
                    .with(EndPortalFrameBlock.EYE, hasEyePredicate)))
            .where('<', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME)
                    .with(Properties.HORIZONTAL_FACING, dir -> dir == Direction.EAST)
                    .with(EndPortalFrameBlock.EYE, hasEyePredicate)))
            .build();

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;

            ItemStack itemStack = player.getStackInHand(hand);
            if (!(itemStack.getItem() == Items.ENDER_EYE)) { return ActionResult.PASS; }

            BlockPos pos = hitResult.getBlockPos();
            BlockState blockState = world.getBlockState(pos);
            if (!(blockState.getBlock() == Blocks.END_PORTAL_FRAME)) { return ActionResult.PASS; }
            if (blockState.get(EndPortalFrameBlock.EYE)) { return ActionResult.PASS; }

            RegistryKey<World> dimension = world.getRegistryKey();
            if (!(dimension == World.END)) { return ActionResult.PASS; }

            world.setBlockState(pos, blockState.with(EndPortalFrameBlock.EYE, true));
            Block.pushEntitiesUpBeforeBlockChange(blockState, blockState.with(EndPortalFrameBlock.EYE, true), world, pos);
            if (!player.isInCreativeMode()) { itemStack.decrement(1); }
            world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);

            BlockPattern.Result result = null;

            if (blockState.get(Properties.HORIZONTAL_FACING) == Direction.SOUTH) {
                result = portalPattern.searchAround(world, pos);

            } else if (blockState.get(Properties.HORIZONTAL_FACING) == Direction.NORTH) {
                result = portalPattern.searchAround(world, pos.add(0, 0, -4));

            } else if (blockState.get(Properties.HORIZONTAL_FACING) == Direction.WEST) {
                for (int z = 1; z <= 3; z++) {
                    if (world.getBlockState(pos.add(0, 0, -z)).getBlock() == Blocks.END_PORTAL_FRAME) {
                        continue;
                    }
                    result = portalPattern.searchAround(world, pos.add(0, 0, -z));
                }

            } else if (blockState.get(Properties.HORIZONTAL_FACING) == Direction.EAST) {
                for (int z = 1; z <= 3; z++) {
                    if (world.getBlockState(pos.add(0, 0, -z)).getBlock() == Blocks.END_PORTAL_FRAME) {
                        continue;
                    }
                    result = portalPattern.searchAround(world, pos.add(0, 0, -z));
                }
            }

            if (result != null) {
                BlockPos center = getCenter(result);
                activatePortal(world, center);
                world.playSound(null, center, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 1.0f, 1.0f);
            }
            return ActionResult.SUCCESS;
        });
    }

    private void activatePortal(World world, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos portalPos = pos.add(x, 0, z);
                world.setBlockState(portalPos, Blocks.END_PORTAL.getDefaultState());
            }
        }
    }

    private BlockPos getCenter(BlockPattern.Result result) {
        BlockPos frontTopLeft = result.getFrontTopLeft();

        return new BlockPos(frontTopLeft.getX() - 2, frontTopLeft.getY(), frontTopLeft.getZ() + 2);
    }
}
