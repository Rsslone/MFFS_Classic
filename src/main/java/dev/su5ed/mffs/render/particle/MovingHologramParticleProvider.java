package dev.su5ed.mffs.render.particle;

import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2 backport of MovingHologramParticleProvider.
 *
 * Reference (1.20.1): implements {@code ParticleProvider<MovingHologramParticleOptions>}.
 * In 1.12.2: implements {@link IParticleFactory}.
 *
 * Note: Actual hologram particle spawning is done by {@code ClientPacketHandler}
 * via direct instantiation of {@link MovingHologramParticle}.
 */
@SideOnly(Side.CLIENT)
public class MovingHologramParticleProvider implements IParticleFactory {
    @Override
    public Particle createParticle(int particleID, World world,
                                   double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed,
                                   int... parameters) {
        int colorOrd = parameters.length >= 1 ? parameters[0] : 0;
        int lifetime = parameters.length >= 2 ? parameters[1] : 20;
        ParticleColor color = colorOrd >= 0 && colorOrd < ParticleColor.values().length
            ? ParticleColor.values()[colorOrd] : ParticleColor.WHITE;
        return new MovingHologramParticle(world, new Vec3d(x, y, z), color, lifetime);
    }
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
