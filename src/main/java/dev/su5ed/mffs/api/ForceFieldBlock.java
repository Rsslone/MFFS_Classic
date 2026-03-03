package dev.su5ed.mffs.api;

// 1.12.2 Backport: ForceFieldBlock interface
// 1.21.x used NeoForge ModelProperty<BlockState> for camouflage.
// In 1.12.2: IExtendedBlockState / IUnlistedProperty from net.minecraftforge.common.property
// is the rough equivalent, but the usage is very different.
// ModelProperty is removed; camouflage state is stored in tile entity NBT instead.

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.util.Optional;

/**
 * Force Field Block that is projected as part of a force field.
 */
public interface ForceFieldBlock {
    // TODO (1.12.2): Camouflage block state is stored in the TileEntity's NBT
    //   ("camouflageBlock" string key: registry name of the block to mimic).
    //   Use IExtendedBlockState + IUnlistedProperty<IBlockState> if baked model
    //   access is needed at render time.

    /**
     * Get the projector that created this force field block.
     *
     * @param world the world to look in
     * @param pos   the position to search
     * @return the force field block's projector
     */
    Optional<Projector> getProjector(IBlockAccess world, BlockPos pos);
}
