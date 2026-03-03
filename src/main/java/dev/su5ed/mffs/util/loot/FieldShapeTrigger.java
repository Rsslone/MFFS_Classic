package dev.su5ed.mffs.util.loot;

// TODO: 1.12.2 advancement trigger not yet implemented.
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
