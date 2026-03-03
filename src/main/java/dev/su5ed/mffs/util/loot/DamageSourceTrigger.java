package dev.su5ed.mffs.util.loot;

// TODO: 1.12.2 advancement trigger not yet implemented.
// In 1.12.2, this would use ICriterionTrigger<ICriterionInstance> pattern.
// Currently unused since shockEntity() no longer fires advancement triggers.
public final class DamageSourceTrigger {
    private DamageSourceTrigger() {}
}

/* class_NeoForge_1_21_x (DamageSourceTrigger):

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ResourceKey<DamageType> damageType) {
        // If it is just any damage regardless of killed or the player is dead (or is on hardcore and used up a totem of undying)
        // And the damage source matches
        trigger(player, instance -> (!instance.killed || player.isDeadOrDying()) && instance.damageType.equals(damageType));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceKey<DamageType> damageType, boolean killed) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            ResourceKey.codec(Registries.DAMAGE_TYPE).fieldOf("damageType").forGetter(TriggerInstance::damageType),
            Codec.BOOL.fieldOf("killed").forGetter(TriggerInstance::killed)
        ).apply(instance, TriggerInstance::new));

        public static TriggerInstance killed(ResourceKey<DamageType> damageType) {
            return new TriggerInstance(Optional.empty(), damageType, true);
        }
    }
}
*/
