package dev.su5ed.mffs.render.particle;

import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2 backport of BeamParticleProvider.
 *
 * Reference (1.20.1): implements {@code ParticleProvider<BeamParticleOptions>}.
 * In 1.12.2: implements {@link IParticleFactory}.
 *
 * Note: In 1.12.2, custom particles are typically spawned directly rather than
 * through the {@code effectRenderer.registerParticle()} factory system, because
 * custom particle IDs outside of {@code EnumParticleTypes} are not supported by
 * vanilla's spawn dispatch. Actual beam spawning is done by
 * {@code ClientPacketHandler} and {@code Fortron} via direct instantiation.
 */
@SideOnly(Side.CLIENT)
public class BeamParticleProvider implements IParticleFactory {
    @Override
    public Particle createParticle(int particleID, World world,
                                   double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed,
                                   int... parameters) {
        // Target is encoded as three int-scaled coordinates in parameters[0..2]
        // when spawned via the factory mechanism.
        Vec3d origin = new Vec3d(x, y, z);
        Vec3d target = parameters.length >= 6
            ? new Vec3d(parameters[0] / 1000.0, parameters[1] / 1000.0, parameters[2] / 1000.0)
            : origin;
        int colorOrd  = parameters.length >= 4 ? parameters[3] : 0;
        int lifetime  = parameters.length >= 5 ? parameters[4] : 20;
        ParticleColor color = colorOrd >= 0 && colorOrd < ParticleColor.values().length
            ? ParticleColor.values()[colorOrd] : ParticleColor.BLUE_BEAM;
        return new BeamParticle(world, origin, target, color, lifetime);
    }
}

/* class_NeoForge_1_21_x (BeamParticleProvider):
package dev.su5ed.mffs.render.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BeamParticleProvider implements ParticleProvider<BeamParticleOptions> {

    @Nullable
    @Override
    public Particle createParticle(BeamParticleOptions options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
        return new BeamParticle(level, new Vec3(x, y, z), options.target(), options.color(), options.lifetime());
    }
}

*/
