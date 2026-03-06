package dev.su5ed.mffs.compat;

/**
 * 1.12.2 backport of MFFSProbeProvider.
 *
 * Reference (1.21): implements {@code IProbeInfoProvider} (The One Probe API)
 * to show camouflage block info and FE/Fortron readouts on block hover.
 *
 * In 1.12.2: The One Probe (mcjty.theoneprobe) is available for 1.12.2 and uses
 * a compatible but different API:
 * <ul>
 *   <li>{@code addProbeInfo(ProbeMode, IProbeInfo, EntityPlayer, World, IBlockState, IProbeHitData)}</li>
 *   <li>Registered via IMC: {@code FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", ...)}</li>
 * </ul>
 * A full port requires the TOP 1.12.2 API jar on the compile classpath.
 * This class is an empty structural placeholder until that dependency is added.
 */
public final class MFFSProbeProvider {
    private MFFSProbeProvider() {}
}

/* class_NeoForge_1_21_x (MFFSProbeProvider):
package dev.su5ed.mffs.compat;

import dev.su5ed.mffs.MFFSMod;
import dev.su5ed.mffs.blockentity.ForceFieldBlockEntity;
import mcjty.theoneprobe.api.*;
import mcjty.theoneprobe.apiimpl.ProbeHitData;
import mcjty.theoneprobe.apiimpl.elements.ElementProgress;
import mcjty.theoneprobe.apiimpl.providers.DefaultProbeInfoProvider;
import mcjty.theoneprobe.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

// TODO
public class MFFSProbeProvider {
    private static final Identifier ID = MFFSMod.location("probe");

    public Void apply(ITheOneProbe probe) {
//        probe.registerBlockDisplayOverride(this);
        return null;
    }

    public boolean overrideStandardInfo(ProbeMode mode, IProbeInfo info, Player player, Level level, BlockState blockState, IProbeHitData data) {
        BlockPos pos = data.getPos();
        // Override info for camouflaged blocks
        if (level.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK) instanceof ForceFieldBlockEntity forceField) {
            BlockState camo = forceField.getCamouflage();
            if (camo != null) {
                // TODO Test?
                ItemStack clone = camo.getCloneItemStack(pos, level, true, player);
                DefaultProbeInfoProvider.showStandardBlockInfo(Config.getRealConfig(), mode, info, camo, camo.getBlock(), level, pos, player, new ProbeHitData(data.getPos(), data.getHitVec(), data.getSideHit(), clone));
                return true;
            }
        }
        return false;
    }

//    @Override
//    public Identifier getID() {
//        return ID;
//    }

    public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level, BlockState blockState, IProbeHitData data) {
//        BlockEntity be = level.getChunkAt(data.getPos()).getBlockEntity(data.getPos(), LevelChunk.EntityCreationType.CHECK);
//        if (be != null) {
//            if (be instanceof ElectricTileEntity electric) {
//                // Special handling for FE as we don't want to expose the cap on the null side, but still show TOP info 
//                IEnergyStorage storage = electric.getGlobalEnergyStorage();
//                addEnergyInfo(info, Config.getRealConfig(), storage.getEnergyStored(), storage.getMaxEnergyStored());
//            }
//            // TODO Show Fortron
//        }
    }

    // Copy of DefaultProbeInfoProvider#addEnergyInfo because original method is private..
    private void addEnergyInfo(IProbeInfo probeInfo, IProbeConfig config, long energy, long maxEnergy) {
        if (config.getRFMode() == 1) {
            probeInfo.progress(
                energy,
                maxEnergy,
                probeInfo.defaultProgressStyle()
                    .suffix("FE")
                    .filledColor(Config.rfbarFilledColor)
                    .alternateFilledColor(Config.rfbarAlternateFilledColor)
                    .borderColor(Config.rfbarBorderColor)
                    .numberFormat(Config.rfFormat.get())
            );
        } else {
            probeInfo.text(
                CompoundText.create()
                    .style(TextStyleClass.PROGRESS)
                    .text("FE: " + ElementProgress.format(energy, Config.rfFormat.get(), Component.literal("FE")))
            );
        }
    }
}

*/
