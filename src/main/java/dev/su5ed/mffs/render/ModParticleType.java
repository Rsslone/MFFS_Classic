package dev.su5ed.mffs.render;

/**
 * 1.12.2 backport of ModParticleType.
 *
 * Reference (1.20.1): extends {@code ParticleType<T extends ParticleOptions>},
 * registered via {@code DeferredRegister<ParticleType<?>>} in ModObjects.
 *
 * In 1.12.2 there is no {@code ParticleType} registry. Custom particles are
 * spawned by directly instantiating the particle class and adding it to
 * {@code Minecraft.getMinecraft().effectRenderer}. The particle type
 * registration done by this class in 1.20.1 is handled by the direct
 * instantiation in {@code ClientPacketHandler} and
 * {@code dev.su5ed.mffs.util.Fortron}.
 *
 * This class is retained as a structural placeholder.
 */
public final class ModParticleType {
    private ModParticleType() {}
}

/* class_NeoForge_1_21_x (ModParticleType):
package dev.su5ed.mffs.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ModParticleType<T extends ParticleOptions> extends ParticleType<T> {
    private final MapCodec<T> codec;
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;

    public ModParticleType(boolean overrideLimiter, MapCodec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        super(overrideLimiter);

        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    @Override
    public MapCodec<T> codec() {
        return this.codec;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
        return this.streamCodec;
    }
}

*/
