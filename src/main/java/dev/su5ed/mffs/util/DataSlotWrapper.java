package dev.su5ed.mffs.util;

// 1.12.2 Backport: DataSlotWrapper is no longer used in active code.
// FortronMenu now tracks data slots via dataGetters/dataSetters lambda lists
// and detectAndSendChanges() / updateProgressBar() directly.
// Old NeoForge implementation preserved below for reference.

/* class DataSlotWrapper_NeoForge_1_21_x:

import net.minecraft.world.inventory.DataSlot;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class DataSlotWrapper extends DataSlot {
    private final IntSupplier getter;
    private final IntConsumer setter;

    public DataSlotWrapper(IntSupplier getter, IntConsumer setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public int get() {
        return this.getter.getAsInt();
    }

    @Override
    public void set(int value) {
        this.setter.accept(value);
    }
}
*/
