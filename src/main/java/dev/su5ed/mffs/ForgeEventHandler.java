package dev.su5ed.mffs;

// =============================================================================
// 1.12.2 Backport: Forge event handler
// 1.21.x used NeoForge bus events; here we use Forge 1.12.2 event types.
// Key changes:
//   MobSpawnEvent.PositionCheck   -> LivingSpawnEvent.CheckSpawn
//   EntityJoinLevelEvent          -> EntityJoinWorldEvent
//   PlayerInteractEvent.RightClickBlock / LeftClickBlock (1.12.2 subclasses)
//   ServerStartingEvent           -> FMLServerStartingEvent (handled in MFFSMod)
//   Level / ServerLevel           -> World / WorldServer
//   ICancellableEvent             -> standard Forge setCanceled
//   ServerPlayer                  -> EntityPlayerMP
//   block.get().is(other)         -> block == ModBlocks.FORCE_FIELD (direct ref)
//   Guidebook criterion trigger   -> EntityJoinWorldEvent + player.getEntityData() NBT flag
// =============================================================================

import dev.su5ed.mffs.api.EventForceManipulate;
import dev.su5ed.mffs.api.security.FieldPermission;
import dev.su5ed.mffs.api.security.InterdictionMatrix;
import dev.su5ed.mffs.blockentity.FortronBlockEntity;
import dev.su5ed.mffs.setup.ModBlocks;
import dev.su5ed.mffs.setup.ModModules;
import dev.su5ed.mffs.util.Fortron;
import dev.su5ed.mffs.util.ModUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class ForgeEventHandler {

    // Note: FMLServerStartingEvent is an FML lifecycle event, not a Forge bus event.
    // It is handled in MFFSMod.serverStarting() instead.

    @SubscribeEvent
    public void eventPreForceManipulate(EventForceManipulate.EventPreForceManipulate event) {
        TileEntity te = event.getWorld().getTileEntity(event.getBeforePos());
        if (te instanceof FortronBlockEntity fortronBlockEntity) {
            fortronBlockEntity.setMarkSendFortron(false);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        onPlayerInteractInternal(event, event.getEntityPlayer(), event.getWorld(), event.getPos(), Fortron.Action.RIGHT_CLICK_BLOCK);
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        // If the block is a force field, cancel the interaction entirely
        if (world.getBlockState(pos).getBlock() == ModBlocks.FORCE_FIELD) {
            event.setCanceled(true);
        } else {
            onPlayerInteractInternal(event, event.getEntityPlayer(), world, pos, Fortron.Action.LEFT_CLICK_BLOCK);
        }
    }

    @SubscribeEvent
    public void livingSpawnEvent(LivingSpawnEvent.CheckSpawn event) {
        BlockPos pos = new BlockPos(MathHelper.floor(event.getX()), MathHelper.floor(event.getY()), MathHelper.floor(event.getZ()));
        InterdictionMatrix interdictionMatrix = Fortron.getNearestInterdictionMatrix(event.getWorld(), pos);
        if (interdictionMatrix != null && interdictionMatrix.hasModule(ModModules.ANTI_SPAWN)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!MFFSConfig.giveGuidebookOnFirstJoin) return;
        if (event.getWorld().isRemote) return;
        if (!(event.getEntity() instanceof EntityPlayerMP)) return;
        if (!Loader.isModLoaded("patchouli")) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
        NBTTagCompound data = player.getEntityData();
        if (data.getBoolean("mffs_receivedHandbook")) return;

        Item bookItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("patchouli", "guide_book"));
        if (bookItem == null) return;

        data.setBoolean("mffs_receivedHandbook", true);

        ItemStack stack = new ItemStack(bookItem);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("patchouli:book", MFFSMod.MODID + ":handbook");
        stack.setTagCompound(tag);
        player.inventory.addItemStackToInventory(stack);
    }

    private void onPlayerInteractInternal(PlayerInteractEvent event, EntityPlayer player, World world, BlockPos pos, Fortron.Action action) {
        if (!player.isCreative()) {
            InterdictionMatrix interdictionMatrix = Fortron.getNearestInterdictionMatrix(world, pos);
            if (interdictionMatrix != null) {
                if (world.getBlockState(pos).getBlock() == ModBlocks.BIOMETRIC_IDENTIFIER
                    && Fortron.isPermittedByInterdictionMatrix(interdictionMatrix, player, FieldPermission.CONFIGURE_SECURITY_CENTER)) {
                    return;
                }
                if (!Fortron.hasPermission(world, pos, interdictionMatrix, action, player)) {
                    player.sendStatusMessage(ModUtil.translate("info", "interdiction_matrix.no_permission", interdictionMatrix.getTitle()), false);
                    event.setCanceled(true);
                }
            }
        }
    }
}
