package dev.su5ed.mffs.blockentity;

import dev.su5ed.mffs.MFFSConfig;
import dev.su5ed.mffs.api.module.InterdictionMatrixModule;
import dev.su5ed.mffs.api.module.Module;
import dev.su5ed.mffs.api.security.BiometricIdentifier;
import dev.su5ed.mffs.api.security.FieldPermission;
import dev.su5ed.mffs.api.security.InterdictionMatrix;
import dev.su5ed.mffs.setup.ModCapabilities;
import dev.su5ed.mffs.setup.ModItems;
import dev.su5ed.mffs.setup.ModModules;
import dev.su5ed.mffs.util.ModUtil;
import dev.su5ed.mffs.util.inventory.InventorySlot;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class InterdictionMatrixBlockEntity extends ModularBlockEntity implements InterdictionMatrix {
    public final InventorySlot secondaryCard;
    public final List<InventorySlot> upgradeSlots;
    public final List<InventorySlot> bannedItemSlots;

    private ConfiscationMode confiscationMode = ConfiscationMode.BLACKLIST;

    public InterdictionMatrixBlockEntity() {
        super();

        this.secondaryCard = addSlot("secondaryCard", InventorySlot.Mode.BOTH,
            stack -> ModUtil.isCard(stack) || stack.getItem() == ModItems.INFINITE_POWER_CARD,
            this::onFrequencySlotChanged);
        this.upgradeSlots = createUpgradeSlots(8, Module.Category.INTERDICTION, stack -> {});
        this.bannedItemSlots = IntStreamEx.range(9)
            .mapToObj(i -> addVirtualSlot("banned_item_" + i))
            .toList();
    }

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        if (capability == ModCapabilities.INTERDICTION_MATRIX) return true;
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        if (capability == ModCapabilities.INTERDICTION_MATRIX) return (T) this;
        return super.getCapability(capability, facing);
    }

    public int getWarningRange() {
        return getModuleCount(ModModules.WARN) + getActionRange() + 3;
    }

    public int getActionRange() {
        return getModuleCount(ModModules.SCALE);
    }

    @Override
    public dev.su5ed.mffs.blockentity.BaseBlockEntity be() {
        return this;
    }

    @Override
    public Collection<ItemStack> getFilteredItems() {
        return StreamEx.of(this.bannedItemSlots)
            .remove(InventorySlot::isEmpty)
            .map(InventorySlot::getItem)
            .toList();
    }

    @Override
    public ConfiscationMode getConfiscationMode() {
        return this.confiscationMode;
    }

    @Override
    public ITextComponent getTitle() {
        return new TextComponentTranslation("tile.mffs.interdiction_matrix.name");
    }

    public void setConfiscationMode(ConfiscationMode confiscationMode) {
        this.confiscationMode = confiscationMode;
    }

    @Override
    protected void addModuleSlots(List<? super InventorySlot> list) {
        super.addModuleSlots(list);
        list.addAll(this.upgradeSlots);
    }

    @Override
    public int getBaseFortronTankCapacity() {
        return MFFSConfig.interdictionMatrixInitialTankCapacity;
    }

    @Override
    protected int getCapacityBoostPerModule() {
        return MFFSConfig.interdictionMatrixTankCapacityPerModule;
    }

    @Override
    public void tickServer() {
        super.tickServer();

        if (getTicks() % MFFSConfig.FORTRON_TRANSFER_TICKS == 0 && (isActive() || this.frequencySlot.getItem().getItem() == ModItems.INFINITE_POWER_CARD)) {
            int extracted = this.fortronStorage.extractFortron(getFortronCost() * MFFSConfig.FORTRON_TRANSFER_TICKS, true);
            if (extracted > 0) {
                this.fortronStorage.extractFortron(getFortronCost() * MFFSConfig.FORTRON_TRANSFER_TICKS, false);
                scan();
            }
        }
    }

    @Override
    protected float getAmplifier() {
        return Math.max(Math.min(getActionRange() / 20, 10), 1);
    }

    public void scan() {
        BiometricIdentifier identifier = getBiometricIdentifier();
        AxisAlignedBB emptyBounds = new AxisAlignedBB(this.pos, this.pos.add(1, 1, 1));

        List<EntityLivingBase> warningList = this.world.getEntitiesWithinAABB(EntityLivingBase.class, emptyBounds.grow(getWarningRange(), getWarningRange(), getWarningRange()));
        List<EntityLivingBase> actionList = this.world.getEntitiesWithinAABB(EntityLivingBase.class, emptyBounds.grow(getActionRange(), getActionRange(), getActionRange()));

        for (EntityLivingBase entity : warningList) {
            if (entity instanceof EntityPlayer player && !actionList.contains(entity) && !canPlayerBypass(identifier, player) && this.world.rand.nextInt(3) == 0) {
                ITextComponent msg = ModUtil.translate("info", "interdiction_matrix.warning", getTitle());
                ((net.minecraft.util.text.Style) msg.getStyle()).setColor(TextFormatting.RED);
                player.sendMessage(msg);
            }
        }

        if (this.world.rand.nextInt(3) == 0) {
            for (EntityLivingBase entity : actionList) {
                applyAction(entity);
            }
        }
    }

    public void applyAction(EntityLivingBase target) {
        if (target instanceof EntityPlayer player) {
            BiometricIdentifier identifier = getBiometricIdentifier();
            if (canPlayerBypass(identifier, player) || MFFSConfig.interactCreative && player.capabilities.isCreativeMode) {
                return;
            }
        }
        for (Module module : getModuleInstances()) {
            if (module instanceof InterdictionMatrixModule interdictionModule && interdictionModule.onDefend(this, target) || target.isDead) {
                break;
            }
        }
    }

    public boolean canPlayerBypass(BiometricIdentifier identifier, EntityPlayer player) {
        return identifier != null && identifier.isAccessGranted(player, FieldPermission.BYPASS_CONFISCATION);
    }

    @Override
    protected void loadCommonTag(NBTTagCompound compound) {
        super.loadCommonTag(compound);

        String modeName = compound.getString("confiscationMode");
        if (!modeName.isEmpty()) {
            try { this.confiscationMode = ConfiscationMode.valueOf(modeName); } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    protected void saveCommonTag(NBTTagCompound compound) {
        super.saveCommonTag(compound);

        compound.setString("confiscationMode", this.confiscationMode.name());
    }
}
