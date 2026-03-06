package dev.su5ed.mffs.util.loot;

/**
 * 1.12.2 backport of MenuInventoryTrigger.
 *
 * Reference (1.21): extends {@code SimpleCriterionTrigger<TriggerInstance>};
 * fires when a player has a specific {@code MenuType} open with the given
 * items present in the menu's inventory. Used by {@code AdvancementsGen} to
 * award the "use custom camouflage" advancement.
 *
 * In 1.12.2: the advancement / criterion trigger system exists but
 * {@code SimpleCriterionTrigger} and codec-based {@code TriggerInstance}
 * are 1.19+ concepts. The entire datagen layer is a no-op placeholder in the
 * 1.12.2 backport (JSON advancement files are authored manually in
 * src/main/resources/). This class is an empty structural placeholder.
 */
public final class MenuInventoryTrigger {
    private MenuInventoryTrigger() {}
}

/* class_NeoForge_1_21_x (MenuInventoryTrigger):
package dev.su5ed.mffs.util.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.criterion.*;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class MenuInventoryTrigger extends SimpleCriterionTrigger<MenuInventoryTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, boolean active, ResourceHandler<ItemResource> itemHandler) {
        trigger(player, instance -> {
            if (instance.active != active) {
                return false;
            }
            List<ItemPredicate> list = new ObjectArrayList<>(instance.items);
            for (int i = 0; i < itemHandler.size(); i++) {
                ItemResource stackInSlot = itemHandler.getResource(i);
                list.removeIf(predicate -> predicate.test(stackInSlot.toStack()));
            }
            return list.isEmpty();
        });
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, MenuType<?> menuType, boolean active, List<ItemPredicate> items) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            BuiltInRegistries.MENU.byNameCodec().fieldOf("menuType").forGetter(TriggerInstance::menuType),
            Codec.BOOL.fieldOf("active").forGetter(TriggerInstance::active),
            ItemPredicate.CODEC.listOf().fieldOf("items").forGetter(TriggerInstance::items)
        ).apply(instance, TriggerInstance::new));

        @SafeVarargs
        public static TriggerInstance create(MenuType<?> menuType, boolean active, Holder<Item>... items) {
            List<ItemPredicate> predicates = Stream.of(items)
                .map(holder -> new ItemPredicate(Optional.of(HolderSet.direct(holder)), MinMaxBounds.Ints.ANY, DataComponentMatchers.ANY))
                .toList();
            return new TriggerInstance(Optional.empty(), menuType, active, predicates);
        }
    }
}

*/
