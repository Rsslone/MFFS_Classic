package dev.su5ed.mffs.blockentity;

// 1.21.x imports (commented out):
// import net.minecraft.core.BlockPos;
// import net.minecraft.server.level.ServerPlayer;
// import net.minecraft.world.entity.player.Inventory;
// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.inventory.AbstractContainerMenu;
// import net.minecraft.world.level.block.state.BlockState;
// import dev.su5ed.mffs.setup.ModObjects;

import dev.su5ed.mffs.MFFSConfig;
import dev.su5ed.mffs.api.card.IdentificationCard;
import dev.su5ed.mffs.api.security.BiometricIdentifier;
import dev.su5ed.mffs.api.security.FieldPermission;
import dev.su5ed.mffs.setup.ModCapabilities;
import dev.su5ed.mffs.util.ModUtil;
import dev.su5ed.mffs.util.inventory.CopyingIdentificationCard;
import dev.su5ed.mffs.util.inventory.InventorySlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import java.util.List;
import java.util.Optional;

public class BiometricIdentifierBlockEntity extends FortronBlockEntity implements BiometricIdentifier {
    public final InventorySlot masterSlot;
    public final InventorySlot rightsSlot;
    public final InventorySlot copySlot;
    public final List<InventorySlot> identitySlots;

    public BiometricIdentifierBlockEntity() {
        super();

        // TODO: Restrict slot access to GUI
        this.masterSlot = addSlot("master", InventorySlot.Mode.BOTH, ModUtil::isIdentificationCard);
        this.rightsSlot = addSlot("rights", InventorySlot.Mode.BOTH, ModUtil::isIdentificationCard);
        this.copySlot = addSlot("copy", InventorySlot.Mode.BOTH, ModUtil::isIdentificationCard, this::copyCard);
        this.identitySlots = IntStreamEx.range(9)
            .mapToObj(i -> addSlot("identity_" + i, InventorySlot.Mode.BOTH, ModUtil::isIdentificationCard))
            .toList();
    }

    @Override
    public Optional<IdentificationCard> getManipulatingCard() {
        // 1.21.x: stack.getCapability(ModCapabilities.IDENTIFICATION_CARD) — no direction needed for items
        return Optional.ofNullable(this.rightsSlot.getItem().getCapability(ModCapabilities.IDENTIFICATION_CARD, null))
            .map(card -> Optional.ofNullable(this.copySlot.getItem().getCapability(ModCapabilities.IDENTIFICATION_CARD, null))
                .<IdentificationCard>map(copy -> new CopyingIdentificationCard(card, copy))
                .orElse(card));
    }

    private void copyCard(ItemStack stack) {
        // 1.21.x: stack.getCapability(ModCapabilities.IDENTIFICATION_CARD) — no direction needed for items
        Optional.ofNullable(this.rightsSlot.getItem().getCapability(ModCapabilities.IDENTIFICATION_CARD, null))
            .ifPresent(card -> Optional.ofNullable(stack.getCapability(ModCapabilities.IDENTIFICATION_CARD, null))
                .ifPresent(card::copyTo));
    }

    @Override
    public void setActive(boolean active) {
        if (!this.masterSlot.isEmpty() || !active) {
            super.setActive(active);
        }
    }

    @Override
    public boolean isActive() {
        return !this.masterSlot.isEmpty() && super.isActive();
    }

    @Override
    protected void animate() {
        super.animate();

        if (!isActive()) {
            this.animation = 0;
        }
    }

    @Override
    // 1.21.x: isAccessGranted(Player player, ...) — Player was net.minecraft.world.entity.player.Player
    public boolean isAccessGranted(EntityPlayer player, FieldPermission permission) {
        return !isActive() || canOpBypass(player) || StreamEx.of(this.masterSlot)
            .append(this.identitySlots)
            .anyMatch(slot -> {
                // 1.21.x: slot.getItem().getCapability(ModCapabilities.IDENTIFICATION_CARD)
                IdentificationCard card = slot.getItem().getCapability(ModCapabilities.IDENTIFICATION_CARD, null);
                return card != null && card.checkIdentity(player);
            });
    }

    // 1.21.x: createMenu(int containerId, Inventory playerInventory, Player player) → AbstractContainerMenu
    // In 1.12.2, GUI is handled via IGuiHandler registered in MFFSMod. ModMenus.BIOMETRIC_IDENTIFIER = GUI ID.
    // TODO: Ensure IGuiHandler returns new BiometricIdentifierMenu for the matching GUI ID

    // 1.21.x: canOpBypass(Player player) — Player was net.minecraft.world.entity.player.Player
    public static boolean canOpBypass(EntityPlayer player) {
        // 1.21.x: player instanceof ServerPlayer serverPlayer
        //     && MFFSConfig.COMMON.allowOpBiometryOverride.get()
        //     && serverPlayer.server.getPlayerList().isOp(player.nameAndId())
        return player instanceof EntityPlayerMP serverPlayer
            && MFFSConfig.allowOpBiometryOverride
            && serverPlayer.server.getPlayerList().canSendCommands(serverPlayer.getGameProfile());
    }
}
