package dev.su5ed.mffs.util.loot;

/**
 * 1.12.2 backport of FieldShapeTrigger.
 *
 * Reference (1.21): extends {@code SimpleCriterionTrigger<TriggerInstance>};
 * fires unconditionally (always {@code true}) when a projector field shape is
 * changed, used to award the "change the field shape" advancement.
 *
 * In 1.12.2: no mod-side criterion trigger registration. Advancements are
 * authored as static JSON files in
 * {@code src/main/resources/data/mffs/advancements/}. The shape-change
 * advancement would need to be driven by a different mechanism (e.g. a
 * stat-increment or an NBT-predicate advancement) rather than a custom trigger.
 * All callers that used to fire this trigger have had the trigger call removed
 * in the 1.12.2 backport.
 * This class is an empty structural placeholder.
 */
public final class FieldShapeTrigger {
    private FieldShapeTrigger() {}
}

/* class_NeoForge_1_21_x (FieldShapeTrigger):
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        trigger(player, instance -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
        public static final TriggerInstance INSTANCE = new TriggerInstance(Optional.empty());
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
        ).apply(instance, TriggerInstance::new));

        public static TriggerInstance create() {
            return new TriggerInstance(Optional.empty());
        }
    }
}
*/
