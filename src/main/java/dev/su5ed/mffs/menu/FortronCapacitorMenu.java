package dev.su5ed.mffs.menu;

// 1.12.2 Backport: FortronCapacitorMenu
// BlockPos → net.minecraft.util.math.BlockPos
// Player → EntityPlayer; Inventory → InventoryPlayer
// addDataSlot(new DataSlotWrapper()) → addDataSlot(getter, setter)

import dev.su5ed.mffs.blockentity.FortronCapacitorBlockEntity;
import dev.su5ed.mffs.util.TransferMode;
import dev.su5ed.mffs.util.inventory.SlotInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import one.util.streamex.EntryStream;

public class FortronCapacitorMenu extends FortronMenu<FortronCapacitorBlockEntity> {

    public FortronCapacitorMenu(World world, BlockPos pos, EntityPlayer player, InventoryPlayer playerInventory) {
        super(world, pos, player, playerInventory);

        layoutPlayerInventorySlots(8, 135);
        addDataSlot(() -> this.blockEntity.getTransferMode().ordinal(),
            i -> this.blockEntity.setTransferMode(TransferMode.values()[i]));

        EntryStream.of(this.blockEntity.upgradeSlots)
            .forKeyValue((i, slot) -> addInventorySlot(new SlotInventory(slot, 154, 47 + i * 20)));
        addInventorySlot(new SlotInventory(this.blockEntity.frequencySlot, 9, 74));
        addInventorySlot(new SlotInventory(this.blockEntity.secondaryCard, 27, 74));
    }
}
