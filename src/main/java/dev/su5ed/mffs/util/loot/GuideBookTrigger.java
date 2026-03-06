package dev.su5ed.mffs.util.loot;

/**
 * 1.12.2 backport of GuideBookTrigger.
 *
 * Reference (1.21): extends {@code SimpleCriterionTrigger<TriggerInstance>};
 * fires unconditionally when a player first joins a world and the config option
 * {@code giveGuidebookOnFirstJoin} is enabled, granting the guidebook item.
 *
 * In 1.12.2: no mod-side criterion trigger registration. The guidebook gift on
 * first join is wired through {@code ForgeEventHandler.onEntityJoinWorld()},
 * which checks a per-player NBT flag and gives the book through a direct
 * {@code EntityPlayer.inventory.addItemStackToInventory()} call — no trigger
 * needed. When Patchouli integration is added, the call will similarly be
 * direct.
 * All callers that used to fire this trigger have had the trigger call removed
 * in the 1.12.2 backport.
 * This class is an empty structural placeholder.
 */
public final class GuideBookTrigger {
    private GuideBookTrigger() {}
}

/* class_NeoForge_1_21_x (GuideBookTrigger):
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        trigger(player, instance -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
        public static final TriggerInstance INSTANCE = new TriggerInstance(Optional.empty());
        private static final Codec<TriggerInstance> CODEC = MapCodec.unitCodec(INSTANCE);
    }
}
*/
