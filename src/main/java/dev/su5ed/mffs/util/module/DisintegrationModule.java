package dev.su5ed.mffs.util.module;

import dev.su5ed.mffs.api.Projector;
import dev.su5ed.mffs.api.module.ModuleType;
import dev.su5ed.mffs.blockentity.ProjectorBlockEntity;
import dev.su5ed.mffs.network.DrawHologramPacket;
import dev.su5ed.mffs.network.Network;
import dev.su5ed.mffs.setup.ModModules;
import dev.su5ed.mffs.util.ModUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

public class DisintegrationModule extends BaseModule {
    private final List<BlockPos> activeBlocks = new ArrayList<>();

    public DisintegrationModule(ModuleType<?> type, ItemStack stack) {
        super(type, stack);
    }

    @Override
    public ProjectAction onSelect(Projector projector, BlockPos pos) {
        if (!this.activeBlocks.contains(pos)) {
            World world = projector.be().getWorld();
            IBlockState state = world.getBlockState(pos);
            if (!state.getBlock().isAir(state, world, pos)) {
                Block block = state.getBlock();
                if (projector.hasModule(ModModules.CAMOUFLAGE)) {
                    Item blockItem = Item.getItemFromBlock(block);
                    if (projector.getAllModuleItemsStream()
                        .noneMatch(s -> ProjectorBlockEntity.getFilterBlock(s).isPresent() && s.getItem() == blockItem)) {
                        return ProjectAction.SKIP;
                    }
                }
                if (!ModUtil.isLiquidBlock(block) && state.getBlockHardness(world, pos) != -1) {
                    if (this.activeBlocks.size() - 1 >= projector.getModuleCount(ModModules.SPEED) / 3) {
                        return ProjectAction.INTERRUPT;
                    }
                    this.activeBlocks.add(pos);
                    return ProjectAction.PROJECT;
                }
            }
        }
        return ProjectAction.SKIP;
    }

    @Override
    public ProjectAction onProject(Projector projector, BlockPos position) {
        World world = projector.be().getWorld();
        IBlockState state = world.getBlockState(position);
        Vec3d pos = new Vec3d(projector.be().getPos());
        Vec3d target = new Vec3d(position);
        Network.sendToAllAround(
            new DrawHologramPacket(pos, target, DrawHologramPacket.HoloType.DESTROY),
            world, position, 64);

        Block block = state.getBlock();
        projector.schedule(39, () -> {
            if (projector.hasModule(ModModules.COLLECTION)) {
                collectBlock(projector, world, position, block);
            } else {
                destroyBlock(world, position, block);
            }
            this.activeBlocks.remove(position);
        });
        return ProjectAction.SKIP;
    }

    private static void destroyBlock(World world, BlockPos pos, Block block) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == block) {
            block.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    private static void collectBlock(Projector projector, World world, BlockPos pos, Block block) {
        IBlockState state = world.getBlockState(pos);
        if (world instanceof WorldServer && state.getBlock() == block) {
            List<ItemStack> drops = block.getDrops(world, pos, state, 0);
            for (ItemStack drop : drops) {
                projector.mergeIntoInventory(drop);
            }
            world.setBlockToAir(pos);
        }
    }
}
