package dev.su5ed.mffs.compat;

/**
 * 1.12.2 backport of CreateTrainCompat.
 *
 * Reference (1.21): memoized check for Create mod; detects whether an entity is
 * riding a {@code carriage_contraption} entity (Create train car) so that
 * interdiction matrices do not prevent boarding a train.
 *
 * In 1.12.2: the Create mod does NOT have trains. Train carriages were added in
 * Create 0.5 for Minecraft 1.18+. There is no carriage entity to check against.
 * {@link #isTrainPassenger(net.minecraft.entity.Entity)} always returns
 * {@code false}.
 */
public final class CreateTrainCompat {

    /**
     * Returns {@code false} unconditionally in 1.12.2 — Create trains do not
     * exist in this version.
     */
    public static boolean isTrainPassenger(net.minecraft.entity.Entity entity) {
        return false;
    }

    private CreateTrainCompat() {}
}

/* class_NeoForge_1_21_x (CreateTrainCompat):
package dev.su5ed.mffs.compat;

import com.google.common.base.Suppliers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

import java.util.function.Supplier;

public final class CreateTrainCompat {
    private static final String CREATE_MODID = "create";
    private static final Identifier TRAIN_ENTITY_TYPE = Identifier.fromNamespaceAndPath(CREATE_MODID, "carriage_contraption");
    private static final Supplier<Boolean> CREATE_LOADED = Suppliers.memoize(() -> ModList.get().isLoaded(CREATE_MODID));

    public static boolean isTrainPassenger(Entity entity) {
        if (!CREATE_LOADED.get()) {
            return false;
        }
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            Identifier name = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
            return TRAIN_ENTITY_TYPE.equals(name);
        }
        return false;
    }

    private CreateTrainCompat() {}
}
*/
