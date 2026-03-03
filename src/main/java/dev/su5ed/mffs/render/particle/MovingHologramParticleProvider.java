package dev.su5ed.mffs.render.particle;

// TODO: Not yet backported to 1.12.2 (Phase 13 - Rendering).
public final class MovingHologramParticleProvider {
    private MovingHologramParticleProvider() {}
}

/* class_NeoForge_1_21_x (MovingHologramParticleProvider):
package dev.su5ed.mffs.render.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MovingHologramParticleProvider implements ParticleProvider<MovingHologramParticleOptions> {

    @Nullable
    @Override
    public Particle createParticle(MovingHologramParticleOptions options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
        return new MovingHologramParticle(level, new Vec3(x, y, z), options.color(), options.lifetime());
    }
}

*/
