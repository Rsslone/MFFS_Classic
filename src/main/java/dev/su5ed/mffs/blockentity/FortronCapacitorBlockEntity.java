package dev.su5ed.mffs.blockentity;

// 1.21.x imports (commented out):
// import net.minecraft.core.BlockPos;
// import net.minecraft.world.MenuProvider;
// import net.minecraft.world.entity.player.Inventory;
// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.inventory.AbstractContainerMenu;
// import net.minecraft.world.level.block.state.BlockState;
// import net.minecraft.world.level.storage.ValueInput;
// import net.minecraft.world.level.storage.ValueOutput;
// import dev.su5ed.mffs.setup.ModObjects;

import dev.su5ed.mffs.MFFSConfig;
import dev.su5ed.mffs.api.card.CoordLink;
import dev.su5ed.mffs.api.fortron.FortronCapacitor;
import dev.su5ed.mffs.api.fortron.FortronStorage;
import dev.su5ed.mffs.setup.ModCapabilities;
import dev.su5ed.mffs.setup.ModItems;
import dev.su5ed.mffs.setup.ModModules;
import dev.su5ed.mffs.util.Fortron;
import dev.su5ed.mffs.util.FrequencyGrid;
import dev.su5ed.mffs.util.ModUtil;
import dev.su5ed.mffs.util.TransferMode;
import dev.su5ed.mffs.util.inventory.InventorySlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// In 1.12.2, MenuProvider is not needed — GUI handled via IGuiHandler registered in MFFSMod
public class FortronCapacitorBlockEntity extends ModularBlockEntity implements FortronCapacitor {
    public final InventorySlot secondaryCard;
    public final List<InventorySlot> upgradeSlots;

    private TransferMode transferMode = TransferMode.EQUALIZE;

    public FortronCapacitorBlockEntity() {
        super(10);

        // 1.21.x: stack -> ModUtil.isCard(stack) || stack.is(ModItems.INFINITE_POWER_CARD.get())
        this.secondaryCard = addSlot("secondaryCard", InventorySlot.Mode.BOTH,
            stack -> ModUtil.isCard(stack) || stack.getItem() == ModItems.INFINITE_POWER_CARD,
            this::onFrequencySlotChanged);
        this.upgradeSlots = createUpgradeSlots(3);
    }

    public TransferMode getTransferMode() {
        return this.transferMode;
    }

    public void setTransferMode(TransferMode transferMode) {
        this.transferMode = transferMode;
        markDirty();
    }

    @Override
    public int getBaseFortronTankCapacity() {
        return MFFSConfig.fortronCapacitorInitialTankCapacity;
    }

    @Override
    protected int getCapacityBoostPerModule() {
        return MFFSConfig.fortronCapacitorTankCapacityPerModule;
    }

    @Override
    protected void addModuleSlots(List<? super InventorySlot> list) {
        super.addModuleSlots(list);
        list.addAll(this.upgradeSlots);
    }

    @Override
    protected float getAmplifier() {
        return 0.001F;
    }

    @Override
    public void tickServer() {
        super.tickServer();

        // Bill maintenance cost as a burst, aligned with the Fortron distribution window.
        // Same total drain as per-tick, but in sync with when Fortron actually moves on the network.
        if (getTicks() % MFFSConfig.FORTRON_TRANSFER_TICKS == 0) {
            this.fortronStorage.extractFortron(getFortronCost() * MFFSConfig.FORTRON_TRANSFER_TICKS, false);
        }

        // Distribute fortron across the network
        if (isActive() && getTicks() % MFFSConfig.FORTRON_TRANSFER_TICKS == 0) {
            Set<FortronStorage> machines = new HashSet<>();

            for (ItemStack stack : getCards()) {
                // 1.21.x: stack.is(ModItems.INFINITE_POWER_CARD.get())
                if (stack.getItem() == ModItems.INFINITE_POWER_CARD) {
                    this.fortronStorage.setStoredFortron(this.fortronStorage.getFortronCapacity());
                }
                // 1.21.x: stack.getItem() instanceof CoordLink coordLink
                else if (stack.getItem() instanceof CoordLink coordLink) {
                    Optional.ofNullable(coordLink.getLink(stack))
                        .map(linkPosition -> {
                            // 1.21.x: this.level.getCapability(ModCapabilities.FORTRON, linkPosition, null)
                            var te = this.world.getTileEntity(linkPosition);
                            if (te != null && te.hasCapability(ModCapabilities.FORTRON, null)) {
                                return (FortronStorage) te.getCapability(ModCapabilities.FORTRON, null);
                            }
                            return null;
                        })
                        .ifPresent(fortron -> {
                            machines.add(this.fortronStorage);
                            machines.add(fortron);
                        });
                }
            }

            Fortron.transferFortron(this.fortronStorage, machines.isEmpty() ? getDevicesByFrequency() : machines, this.transferMode, getTransmissionRate());
        }
    }

    @Override
    public Collection<FortronStorage> getDevicesByFrequency() {
        // 1.21.x: this.level, this.worldPosition → this.world, this.pos
        return FrequencyGrid.instance().get(this.world, this.pos, getTransmissionRange(), this.fortronStorage.getFrequency());
    }

    @Override
    public List<ItemStack> getCards() {
        // 1.21.x: List.of(this.frequencySlot.getItem(), this.secondaryCard.getItem())
        return Arrays.asList(this.frequencySlot.getItem(), this.secondaryCard.getItem());
    }

    @Override
    public int getTransmissionRange() {
        return MFFSConfig.fortronCapacitorInitialRange + MFFSConfig.fortronCapacitorRangePerModule * getModuleCount(ModModules.SCALE);
    }

    @Override
    public int getTransmissionRate() {
        return MFFSConfig.fortronCapacitorInitialTransmissionRate + MFFSConfig.fortronCapacitorTransmissionRatePerModule * getModuleCount(ModModules.SPEED);
    }

    @Override
    protected void saveCommonTag(NBTTagCompound compound) {
        super.saveCommonTag(compound);
        compound.setString("transferMode", this.transferMode.name());
    }

    @Override
    protected void loadCommonTag(NBTTagCompound compound) {
        super.loadCommonTag(compound);

        String modeName = compound.getString("transferMode");
        if (!modeName.isEmpty()) {
            try { this.transferMode = TransferMode.valueOf(modeName); } catch (IllegalArgumentException ignored) {}
        }
    }

    // 1.21.x: createMenu(int containerId, Inventory inventory, Player player) → AbstractContainerMenu
    // In 1.12.2, GUI is handled via IGuiHandler registered in MFFSMod. ModMenus.FORTRON_CAPACITOR = GUI ID.
    // TODO: Ensure IGuiHandler returns new FortronCapacitorMenu for the matching GUI ID
}
