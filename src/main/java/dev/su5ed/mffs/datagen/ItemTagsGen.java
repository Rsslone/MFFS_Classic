package dev.su5ed.mffs.datagen;

// 1.12.2 does not use NeoForge DataProvider API.
// JSON resources live in src/main/resources/ directly.
// Reference (1.21): extends ItemTagsProvider — adds FORTRON_FUEL tag (lapis, quartz)
// and INGOTS_STEEL tag (steel ingot).

public final class ItemTagsGen {
    private ItemTagsGen() {}
}

/* class_NeoForge_1_21_x (ItemTagsGen):
package dev.su5ed.mffs.datagen;

// TODO: Not yet backported to 1.12.2 (Phase 15/16).
public final class ItemTagsGen {
    private ItemTagsGen() {}
}

/* class_NeoForge_1_21_x (ItemTagsGen):
package dev.su5ed.mffs.datagen;

import dev.su5ed.mffs.MFFSMod;
import dev.su5ed.mffs.setup.ModItems;
import dev.su5ed.mffs.setup.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ItemTagsGen extends ItemTagsProvider {

    public ItemTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, MFFSMod.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        tag(ModTags.FORTRON_FUEL).add(Items.LAPIS_LAZULI, Items.QUARTZ);
        tag(ModTags.INGOTS_STEEL).add(ModItems.STEEL_INGOT.get());
    }

    @Override
    public String getName() {
        return MFFSMod.NAME + " Item Tags";
    }
}

* /

*/
