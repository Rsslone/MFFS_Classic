package dev.su5ed.mffs.blockentity;

// 1.21.x imports (commented out):
// import net.minecraft.core.BlockPos;
// import net.minecraft.network.FriendlyByteBuf;
// import net.minecraft.network.chat.MutableComponent;
// import net.minecraft.network.codec.StreamCodec;
// import net.minecraft.world.entity.player.Inventory;
// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.inventory.AbstractContainerMenu;
// import net.minecraft.world.level.block.state.BlockState;
// import net.minecraft.world.level.storage.ValueInput;
// import net.minecraft.world.level.storage.ValueOutput;
// import net.neoforged.neoforge.capabilities.Capabilities;
// import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
// import net.neoforged.neoforge.transfer.access.ItemAccess;
// import net.neoforged.neoforge.transfer.transaction.Transaction;
// import dev.su5ed.mffs.setup.ModObjects;

import dev.su5ed.mffs.MFFSConfig;
import dev.su5ed.mffs.menu.CoercionDeriverMenu;
import dev.su5ed.mffs.setup.ModModules;
import dev.su5ed.mffs.setup.ModTags;
import dev.su5ed.mffs.util.ModUtil;
import dev.su5ed.mffs.util.inventory.InventorySlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.oredict.OreDictionary;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CoercionDeriverBlockEntity extends ElectricTileEntity {
    private static final int DEFAULT_FE_CAPACITY = 1_500_000;

    public final InventorySlot batterySlot;
    public final InventorySlot fuelSlot;
    public final List<InventorySlot> upgradeSlots;

    // NOTE: Tructated to short when syncing to the client
    private int processTime;
    private EnergyMode energyMode = EnergyMode.DERIVE;

    public int fortronProducedLastTick = 0;

    public CoercionDeriverBlockEntity() {
        super(DEFAULT_FE_CAPACITY);

        // 1.21.x: stack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forStack(stack)) != null
        this.batterySlot = addSlot("battery", InventorySlot.Mode.BOTH, stack -> stack.getCapability(CapabilityEnergy.ENERGY, null) != null);
        // 1.21.x: stack.is(ModTags.FORTRON_FUEL)
        this.fuelSlot = addSlot("fuel", InventorySlot.Mode.BOTH, stack -> !OreDictionary.getOres(ModTags.FORTRON_FUEL).isEmpty() && OreDictionary.containsMatch(false, OreDictionary.getOres(ModTags.FORTRON_FUEL), stack));
        this.upgradeSlots = createUpgradeSlots(3);
        this.energy.setMaxTransfer(getMaxFETransferRate());
        this.energy.setCapacity(getScaledFECapacity());
    }

    public EnergyMode getEnergyMode() {
        return energyMode;
    }

    public void setEnergyMode(EnergyMode energyMode) {
        this.energyMode = energyMode;
    }

    public int getMaxFETransferRate() {
        return (int) (DEFAULT_FE_CAPACITY + DEFAULT_FE_CAPACITY * (getModuleCount(ModModules.SPEED) / 8.0F)); //TODO config
    }

    public int getScaledFECapacity() {
        return (int) (DEFAULT_FE_CAPACITY + DEFAULT_FE_CAPACITY * (getModuleCount(ModModules.SPEED) / 8.0));
    }

    public boolean isInversed() {
        return this.energyMode == EnergyMode.INTEGRATE;
    }

    public int getProcessTime() {
        return this.processTime;
    }

    public void setProcessTime(int processTime) {
        this.processTime = processTime;
    }

    @Override
    public int getBaseFortronTankCapacity() {
        return 30; //TODO config
    }

    @Override
    protected void addModuleSlots(List<? super InventorySlot> list) {
        super.addModuleSlots(list);
        list.addAll(this.upgradeSlots);
    }

    @Override
    protected void onInventoryChanged() {
        super.onInventoryChanged();

        this.energy.setMaxTransfer(getMaxFETransferRate());
        this.energy.setCapacity(getScaledFECapacity());
    }

    @Override
    public void tickServer() {
        super.tickServer();

        // Reset last tick values
        this.fortronProducedLastTick = 0;

        if (isActive()) {
            if (isInversed() && MFFSConfig.enableElectricity) {
                convertFortronIntoEnergy();
                chargeItemFromSelf(this.batterySlot.getItem());
                outputEnergyToNearbyTiles();
            } else if (this.fortronStorage.getStoredFortron() < this.fortronStorage.getFortronCapacity()) {
                dischargeItemIntoSelf(this.batterySlot.getItem());

                // Check if we can afford the effective cost for at least 1 Fortron.
                // Speed modules reduce the per-Fortron cost, matching 1.7.10's balanced scaling.
                if (this.energy.extractEnergy(getEffectiveCostPerFortron(), true) >= getEffectiveCostPerFortron()
                    || !MFFSConfig.enableElectricity && hasFuel()) {
                    produceFortron();
                    consumeFuel();
                }
            }
        }
    }

    private void consumeFuel() {
        // TODO Fuel display
        if (this.processTime == 0 && hasFuel()) {
            this.fuelSlot.getItem().shrink(1);
            // 1.21.x: MFFSConfig.COMMON.catalystBurnTime.get()
            this.processTime = MFFSConfig.catalystBurnTime * Math.max(getModuleCount(ModModules.SCALE) / 20, 1);
        }
        this.processTime = Math.max(--this.processTime, 0);
    }

    private void produceFortron() {
        int fortronOutput = calculateFortronProduction();
        // 1.21.x: try (Transaction tx = Transaction.openRoot()) {
        //     int fortronStored = fortronProducedLastTick = this.fortronStorage.insertFortron(fortronOutput, tx);
        //     int asEnergy = fortronStored * MFFSConfig.COMMON.coercionDriverFePerFortron.get();
        //     this.energy.extract(asEnergy, tx);
        //     tx.commit();
        // }
        int fortronStored = this.fortronStorage.insertFortron(fortronOutput, false);
        this.fortronProducedLastTick = fortronStored;
        // Proportional cost: each Fortron costs getEffectiveCostPerFortron() FE.
        // Speed modules reduce the per-Fortron cost to match 1.7.10's balanced scaling.
        int asEnergy = fortronStored * getEffectiveCostPerFortron();
        this.energy.extractEnergy(asEnergy, false);
    }

    /**
     * Returns the effective FE cost per Fortron unit, reduced by speed modules.
     * <p>
     * In 1.7.10, both energy cost and Fortron output scale by (1 + speedCount),
     * keeping the cost-per-Fortron ratio constant. In 1.21, the cost is fixed at
     * 400 FE/Fortron regardless of speed modules, creating an energy bottleneck
     * that makes speed modules ineffective with modest power sources.
     * <p>
     * This method bridges the two: the base cost (400 FE) is divided by (1 + speedCount),
     * so speed modules make production more efficient per unit while total energy
     * consumption still increases (more units × lower cost per unit).
     * <p>
     * Example with 5200 FE/tick supply:
     *   0 modules: 400 FE/Fortron → 13/tick → 260 mB/s
     *   1 module:  200 FE/Fortron → 26/tick → 520 mB/s
     *   8 modules:  44 FE/Fortron → 118/tick → 2360 mB/s
     */
    public int getEffectiveCostPerFortron() {
        int speedCount = getModuleCount(ModModules.SPEED);
        return Math.max(1, MFFSConfig.coercionDriverFePerFortron / (1 + speedCount));
    }

    /**
     * Predicted fortron to be produced next energy tick.
     * Uses proportional model (like 1.21) but with speed-module-reduced FE cost per Fortron.
     *
     * @return fortron(ml)
     */
    public int calculateFortronProduction() {
        final int spaceLeft = this.fortronStorage.getFortronCapacity() - this.fortronStorage.getStoredFortron();
        final int effectiveCost = getEffectiveCostPerFortron();
        // 1.21.x: final int maxFortronFromEnergy = this.energy.getEnergyStored() / MFFSConfig.coercionDriverFePerFortron;
        final int maxFortronFromEnergy = this.energy.getEnergyStored() / effectiveCost;
        return Math.min(maxFortronFromEnergy, Math.min(getMaxFortronProducedPerTick(), spaceLeft));
    }

    private void convertFortronIntoEnergy() {
        // 1.21.x: final int energyPerFortron = MFFSConfig.COMMON.coercionDriverFePerFortron.get() - ... etc (Transaction-based)
        final int energyPerFortron = MFFSConfig.coercionDriverFePerFortron - MFFSConfig.coercionDriverFortronToFeLoss;

        // 1.21.x: energy.getAmountAsInt() → energy.getEnergyStored()
        // 1.21.x: energy.getCapacityAsInt() → energy.getMaxEnergyStored()
        if (this.energy.getEnergyStored() + energyPerFortron < this.energy.getMaxEnergyStored()) {
            // Simulate extraction: use simulate=true to get max fortron available
            int maxFortronOut = this.fortronStorage.extractFortron(getMaxFortronProducedPerTick(), true);
            int maxEnergyOut = maxFortronOut * energyPerFortron;

            // Simulate receive: how much energy can we actually accept?
            // 1.21.x: energy.insert(amount, stx) → energy.receiveEnergy(amount, simulate)
            int maxEnergyReceived = this.energy.receiveEnergy(maxEnergyOut, true);

            // Calculate actual values to move, round down to avoid material loss
            final int fortronToRemove = (int) Math.floor(maxEnergyReceived / (float) energyPerFortron);

            // Apply values
            final int extracted = this.fortronStorage.extractFortron(fortronToRemove, false);
            this.energy.receiveEnergy(extracted * energyPerFortron, false);
        }
    }

    /**
     * Upper limit on fortron produced per tick
     *
     * @return fortron(ml) per tick
     */
    public int getMaxFortronProducedPerTick() {
        if (isActive()) {
            // 1.21.x: MFFSConfig.COMMON.coercionDriverFortronPerTick.get()
            final int perTick = MFFSConfig.coercionDriverFortronPerTick;
            final int speedBonus = MFFSConfig.coercionDriverFortronPerTickSpeedModule * getModuleCount(ModModules.SPEED);
            final int production = perTick + speedBonus;
            final double catMultiplier = this.hasFuel() ? Math.max(MFFSConfig.catalystMultiplier, 0) : 0;
            return production + (int) Math.floor(production * catMultiplier);
        }
        return 0;
    }

    public boolean hasFuel() {
        return !this.fuelSlot.isEmpty();
    }

    // 1.21.x: createMenu(int containerId, Inventory inventory, Player player) -> AbstractContainerMenu
    // In 1.12.2, GUI is handled via IGuiHandler registered in MFFSMod. ModMenus.COERCION_DERIVER = GUI ID.
    // TODO: Ensure IGuiHandler returns new CoercionDeriverMenu for the matching GUI ID

    @Override
    protected void saveTag(NBTTagCompound compound) {
        super.saveTag(compound);

        compound.setInteger("processTime", this.processTime);
        compound.setString("energyMode", this.energyMode.name());
    }

    @Override
    protected void loadTag(NBTTagCompound compound) {
        super.loadTag(compound);

        this.processTime = compound.getInteger("processTime");
        String energyModeName = compound.getString("energyMode");
        if (!energyModeName.isEmpty()) {
            try { this.energyMode = EnergyMode.valueOf(energyModeName); } catch (IllegalArgumentException ignored) {}
        }
    }

    // 1.21.x: Set<Direction> -> Set<EnumFacing>
    @Override
    public Set<EnumFacing> getEnergyInputSides() {
        return EnumSet.allOf(EnumFacing.class);
    }

    @Override
    public Set<EnumFacing> getEnergyOutputSides() {
        return EnumSet.allOf(EnumFacing.class);
    }

    public enum EnergyMode {
        DERIVE,     // FE -> FORT
        INTEGRATE;  // FORT -> FE

        private static final EnergyMode[] VALUES = values();
        // 1.21.x: public static final StreamCodec<FriendlyByteBuf, EnergyMode> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(EnergyMode.class);
        // In 1.12.2: serialized by ordinal via SwitchEnergyModePacket

        public EnergyMode next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        // 1.21.x: MutableComponent translate()
        public net.minecraft.util.text.ITextComponent translate() {
            return ModUtil.translate("info", "coercion_deriver.mode." + name().toLowerCase(Locale.ROOT));
        }
    }
}
