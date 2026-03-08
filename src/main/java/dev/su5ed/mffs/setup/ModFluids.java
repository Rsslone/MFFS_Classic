package dev.su5ed.mffs.setup;

/**
 * 1.12.2 backport of ModFluids.
 *
 * In 1.21.x (NeoForge), FORTRON_FLUID was a FluidRegistryObject backed by
 * NeoForge BaseFlowingFluid + DeferredRegister. Used internally via
 * FluidTank-based Fortron storage and hidden from JEI.
 *
 * In the 1.12.2 backport, Fortron storage is pure-integer (FortronStorageImpl).
 * No FluidTank or FluidStack is used anywhere, so no Forge fluid is needed.
 * Registering one would create an orphaned bucket item ("fluid.fortron") with
 * no texture in NEI/JEI.
 */
public final class ModFluids {
    private ModFluids() {}
}
