package dev.su5ed.mffs.item;

// =============================================================================
// 1.12.2 Backport: BaseItem
// Key changes:
//   Item(Properties) → Item() with setter methods
//   appendHoverText(TooltipDisplay, Consumer<Component>) → addInformation(List<String>)
//   ExtendedItemProperties → simplified shim (description via boolean flag)
//   Component/ChatFormatting → String + TextFormatting
//   BuiltInRegistries → getRegistryName()
// =============================================================================

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BaseItem extends Item {

    /** If true, hovering with Shift shows the item's description translation. */
    protected final boolean showDescription;

    public BaseItem() {
        this(false);
    }

    public BaseItem(boolean showDescription) {
        this.showDescription = showDescription;
    }

    /**
     * Override to inject tooltip lines before the description line.
     * Equivalent of 1.21.x {@code appendHoverTextPre}.
     */
    @SideOnly(Side.CLIENT)
    protected void addInformationPre(ItemStack stack, @Nullable World worldIn, List<String> tooltip,
                                     net.minecraft.client.util.ITooltipFlag flagIn) {}

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        addInformationPre(stack, worldIn, tooltip, flagIn);
        if (this.showDescription) {
            if (GuiScreen.isShiftKeyDown()) {
                // Shift held: show full description
                String descKey = "item.mffs." + this.getRegistryName().getPath() + ".description";
                tooltip.add(TextFormatting.GRAY + I18n.format(descKey));
            } else {
                // Hint to hold Shift
                tooltip.add(TextFormatting.DARK_GRAY + I18n.format("info.mffs.show_details",
                    TextFormatting.GRAY + I18n.format("info.mffs.key.shift")));
            }
        }
    }

    // -----------------------------------------------------------------
    // Compatibility shim for old 1.21.x ExtendedItemProperties pattern.
    // In 1.12.2, just pass `showDescription=true` to the BaseItem ctor.
    // This stub class lets callers that haven't been fully ported to
    // compile without errors while the migration is in progress.
    // -----------------------------------------------------------------
    public static class ExtendedItemProperties {
        public boolean hasDescription;

        /** No-op in 1.12.2; use BaseItem(true) instead. */
        public ExtendedItemProperties description() {
            this.hasDescription = true;
            return this;
        }
    }
}
