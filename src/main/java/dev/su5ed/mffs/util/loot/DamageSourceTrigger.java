package dev.su5ed.mffs.util.loot;

/**
 * 1.12.2 backport of DamageSourceTrigger.
 *
 * Reference (1.21): extends {@code SimpleCriterionTrigger<TriggerInstance>};
 * fires when a player takes damage matching a specific
 * {@code ResourceKey<DamageType>} (and optionally dies from it), used to
 * award the "shocked by a force field" advancement.
 *
 * In 1.12.2:
 * <ul>
 *   <li>Damage types are plain string keys on {@code DamageSource}, not a
 *       registry. The shock damage is created via
 *       {@code new DamageSource("mffs.field_shock")}.</li>
 *   <li>The 1.12.2 advancement/trigger framework ({@code IJsonSerializable}
 *       + {@code ICriteriaTrigger}) exists in principle but is far more
 *       limited and rarely used by mods. Advancements are authored as static
 *       JSON files; custom triggers require nontrivial boilerplate.</li>
 *   <li>All callers that used to fire this trigger have had the trigger call
 *       removed in the 1.12.2 backport.</li>
 * </ul>
 * This class is an empty structural placeholder.
 */
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
