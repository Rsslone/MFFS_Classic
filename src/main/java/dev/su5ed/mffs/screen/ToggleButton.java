package dev.su5ed.mffs.screen;

// 1.12.2 Backport: ToggleButton
// GuiGraphics.renderItem() → mc.getRenderItem().renderItemIntoGUI() + renderItemOverlayIntoGUI()
// ModItems.REDSTONE_TORCH_OFF.get() — still valid in 1.12.2 via RegistryObject<Item>
// Items.REDSTONE_TORCH → net.minecraft.init.Items.REDSTONE_TORCH

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.function.BooleanSupplier;

public class ToggleButton extends BaseButton {
    // 1.12.2 Backport: Blocks.UNLIT_REDSTONE_TORCH has no item form, so use
    // Items.REDSTONE as the off-state icon instead.
    private final ItemStack itemOff = new ItemStack(Items.REDSTONE);
    private final ItemStack itemOn = new ItemStack(Item.getItemFromBlock(Blocks.REDSTONE_TORCH));

    private final BooleanSupplier enabled;

    public ToggleButton(int x, int y, BooleanSupplier enabled, Runnable onPress) {
        super(x, y, 20, 20, onPress);
        this.enabled = enabled;
    }

    @Override
    public void renderFg(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        ItemStack stack = this.enabled.getAsBoolean() ? this.itemOn : this.itemOff;
        mc.getRenderItem().renderItemIntoGUI(stack, this.x + 2, this.y - 1);
    }
}

/* class ToggleButton_NeoForge_1_21_x:
import dev.su5ed.mffs.setup.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.BooleanSupplier;

public class ToggleButton extends BaseButton {
    private final ItemStack itemOff = new ItemStack(ModItems.REDSTONE_TORCH_OFF.get());
    private final ItemStack itemOn = new ItemStack(Items.REDSTONE_TORCH);
    private final BooleanSupplier enabled;

    public ToggleButton(int x, int y, BooleanSupplier enabled, Runnable onPress) {
        super(x, y, 20, 20, onPress);
        this.enabled = enabled;
    }

    @Override
    public void renderFg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY, float partialTick) {
        guiGraphics.renderItem(this.enabled.getAsBoolean() ? this.itemOn : this.itemOff, getX() + 2, getY() - 1, 0);
    }
}
*/
