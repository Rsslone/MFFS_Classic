package dev.su5ed.mffs.setup;

import dev.su5ed.mffs.MFFSMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * Registers the Fortron fluid.
 * In 1.21.x this used DeferredRegister + NeoForge FluidType.
 * In 1.12.2, fluids are registered directly via {@link FluidRegistry}.
 */
public final class ModFluids {

    public static final Fluid FORTRON = new Fluid(
        "fortron",
        new ResourceLocation(MFFSMod.MODID, "fluid/fortron"),
        new ResourceLocation(MFFSMod.MODID, "fluid/fortron")
    ).setDensity(1000).setLuminosity(8);

    /**
     * Register fluids. Must be called during preInit before block/item registration.
     */
    public static void register() {
        FluidRegistry.registerFluid(FORTRON);
        FluidRegistry.addBucketForFluid(FORTRON);
    }

    private ModFluids() {}
}
