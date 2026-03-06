package dev.su5ed.mffs.screen;

// 1.12.2 Backport: CoercionDeriverScreen
// ClientPacketDistributor.sendToServer() → Network.sendToServer()
// Matrix3x2fStack pose rotation → GlStateManager.rotate() + translate()
// GuiGraphics.drawString() → fontRenderer.drawString()
// ChatFormatting → net.minecraft.util.text.TextFormatting

import dev.su5ed.mffs.MFFSMod;
import dev.su5ed.mffs.blockentity.CoercionDeriverBlockEntity.EnergyMode;
import dev.su5ed.mffs.menu.CoercionDeriverMenu;
import dev.su5ed.mffs.network.Network;
import dev.su5ed.mffs.network.SwitchEnergyModePacket;
import dev.su5ed.mffs.util.ModUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public class CoercionDeriverScreen extends FortronScreen<CoercionDeriverMenu> {
    public static final ResourceLocation BACKGROUND = new ResourceLocation(MFFSMod.MODID, "textures/gui/coercion_deriver.png");

    public CoercionDeriverScreen(CoercionDeriverMenu menu, InventoryPlayer playerInventory) {
        super(menu, playerInventory, BACKGROUND);
        this.frequencyBoxX = 30;
        this.frequencyBoxY = 43;
        this.frequencyLabelX = 8;
        this.frequencyLabelY = 30;
        this.fortronEnergyBarX = 8;
        this.fortronEnergyBarY = 115;
        this.fortronEnergyBarWidth = 103;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.buttonList.add(new TextButton(
            this.width / 2 - 10, this.height / 2 - 28, 58, 20,
            () -> ((CoercionDeriverMenu) this.inventorySlots).blockEntity.getEnergyMode().translate(),
            () -> {
                CoercionDeriverMenu menu = (CoercionDeriverMenu) this.inventorySlots;
                EnergyMode mode = menu.blockEntity.getEnergyMode().next();
                menu.blockEntity.setEnergyMode(mode);
                Network.sendToServer(new SwitchEnergyModePacket(menu.blockEntity.getPos(), mode));
            }
        ));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        CoercionDeriverMenu menu = (CoercionDeriverMenu) this.inventorySlots;

        // Rotated "upgrade" label
        GlStateManager.pushMatrix();
        GlStateManager.translate(140, 95, 0);
        GlStateManager.rotate(-90.0F, 0, 0, 1);
        GlStateManager.translate(-140, -95, 0);
        this.fontRenderer.drawString(ModUtil.translate("screen", "upgrade").getFormattedText(), 140, 95, GuiColors.DARK_GREY);
        GlStateManager.popMatrix();

        // Progress status
        String progressKey = "progress." + (menu.blockEntity.isActive() ? "running" : "idle");
        String progressText = ModUtil.translate("screen", "progress").getFormattedText()
            + ModUtil.translate("screen", progressKey).getFormattedText();
        this.fontRenderer.drawString(progressText, 8, 70, GuiColors.DARK_GREY);

        // Fortron stored
        int energy = menu.blockEntity.fortronStorage.getStoredFortron();
        this.fontRenderer.drawString(ModUtil.translate("screen", "fortron.short", energy).getFormattedText(), 8, 105, GuiColors.DARK_GREY);

        // Fortron cost
        boolean inversed = menu.blockEntity.isInversed();
        int displayFortron = menu.blockEntity.fortronProducedLastTick * ModUtil.TICKS_PER_SECOND;
        String sign = inversed ? "-" : "+";
        int costColor = inversed ? 0xAA0000 : 0x00AA00;
        this.fontRenderer.drawString(ModUtil.translate("screen", "fortron_cost", sign, displayFortron).getFormattedText(), 114, 117, costColor);
    }
}

/* class CoercionDeriverScreen_NeoForge_1_21_x:
... original NeoForge source (Matrix3x2fStack, GuiGraphics, ClientPacketDistributor, ChatFormatting) preserved for reference ...
*/
