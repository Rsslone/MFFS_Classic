package dev.su5ed.mffs.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

/**
 * 1.12.2 backport of ModRenderPipeline.
 *
 * Reference (1.21) defines {@code RenderPipeline} instances for each holographic render type.
 * In 1.12.2 there is no RenderPipeline / RenderType abstraction for TESR-style rendering;
 * GL state is managed directly via {@link GlStateManager}.
 *
 * Each constant here is a {@link Pipeline} – a simple setup/teardown pair that replicates
 * the blend function, cull, depth-write and line-smooth flags of the reference pipeline.
 */
public final class ModRenderPipeline {

    // -------------------------------------------------------------------------
    // Pipeline interface
    // -------------------------------------------------------------------------

    /** Encapsulates the GL state changes needed to start / stop a render pipeline. */
    public interface Pipeline {
        void setup();
        void teardown();
    }

    // -------------------------------------------------------------------------
    // Holo entity – translucent blend, no depth write, use light map
    // Reference: BlendFunction.TRANSLUCENT, withDepthWrite(false)
    // -------------------------------------------------------------------------
    public static final Pipeline HOLO_ENTITY = new Pipeline() {
        @Override
        public void setup() {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthMask(false);
            GlStateManager.disableAlpha();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        }

        @Override
        public void teardown() {
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
        }
    };

    // -------------------------------------------------------------------------
    // Holo triangle – additive / lightning blend, position+color, no depth write
    // Reference: BlendFunction.LIGHTNING, withDepthWrite(false), TRIANGLES
    // -------------------------------------------------------------------------
    public static final Pipeline HOLO_TRIANGLE = new Pipeline() {
        @Override
        public void setup() {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            GlStateManager.disableAlpha();
            GlStateManager.disableTexture2D();
        }

        @Override
        public void teardown() {
            GlStateManager.enableTexture2D();
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
        }
    };

    // -------------------------------------------------------------------------
    // Holo textured triangle – translucent blend, no cull, position+tex, no depth write
    // Reference: BlendFunction.TRANSLUCENT, withCull(false), withDepthWrite(false), TRIANGLES
    // -------------------------------------------------------------------------
    public static final Pipeline HOLO_TEXTURED_TRIANGLE = new Pipeline() {
        @Override
        public void setup() {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthMask(false);
            GlStateManager.disableAlpha();
            GlStateManager.disableCull();
        }

        @Override
        public void teardown() {
            GlStateManager.enableCull();
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
        }
    };

    // -------------------------------------------------------------------------
    // Holo quad – translucent blend (GUI style), no cull, position+tex, quads
    // Reference: GUI_TEXTURED_SNIPPET, withCull(false)
    // -------------------------------------------------------------------------
    public static final Pipeline HOLO_QUAD = new Pipeline() {
        @Override
        public void setup() {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableCull();
        }

        @Override
        public void teardown() {
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
        }
    };

    // -------------------------------------------------------------------------
    // Beam particle – additive blend, no cull, LEQUAL depth, no depth write
    // Reference: BlendFunction.LIGHTNING, withCull(false), LEQUAL_DEPTH_TEST, withDepthWrite(false)
    // -------------------------------------------------------------------------
    public static final Pipeline BEAM_PARTICLE = new Pipeline() {
        @Override
        public void setup() {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            GlStateManager.disableCull();
            GlStateManager.enableAlpha();
        }

        @Override
        public void teardown() {
            GlStateManager.enableCull();
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
        }
    };

    // -------------------------------------------------------------------------
    // Block fill – position+color quads, no cull, no depth write, no depth test (see-through)
    // Reference: DEBUG_FILLED_SNIPPET, withCull(false), withDepthWrite(false), NO_DEPTH_TEST
    // -------------------------------------------------------------------------
    public static final Pipeline BLOCK_FILL = new Pipeline() {
        @Override
        public void setup() {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableDepth();
            GlStateManager.disableCull();
            GlStateManager.depthMask(false);
            GlStateManager.disableTexture2D();
        }

        @Override
        public void teardown() {
            GlStateManager.enableTexture2D();
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
        }
    };

    // -------------------------------------------------------------------------
    // Block outline – lines, no cull, no depth write, no depth test (see-through)
    // Reference: LINES_SNIPPET, withCull(false), withDepthWrite(false), NO_DEPTH_TEST
    // -------------------------------------------------------------------------
    public static final Pipeline BLOCK_OUTLINE = new Pipeline() {
        @Override
        public void setup() {
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableDepth();
            GlStateManager.disableCull();
            GlStateManager.depthMask(false);
            GlStateManager.disableTexture2D();
        }

        @Override
        public void teardown() {
            GlStateManager.enableTexture2D();
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }
    };

    private ModRenderPipeline() {}
}

/* class_NeoForge_1_21_x (ModRenderPipeline):
package dev.su5ed.mffs.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.su5ed.mffs.MFFSMod;
import net.minecraft.client.renderer.RenderPipelines;

import static net.minecraft.client.renderer.RenderPipelines.*;

public class ModRenderPipeline {
    public static final RenderPipeline HOLO_ENTITY = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
        .withLocation(MFFSMod.location("pipeline/holo_entity"))
        .withShaderDefine("EMISSIVE")
        .withShaderDefine("NO_OVERLAY")
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withVertexShader("core/entity")
        .withFragmentShader("core/entity")
        .withSampler("Sampler0")
        .withBlend(BlendFunction.TRANSLUCENT)
        .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
        .withDepthWrite(false)
        .build();

    public static final RenderPipeline HOLO_TRIANGLE = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
        .withLocation(MFFSMod.location("pipeline/holo_triangle"))
        .withVertexShader("core/position_color")
        .withFragmentShader("core/position_color")
        .withBlend(BlendFunction.LIGHTNING)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
        .withDepthWrite(false)
        .build();

    public static final RenderPipeline HOLO_TEXTURED_TRIANGLE = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
        .withLocation(MFFSMod.location("pipeline/holo_textured_triangle"))
        .withVertexShader("core/position_tex")
        .withFragmentShader("core/position_tex")
        .withSampler("Sampler0")
        .withBlend(BlendFunction.TRANSLUCENT)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
        .withCull(false)
        .withDepthWrite(false)
        .build();

    public static final RenderPipeline HOLO_QUAD = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withLocation(MFFSMod.location("pipeline/holo_quad"))
        .withCull(false)
        .build();

    public static final RenderPipeline BLOCK_FILL = RenderPipeline.builder(DEBUG_FILLED_SNIPPET)
        .withLocation(MFFSMod.location("pipeline/block_fill"))
        .withCull(false)
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build();

    public static final RenderPipeline BLOCK_OUTLINE = RenderPipeline.builder(LINES_SNIPPET)
        .withLocation(MFFSMod.location("pipeline/block_outline"))
        .withCull(false)
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build();

    public static final RenderPipeline BEAM_PARTICLE = RenderPipeline.builder(PARTICLE_SNIPPET)
        .withLocation(MFFSMod.location("pipeline/beam_particle"))
        .withBlend(BlendFunction.LIGHTNING)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withDepthWrite(false)
        .build();
}

*/
