package dev.su5ed.mffs.render.particle;

import net.minecraft.util.math.Vec3d;

/**
 * 1.12.2 backport of BeamParticleOptions.
 *
 * Reference (1.20.1): implements {@code ParticleOptions} for use with the
 * {@code ParticleType} registry ({@code DeferredRegister<ParticleType<?>>}).
 * In 1.12.2 there is no {@code ParticleType} registry or {@code ParticleOptions}
 * interface. Particles are directly instantiated and added to the effect renderer.
 *
 * This class is a plain data holder for the parameters required to construct a
 * {@link BeamParticle}. It is not used by the particle-spawn mechanism itself,
 * but can be used to bundle parameters when passing them between methods.
 */
public final class BeamParticleOptions {
    /** Default-parameter singleton (matches reference {@code DEFAULT}). */
    public static final BeamParticleOptions DEFAULT =
        new BeamParticleOptions(Vec3d.ZERO, ParticleColor.BLUE_BEAM, 20);

    public final Vec3d target;
    public final ParticleColor color;
    public final int lifetime;

    public BeamParticleOptions(Vec3d target, ParticleColor color, int lifetime) {
        this.target   = target;
        this.color    = color;
        this.lifetime = lifetime;
    }
}

/* class_NeoForge_1_21_x (BeamParticleOptions):
package dev.su5ed.mffs.render.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.su5ed.mffs.setup.ModObjects;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record BeamParticleOptions(Vec3 target, ParticleColor color, int lifetime) implements ParticleOptions {
    public static final BeamParticleOptions DEFAULT = new BeamParticleOptions(Vec3.ZERO, ParticleColor.BLUE_BEAM, 20);
    public static final Codec<BeamParticleOptions> CODEC = MapCodec.unitCodec(DEFAULT);
    public static final StreamCodec<ByteBuf, BeamParticleOptions> STREAM_CODEC = StreamCodec.unit(DEFAULT);

    @Override
    public ParticleType<?> getType() {
        return ModObjects.BEAM_PARTICLE.get();
    }
}

*/
