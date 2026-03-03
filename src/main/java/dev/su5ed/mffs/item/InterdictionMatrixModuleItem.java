package dev.su5ed.mffs.item;

// 1.12.2 Backport: appendHoverText(NeoForge) → addInformation(1.12.2).

import dev.su5ed.mffs.api.module.InterdictionMatrixModule;
import dev.su5ed.mffs.api.module.ModuleType;
import dev.su5ed.mffs.setup.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InterdictionMatrixModuleItem extends ModuleItem<InterdictionMatrixModule> {

    public InterdictionMatrixModuleItem(ModuleType<InterdictionMatrixModule> module) {
        super(module);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addInformationPre(ItemStack stack, @Nullable World worldIn, List<String> tooltip,
                                     net.minecraft.client.util.ITooltipFlag flagIn) {
        // Show which machine this module belongs to
        if (ModBlocks.INTERDICTION_MATRIX != null) {
            tooltip.add(TextFormatting.DARK_RED + ModBlocks.INTERDICTION_MATRIX.getLocalizedName());
        }
        super.addInformationPre(stack, worldIn, tooltip, flagIn);
    }
}

/*
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
public class InterdictionMatrixModuleItem_NeoForge extends ModuleItem<InterdictionMatrixModule> {
    public InterdictionMatrixModuleItem_NeoForge(ExtendedItemProperties properties, ModuleType<InterdictionMatrixModule> module) { super(properties, module); }
    @Override
    protected void appendHoverTextPre(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(ModBlocks.INTERDICTION_MATRIX.get().getName().withStyle(ChatFormatting.DARK_RED));
        super.appendHoverTextPre(stack, context, tooltipDisplay, tooltipAdder, flag);
    }
}
*/
