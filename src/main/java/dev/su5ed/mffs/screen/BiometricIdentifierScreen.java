package dev.su5ed.mffs.screen;

// 1.12.2 Backport: BiometricIdentifierScreen
// ClientPacketDistributor.sendToServer() → Network.sendToServer()
// AbstractWidget → GuiButton (IconToggleButton)
// addWidget() → just keep in permissionButtons list and call drawButton() manually

import dev.su5ed.mffs.MFFSMod;
import dev.su5ed.mffs.api.security.FieldPermission;
import dev.su5ed.mffs.menu.BiometricIdentifierMenu;
import dev.su5ed.mffs.network.Network;
import dev.su5ed.mffs.network.ToggleFieldPermissionPacket;
import dev.su5ed.mffs.util.ModUtil;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class BiometricIdentifierScreen extends FortronScreen<BiometricIdentifierMenu> {
    public static final ResourceLocation BACKGROUND = new ResourceLocation(MFFSMod.MODID, "textures/gui/biometric_identifier.png");

    private final List<IconToggleButton> permissionButtons = new ArrayList<>();

    public BiometricIdentifierScreen(BiometricIdentifierMenu menu, InventoryPlayer playerInventory) {
        super(menu, playerInventory, BACKGROUND);
        this.frequencyBoxX = 109;
        this.frequencyBoxY = 92;
        this.frequencyLabelX = 87;
        this.frequencyLabelY = 80;
        this.fortronEnergyBarX = 87;
        this.fortronEnergyBarY = 66;
        this.fortronEnergyBarWidth = 82;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.permissionButtons.clear();
        for (int i = 0, x = 0, y = 0; i < FieldPermission.values().length; i++) {
            x++;
            FieldPermission permission = FieldPermission.values()[i];
            IconToggleButton widget = new IconToggleButton(
                this.width / 2 - 21 + 20 * x, this.height / 2 - 87 + 20 * y, 18, 18,
                ModUtil.translateTooltip(permission),
                18, 18 * i,
                () -> ((BiometricIdentifierMenu) this.inventorySlots).hasPermission(permission),
                value -> togglePermission(permission, !value)
            );
            this.permissionButtons.add(widget);
            this.buttonList.add(widget);
            if (i % 3 == 0 && i != 0) {
                x = 0;
                y++;
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);
        // Permission buttons only visible when rightsSlot has an item
        BiometricIdentifierMenu menu = (BiometricIdentifierMenu) this.inventorySlots;
        for (IconToggleButton btn : this.permissionButtons) {
            btn.visible = !menu.blockEntity.rightsSlot.isEmpty();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        drawWithTooltip(28, 50, GuiColors.DARK_GREY, "rights");
        drawWithTooltip(28, 70, GuiColors.DARK_GREY, "copy");
        drawWithTooltip(28, 95, GuiColors.DARK_GREY, "master");
    }

    public void togglePermission(FieldPermission permission, boolean value) {
        Network.sendToServer(new ToggleFieldPermissionPacket(
            ((BiometricIdentifierMenu) this.inventorySlots).blockEntity.getPos(), permission, value));
    }
}

/* class BiometricIdentifierScreen_NeoForge_1_21_x:
... original NeoForge source preserved for reference (ClientPacketDistributor, AbstractWidget, addWidget, etc.) ...
*/
