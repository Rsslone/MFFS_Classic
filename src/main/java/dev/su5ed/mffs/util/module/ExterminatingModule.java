package dev.su5ed.mffs.util.module;

import dev.su5ed.mffs.MFFSConfig;
import dev.su5ed.mffs.api.fortron.FortronStorage;
import dev.su5ed.mffs.api.module.ModuleType;
import dev.su5ed.mffs.api.security.InterdictionMatrix;
import dev.su5ed.mffs.setup.ModCapabilities;
import dev.su5ed.mffs.util.ModUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.util.function.Predicate;

public class ExterminatingModule extends BaseInterdictionModule {
    private final Predicate<EntityLivingBase> predicate;

    public ExterminatingModule(ModuleType<?> type, ItemStack stack, Predicate<EntityLivingBase> predicate) {
        super(type, stack);
        this.predicate = predicate;
    }

    @Override
    public boolean onDefend(InterdictionMatrix interdictionMatrix, EntityLivingBase target) {
        if (this.predicate.test(target)) {
            ModUtil.shockEntity(target, Integer.MAX_VALUE);
            if (MFFSConfig.interdictionMatrixMobKillEnergy > 0) {
                TileEntity be = interdictionMatrix.be();
                if (be.hasCapability(ModCapabilities.FORTRON, null)) {
                    FortronStorage fortron = be.getCapability(ModCapabilities.FORTRON, null);
                    if (fortron != null) {
                        fortron.extractFortron(MFFSConfig.interdictionMatrixMobKillEnergy, false);
                    }
                }
            }
        }
        return false;
    }
}
