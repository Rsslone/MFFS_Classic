package dev.su5ed.mffs.setup;

// =============================================================================
// 1.12.2 Backport: Tag / OreDictionary
// 1.21.x used TagKey<Item> and TagKey<Block> (data-driven, requires datagen).
// In 1.12.2 item tags are replaced by OreDictionary string constants.
// Block membership is checked by iterating OreDictionary entries.
//
// Usage (registering, typically in postInit or via @SubscribeEvent OreRegisterEvent):
//   OreDictionary.registerOre(ModTags.FORTRON_FUEL, someItem);
//   OreDictionary.registerOre(ModTags.INGOTS_STEEL, ModItems.STEEL_INGOT);
// Usage (checking):
//   boolean isFuel = OreDictionary.containsMatch(false, OreDictionary.getOres(ModTags.FORTRON_FUEL), stack);
// =============================================================================

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ModTags {

    // Item OreDictionary names
    // 1.21.x: TagKey<Item> FORTRON_FUEL  = itemTag("fortron_fuel")
    public static final String FORTRON_FUEL  = "fortronFuel";

    // 1.21.x: TagKey<Item> INGOTS_STEEL  = cItemTag("ingots/steel")  (common tag)
    public static final String INGOTS_STEEL  = "ingotSteel";

    // Block sets — 1.12.2 equivalent of block tags
    // 1.21.x: TagKey<Block> FORCEFIELD_REPLACEABLE — blocks that force fields can replace
    private static Set<Block> FORCEFIELD_REPLACEABLE;

    /**
     * Get the set of blocks that force fields can replace when projecting.
     * Includes snow, vines, tall/short grass, dead bushes, etc.
     * Lazily initialized to avoid class loading issues.
     */
    public static Set<Block> getForceFieldReplaceable() {
        if (FORCEFIELD_REPLACEABLE == null) {
            Set<Block> set = new HashSet<>();
            set.add(Blocks.SNOW_LAYER);
            set.add(Blocks.SNOW);
            set.add(Blocks.VINE);
            set.add(Blocks.TALLGRASS);
            set.add(Blocks.DEADBUSH);
            set.add(Blocks.DOUBLE_PLANT); // tall grass, sunflowers, etc.
            // 1.12.2 doesn't have glow_lichen, seagrass, kelp, but waterlily is similar
            set.add(Blocks.WATERLILY);
            FORCEFIELD_REPLACEABLE = Collections.unmodifiableSet(set);
        }
        return FORCEFIELD_REPLACEABLE;
    }

    // Block sets not yet defined, check via IBlockState properties if needed:
    // 1.21.x: TagKey<Block> STABILIZATION_BLACKLIST
    // 1.21.x: TagKey<Block> DISINTEGRATION_BLACKLIST

    private ModTags() {}
}
