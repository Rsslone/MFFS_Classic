package dev.su5ed.mffs.render.particle;

/**
 * 1.12.2 backport of ModParticleRenderType.
 *
 * Reference (1.20.1): constants of type {@code IParticleRenderType} controlling
 * how particles are batched (e.g. {@code PARTICLE_SHEET_TRANSLUCENT},
 * {@code PARTICLE_SHEET_LIT}, {@code CUSTOM}).
 *
 * In 1.12.2, particle render type is controlled by {@code Particle.getFXLayer()}:
 * <ul>
 *   <li>0 — standard translucent particle sheet (particles.png)</li>
 *   <li>1 — custom / GL immediate-mode rendering ({@code renderParticle()})</li>
 *   <li>3 — uses the main block/entity texture atlas</li>
 * </ul>
 * Our custom particles ({@link BeamParticle}, {@link MovingHologramParticle})
 * override {@code getFXLayer()} directly. This class is an empty placeholder.
 */
public final class ModParticleRenderType {
    /** Layer constant: custom GL rendering (matches {@code CUSTOM} in 1.20.1). */
    public static final int CUSTOM = 1;

    private ModParticleRenderType() {}
}

/* class_NeoForge_1_21_x (ModParticleRenderType):
package dev.su5ed.mffs.render.particle;

import net.minecraft.client.particle.ParticleRenderType;

public final class ModParticleRenderType {
    public static final ParticleRenderType HOLO = new ParticleRenderType("HOLO");
    public static final ParticleRenderType BEAM = new ParticleRenderType("BEAM");
}

*/
