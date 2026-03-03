package dev.su5ed.mffs.util.loot;

// TODO: 1.12.2 advancement trigger not yet implemented.
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
