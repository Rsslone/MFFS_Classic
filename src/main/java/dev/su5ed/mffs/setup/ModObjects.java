package dev.su5ed.mffs.setup;

// =============================================================================
// 1.12.2 Backport: TileEntity, Particle, and Advancement registration
// 1.21.x used DeferredRegister<BlockEntityType<?>>, DeferredRegister<ParticleType<?>>,
// and DeferredRegister<CriterionTrigger<?>> (NeoForge). In 1.12.2:
//   - TileEntities are registered via GameRegistry.registerTileEntity() in preInit
//   - Particles use EnumParticleTypes or custom IParticleFactory (no registry)
//   - Advancements/CriterionTriggers do not exist in 1.12.2  (Forge 1.12.2 has
//     no advancement trigger system)
//   - DamageType is not a registry in 1.12.2 (just a string used in DamageSource)
// =============================================================================

import dev.su5ed.mffs.MFFSMod;
import dev.su5ed.mffs.blockentity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class ModObjects {

    /**
     * Call from {@link MFFSMod#preInit} to register all tile entity classes.
     * 1.12.2 uses GameRegistry.registerTileEntity instead of DeferredRegister.
     */
    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(ProjectorBlockEntity.class,           new ResourceLocation(MFFSMod.MODID, "projector"));
        GameRegistry.registerTileEntity(CoercionDeriverBlockEntity.class,     new ResourceLocation(MFFSMod.MODID, "coercion_deriver"));
        GameRegistry.registerTileEntity(FortronCapacitorBlockEntity.class,    new ResourceLocation(MFFSMod.MODID, "fortron_capacitor"));
        GameRegistry.registerTileEntity(ForceFieldBlockEntity.class,          new ResourceLocation(MFFSMod.MODID, "force_field"));
        GameRegistry.registerTileEntity(BiometricIdentifierBlockEntity.class, new ResourceLocation(MFFSMod.MODID, "biometric_identifier"));
        GameRegistry.registerTileEntity(InterdictionMatrixBlockEntity.class,  new ResourceLocation(MFFSMod.MODID, "interdiction_matrix"));
    }

    // DamageSource string key - used when creating DamageSource("mffs.field_shock")
    public static final String FIELD_SHOCK_DAMAGE_TYPE = MFFSMod.MODID + ".field_shock";

    private ModObjects() {}
}
