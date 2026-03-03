package dev.su5ed.mffs.util.inventory;

// 1.12.2 Backport: SlotInventoryFilter
// mayPlace()/mayPickup() → isItemValid()/canTakeStack()
// allowModification(Player) does not exist in 1.12.2 Slot — removed
// remove(int) → decrStackSize(int)

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class SlotInventoryFilter extends SlotInventory {

    public SlotInventoryFilter(InventorySlot inventorySlot, int x, int y) {
        super(inventorySlot, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        return ItemStack.EMPTY;
    }
}
