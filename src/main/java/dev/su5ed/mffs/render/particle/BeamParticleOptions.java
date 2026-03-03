package dev.su5ed.mffs.render.particle;

// TODO: Not yet backported to 1.12.2 (Phase 13 - Rendering).
public final class BeamParticleOptions {
    private BeamParticleOptions() {}
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
