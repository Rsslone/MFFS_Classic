package dev.su5ed.mffs.util.module;

import dev.su5ed.mffs.api.Projector;
import dev.su5ed.mffs.api.module.ModuleType;
import dev.su5ed.mffs.network.DrawHologramPacket;
import dev.su5ed.mffs.network.Network;
import dev.su5ed.mffs.setup.ModModules;
import dev.su5ed.mffs.util.ModUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class StabilizationModule extends BaseModule {
    private int blockCount = 0;

    public StabilizationModule(ModuleType<?> type, ItemStack stack) {
        super(type, stack);
    }

    @Override
    public void beforeProject(Projector projector) {
        this.blockCount = 0;
    }

    @Override
    public ProjectAction onProject(Projector projector, BlockPos position) {
        if (projector.getTicks() % 40 == 0) {
            World world = projector.be().getWorld();
            BlockPos pos = projector.be().getPos();
            for (EnumFacing side : EnumFacing.values()) {
                TileEntity neighbor = world.getTileEntity(pos.offset(side));
                if (neighbor != null && neighbor.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) {
                    IItemHandler handler = neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
                    if (handler != null) {
                        for (int i = 0; i < handler.getSlots(); i++) {
                            ItemStack slotStack = handler.getStackInSlot(i);
                            if (slotStack.getItem() instanceof ItemBlock) {
                                ItemBlock itemBlock = (ItemBlock) slotStack.getItem();
                                Block block = itemBlock.getBlock();
                                IBlockState state = block.getDefaultState();
                                if (!ModUtil.isLiquidBlock(block) && world.setBlockState(position, state)) {
                                    handler.extractItem(i, 1, false);
                                    Vec3d start = new Vec3d(pos);
                                    Vec3d target = new Vec3d(position);
                                    Network.sendToAllAround(
                                        new DrawHologramPacket(start, target, DrawHologramPacket.HoloType.CONSTRUCT),
                                        world, position, 64);
                                    return this.blockCount++ >= projector.getModuleCount(ModModules.SPEED) / 3
                                        ? ProjectAction.INTERRUPT : ProjectAction.SKIP;
                                }
                            }
                        }
                    }
                }
            }
        }
        return ProjectAction.SKIP;
    }
}
