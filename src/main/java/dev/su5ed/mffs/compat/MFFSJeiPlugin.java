package dev.su5ed.mffs.compat;

/**
 * 1.12.2 backport of MFFSJeiPlugin.
 *
 * Reference (1.21, NeoForge):
 * <ul>
 *   <li>{@code @JeiPlugin} annotation + {@code IModPlugin} from JEI NeoForge API</li>
 *   <li>Hides the Fortron fluid from JEI ingredient list</li>
 *   <li>Registers {@link BasicGhostIngredientHandler} for {@code InterdictionMatrixScreen}</li>
 * </ul>
 *
 * In 1.12.2: JEI uses {@code mezz.jei.api.IModPlugin} / {@code @JEIPlugin}
 * with a different registration lifecycle. The fluid hiding API and ghost
 * ingredient system differ significantly.
 * A full port requires the JEI 1.12.2 API jar on the compile classpath.
 * This class is an empty structural placeholder until that dependency is added.
 */
public final class MFFSJeiPlugin {
    private MFFSJeiPlugin() {}
}

/* class_NeoForge_1_21_x (MFFSJeiPlugin):
package dev.su5ed.mffs.compat;

import dev.su5ed.mffs.screen.InterdictionMatrixScreen;
import dev.su5ed.mffs.setup.ModFluids;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.List;

import static dev.su5ed.mffs.MFFSMod.location;

@JeiPlugin
public class MFFSJeiPlugin implements IModPlugin {
    public static final Identifier PLUGIN_ID = location("jei_plugin");

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        // Hide Fortron from JEI as it's not a crafting fluid
        runtime.getIngredientManager().removeIngredientsAtRuntime(
            NeoForgeTypes.FLUID_STACK,
            List.of(new FluidStack(ModFluids.FORTRON_FLUID.get(), FluidType.BUCKET_VOLUME))
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(InterdictionMatrixScreen.class, new BasicGhostIngredientHandler<>());
    }

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }
}
*/
