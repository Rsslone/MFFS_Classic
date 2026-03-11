package dev.su5ed.mffs.api.card;

// 1.12.2 Backport: CoordLink
// BlockPos/ItemStack namespace change only.

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

// TODO capability: expose via ICapabilityProvider.getCapability(ModCapabilities.FREQUENCY_CARD, null)
public interface CoordLink {
    void setLink(ItemStack stack, BlockPos pos);

    @Nullable
    BlockPos getLink(ItemStack stack);
}
