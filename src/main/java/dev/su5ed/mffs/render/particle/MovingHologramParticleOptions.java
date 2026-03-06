package dev.su5ed.mffs.render.particle;

/**
 * 1.12.2 backport of MovingHologramParticleOptions.
 *
 * Reference (1.20.1): implements {@code ParticleOptions} for use with the
 * {@code ParticleType} registry.
 * In 1.12.2 there is no {@code ParticleType} registry or {@code ParticleOptions}.
 * Particles are directly instantiated and added to the effect renderer.
 *
 * This is a plain data holder for {@link MovingHologramParticle} construction
 * parameters.
 */
public final class MovingHologramParticleOptions {
    /** Default-parameter singleton (matches reference {@code DEFAULT}). */
    public static final MovingHologramParticleOptions DEFAULT =
        new MovingHologramParticleOptions(ParticleColor.WHITE, 20);

    public final ParticleColor color;
    public final int lifetime;

    public MovingHologramParticleOptions(ParticleColor color, int lifetime) {
        this.color    = color;
        this.lifetime = lifetime;
    }
}

/* class_NeoForge_1_21_x (MovingHologramParticleOptions):
package dev.su5ed.mffs.render.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.su5ed.mffs.setup.ModObjects;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;

public record MovingHologramParticleOptions(ParticleColor color, int lifetime) implements ParticleOptions {
    public static final MovingHologramParticleOptions DEFAULT = new MovingHologramParticleOptions(ParticleColor.WHITE, 20);
    public static final Codec<MovingHologramParticleOptions> CODEC = MapCodec.unitCodec(DEFAULT);
    public static final StreamCodec<ByteBuf, MovingHologramParticleOptions> STREAM_CODEC = StreamCodec.unit(DEFAULT);

    @Override
    public ParticleType<?> getType() {
        return ModObjects.MOVING_HOLOGRAM_PARTICLE.get();
    }
}

*/
