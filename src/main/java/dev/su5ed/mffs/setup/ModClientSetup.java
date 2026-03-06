package dev.su5ed.mffs.setup;

// =============================================================================
// 1.12.2 Backport: Client-side setup
// 1.21.x used @EventBusSubscriber(Dist.CLIENT) with NeoForge client events.
// In 1.12.2 client-side setup is done via a client-only proxy class
// (annotated with @SidedProxy) that implements an IProxy interface,
// OR via @Mod.EventBusSubscriber(value = Side.CLIENT) on a class that
// registers listeners on the FML event bus.
//
// Key replacements:
//   FMLClientSetupEvent               -> FMLInitializationEvent (client side)
//   RegisterMenuScreensEvent          -> GameRegistry + IGuiHandler
//   EntityRenderersEvent.Register*    -> ClientRegistry.bindTileEntitySpecialRenderer
//   RegisterParticleProvidersEvent    -> IParticleFactory registered in preInit
//   EntityRenderersEvent.Layers       -> ModelBakeEvent / TextureStitchEvent
//   RegisterColorHandlersEvent.Block  -> ColorHandlerEvent.Block
//   RegisterClientExtensionsEvent     -> not applicable (NeoForge-specific)
//   GuiGraphics                       -> GlStateManager / Gui
// =============================================================================

import dev.su5ed.mffs.MFFSMod;
import dev.su5ed.mffs.blockentity.BiometricIdentifierBlockEntity;
import dev.su5ed.mffs.blockentity.CoercionDeriverBlockEntity;
import dev.su5ed.mffs.blockentity.ForceFieldBlockEntity;
import dev.su5ed.mffs.blockentity.FortronBlockEntity;
import dev.su5ed.mffs.blockentity.ProjectorBlockEntity;
import dev.su5ed.mffs.render.BiometricIdentifierRenderer;
import dev.su5ed.mffs.render.CoercionDeriverRenderer;
import dev.su5ed.mffs.render.ForceFieldBlockEntityRenderer;
import dev.su5ed.mffs.render.ProjectorRenderer;
import dev.su5ed.mffs.setup.ModBlocks;
import dev.su5ed.mffs.setup.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = MFFSMod.MODID, value = Side.CLIENT)
public final class ModClientSetup {

    /**
     * Register item model/texture mappings for all MFFS items.
     * In 1.12.2, items don't automatically pick up model JSONs — each one
     * must be explicitly bound via ModelLoader.setCustomModelResourceLocation().
     */
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        // Bind TileEntity Special Renderers for rotating accents
        ClientRegistry.bindTileEntitySpecialRenderer(CoercionDeriverBlockEntity.class, new CoercionDeriverRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(ProjectorBlockEntity.class, new ProjectorRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(BiometricIdentifierBlockEntity.class, new BiometricIdentifierRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(ForceFieldBlockEntity.class, new ForceFieldBlockEntityRenderer());

        // Block items
        registerBlockItemModel(ModBlocks.PROJECTOR);
        registerBlockItemModel(ModBlocks.COERCION_DERIVER);
        registerBlockItemModel(ModBlocks.FORTRON_CAPACITOR);
        registerBlockItemModel(ModBlocks.BIOMETRIC_IDENTIFIER);
        registerBlockItemModel(ModBlocks.INTERDICTION_MATRIX);
        // No ItemBlock for FORCE_FIELD — not player-obtainable

        // Tools, cards, materials
        registerItemModel(ModItems.REMOTE_CONTROLLER_ITEM);
        registerItemModel(ModItems.FREQUENCY_CARD);
        registerItemModel(ModItems.ID_CARD);
        registerItemModel(ModItems.BLANK_CARD);
        registerItemModel(ModItems.INFINITE_POWER_CARD);
        registerItemModel(ModItems.FOCUS_MATRIX);
        registerItemModel(ModItems.STEEL_COMPOUND);
        registerItemModel(ModItems.STEEL_INGOT);
        registerItemModel(ModItems.BATTERY);

        // Projector modes
        registerItemModel(ModItems.CUBE_MODE);
        registerItemModel(ModItems.SPHERE_MODE);
        registerItemModel(ModItems.TUBE_MODE);
        registerItemModel(ModItems.PYRAMID_MODE);
        registerItemModel(ModItems.CYLINDER_MODE);
        registerItemModel(ModItems.CUSTOM_MODE);

        // Field / general modules
        registerItemModel(ModItems.TRANSLATION_MODULE);
        registerItemModel(ModItems.SCALE_MODULE);
        registerItemModel(ModItems.ROTATION_MODULE);
        registerItemModel(ModItems.SPEED_MODULE);
        registerItemModel(ModItems.CAPACITY_MODULE);
        registerItemModel(ModItems.FUSION_MODULE);
        registerItemModel(ModItems.DOME_MODULE);
        registerItemModel(ModItems.CAMOUFLAGE_MODULE);
        registerItemModel(ModItems.DISINTEGRATION_MODULE);
        registerItemModel(ModItems.SHOCK_MODULE);
        registerItemModel(ModItems.GLOW_MODULE);
        registerItemModel(ModItems.SPONGE_MODULE);
        registerItemModel(ModItems.STABILIZATION_MODULE);
        registerItemModel(ModItems.COLLECTION_MODULE);
        registerItemModel(ModItems.INVERTER_MODULE);
        registerItemModel(ModItems.SILENCE_MODULE);

        // Interdiction matrix modules
        registerItemModel(ModItems.WARN_MODULE);
        registerItemModel(ModItems.BLOCK_ACCESS_MODULE);
        registerItemModel(ModItems.BLOCK_ALTER_MODULE);
        registerItemModel(ModItems.ANTI_FRIENDLY_MODULE);
        registerItemModel(ModItems.ANTI_HOSTILE_MODULE);
        registerItemModel(ModItems.ANTI_PERSONNEL_MODULE);
        registerItemModel(ModItems.ANTI_SPAWN_MODULE);
        registerItemModel(ModItems.CONFISCATION_MODULE);
    }

    @SubscribeEvent
    public static void onBlockColorHandler(ColorHandlerEvent.Block event) {
        event.getBlockColors().registerBlockColorHandler(
            (state, access, pos, tintIndex) -> {
                if (access != null && pos != null) {
                    TileEntity te = access.getTileEntity(pos);
                    if (te instanceof ForceFieldBlockEntity forceField) {
                        IBlockState camo = forceField.getCamouflage();
                        if (camo != null) {
                            return event.getBlockColors().colorMultiplier(camo, access, pos, tintIndex);
                        }
                    }
                }
                return 0x34FEFF; // default cyan tint matching reference
            },
            ModBlocks.FORCE_FIELD
        );
    }

    private static void registerItemModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
            new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }

    private static void registerBlockItemModel(Block block) {
        Item item = Item.getItemFromBlock(block);
        if (item != null) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
    }

    private ModClientSetup() {}
}
