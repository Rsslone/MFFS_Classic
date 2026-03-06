package dev.su5ed.mffs.util;

/**
 * 1.12.2 backport of FluidRegistryObject.
 *
 * Reference (1.21): holds memoized {@code Supplier<FluidType>},
 * {@code Supplier<Fluid>} (source), and {@code Supplier<Fluid>} (flowing)
 * for use with NeoForge's {@code BaseFlowingFluid} + {@code DeferredRegister}.
 *
 * In 1.12.2: fluid registration is done directly via {@link net.minecraftforge.fluids.FluidRegistry}.
 * The Fortron fluid is registered in {@link dev.su5ed.mffs.setup.ModFluids}.
 * This class is an empty structural placeholder.
 */
public final class FluidRegistryObject {
    private FluidRegistryObject() {}
}

/* class_NeoForge_1_21_x (FluidRegistryObject):
package dev.su5ed.mffs.util;

import com.google.common.base.Suppliers;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Supplier;

public class FluidRegistryObject {
    private final Supplier<FluidType> fluidType;
    private final Supplier<Fluid> sourceFluid;
    private final Supplier<Fluid> flowingFluid;
    private final ModFluidType.FluidRenderInfo properties;

    public FluidRegistryObject(ModFluidType.FluidProperties properties) {
        this.properties = properties.build();
        this.fluidType = Suppliers.memoize(() -> new ModFluidType(properties));

        BaseFlowingFluid.Properties fluidProperties = new BaseFlowingFluid.Properties(this.fluidType, this::getSourceFluid, this::getFlowingFluid);
        this.sourceFluid = Suppliers.memoize(() -> new BaseFlowingFluid.Source(fluidProperties));
        this.flowingFluid = Suppliers.memoize(() -> new BaseFlowingFluid.Flowing(fluidProperties));
    }

    public ModFluidType.FluidRenderInfo getProperties() {
        return this.properties;
    }

    public Supplier<FluidType> fluidType() {
        return this.fluidType;
    }

    public Supplier<Fluid> sourceFluid() {
        return this.sourceFluid;
    }

    public Supplier<Fluid> flowingFluid() {
        return this.flowingFluid;
    }

    public Fluid getSourceFluid() {
        return this.sourceFluid.get();
    }

    public Fluid getFlowingFluid() {
        return this.flowingFluid.get();
    }
}

*/
