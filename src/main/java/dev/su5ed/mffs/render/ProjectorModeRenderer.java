package dev.su5ed.mffs.render;

// 1.12.2 Backport: ProjectorModeRenderer
// Renders holographic mode shapes (cube, sphere, pyramid, cylinder, tube, custom)
// above the Projector when a mode item is inserted.
// Ported from 1.21 ProjectorModeRenderer + 1.7.10 RenderForceFieldProjector.
// Uses a textured ModelRenderer cube (force_cube.png) matching 1.21's ForceCubeModel.

import dev.su5ed.mffs.blockentity.ProjectorBlockEntity;
import dev.su5ed.mffs.setup.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.List;

@SideOnly(Side.CLIENT)
public final class ProjectorModeRenderer {
    private static final ResourceLocation FORCE_CUBE_TEXTURE = new ResourceLocation("mffs", "textures/model/force_cube.png");
    private static final float CUBE_SCALE = 0.55f;
    private static final float SPHERE_SCALE = 0.2f;
    private static final float SPHERE_RADIUS = 1.5f;
    private static final float CYLINDER_SCALE = 0.15f;
    private static final float CYLINDER_RADIUS = 1.5f;
    private static final float CYLINDER_DETAIL = 0.5f;
    private static final float RADIUS_EXPANSION = 0.0f;
    private static final float PYRAMID_HEIGHT = 0.5f;
    private static final float PYRAMID_WIDTH = 0.3f;

    // Color for holographic shapes (light blue, used for pyramid)
    private static final float HOLO_R = 0.4f;
    private static final float HOLO_G = 0.8f;
    private static final float HOLO_B = 1.0f;

    private static final List<Item> CYCLE_MODES = Arrays.asList(ModItems.CUBE_MODE, ModItems.SPHERE_MODE, ModItems.TUBE_MODE, ModItems.PYRAMID_MODE);
    private static final int CUSTOM_PERIOD = 40;

    // Textured cube model matching 1.7.10/1.21 ForceCubeModel: addBox(-8, -8, -8, 16, 16, 16)
    // Texture is 112x70 (1.7.10 setTextureSize(112, 70)), UV space must match for all 6 faces
    private static final ModelBase CUBE_MODEL_BASE = new ModelBase() {};
    private static final ModelRenderer FORCE_CUBE;
    static {
        CUBE_MODEL_BASE.textureWidth = 112;
        CUBE_MODEL_BASE.textureHeight = 70;
        FORCE_CUBE = new ModelRenderer(CUBE_MODEL_BASE, 0, 0);
        FORCE_CUBE.addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16);
        FORCE_CUBE.setRotationPoint(0.0F, 0.0F, 0.0F);
    }

    /**
     * Render the holographic mode shape above the projector.
     * Called from ProjectorRenderer when a mode is present.
     */
    public static void renderMode(ProjectorBlockEntity te, Item modeItem, double x, double y, double z, float partialTicks) {
        float ticks = te.getAnimation() + (te.isActive() ? partialTicks : 0);

        if (modeItem == ModItems.CUBE_MODE || modeItem == ModItems.TUBE_MODE) {
            renderCubeMode(x, y, z, ticks);
        } else if (modeItem == ModItems.SPHERE_MODE) {
            renderSphereMode(x, y, z, ticks);
        } else if (modeItem == ModItems.PYRAMID_MODE) {
            renderPyramidMode(x, y, z, ticks);
        } else if (modeItem == ModItems.CYLINDER_MODE) {
            renderCylinderMode(x, y, z, ticks);
        } else if (modeItem == ModItems.CUSTOM_MODE) {
            renderCustomMode(x, y, z, ticks);
        }
    }

    private static void beginModeRendering(double x, double y, double z) {
        RenderHelper.disableStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);

        // Bind the force cube texture (used by all cube-based shapes)
        Minecraft.getMinecraft().getTextureManager().bindTexture(FORCE_CUBE_TEXTURE);

        GlStateManager.enableBlend();
        // Additive blending for holographic glow effect (matches 1.7.10 reference)
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.enableCull();

        // Full brightness (emissive) — matches 1.21's LightTexture.FULL_BRIGHT + EMISSIVE shader
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    }

    private static void endModeRendering() {
        GlStateManager.disableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        RenderHelper.enableStandardItemLighting();
    }

    /**
     * Apply the hovering + spinning transformation.
     * Matches 1.21's hoverObject(poseStack, ticks, scale, centerPos).
     */
    private static void hoverObject(float ticks, float scale) {
        GlStateManager.translate(0, 1 + Math.sin(Math.toRadians(ticks * 3L)) / 7.0, 0);
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.rotate(ticks * 4L, 0, 1, 0);
        GlStateManager.rotate(36 + ticks * 4L, 0, 0, 1);
    }

    /**
     * Render the textured force cube model at the current transform.
     * Uses GlStateManager.color() for alpha tinting (white color * texture * alpha).
     */
    private static void renderForceCube(float alpha) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        FORCE_CUBE.render(0.0625F);
    }

    // ========================================================================
    // Cube Mode
    // ========================================================================
    private static void renderCubeMode(double x, double y, double z, float ticks) {
        float alpha = Math.min((float) (Math.sin(ticks / 10.0) / 2.0 + 1.0), 1.0f);
        beginModeRendering(x, y, z);
        hoverObject(ticks, CUBE_SCALE);
        renderForceCube(alpha);
        endModeRendering();
    }

    // ========================================================================
    // Sphere Mode
    // ========================================================================
    private static void renderSphereMode(double x, double y, double z, float ticks) {
        float alpha = Math.min((float) (Math.sin(ticks / 10.0) / 2.0 + 1.0), 1.0f) / 5f;
        beginModeRendering(x, y, z);
        hoverObject(ticks, SPHERE_SCALE);

        int steps = (int) Math.ceil(Math.PI / Math.atan(1.0D / SPHERE_RADIUS / 2));
        for (int phi_n = 0; phi_n < 2 * steps; phi_n++) {
            for (int theta_n = 1; theta_n < steps; theta_n++) {
                double phi = Math.PI * 2 / steps * phi_n;
                double theta = Math.PI / steps * theta_n;
                double vx = Math.sin(theta) * Math.cos(phi) * SPHERE_RADIUS;
                double vy = Math.cos(theta) * SPHERE_RADIUS;
                double vz = Math.sin(theta) * Math.sin(phi) * SPHERE_RADIUS;

                GlStateManager.pushMatrix();
                GlStateManager.translate(vx, vy, vz);
                renderForceCube(alpha);
                GlStateManager.popMatrix();
            }
        }

        endModeRendering();
    }

    // ========================================================================
    // Pyramid Mode
    // ========================================================================
    private static void renderPyramidMode(double x, double y, double z, float ticks) {
        RenderHelper.disableStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);

        // Pyramid uses the force_cube texture with textured triangles
        // matching 1.21's HOLO_TEXTURED_TRIANGLE render type
        Minecraft.getMinecraft().getTextureManager().bindTexture(FORCE_CUBE_TEXTURE);

        GlStateManager.enableBlend();
        // Additive blending for holographic glow, matching other mode shapes
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();

        // Full brightness (emissive)
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

        hoverObject(ticks, 1.0f);
        GlStateManager.rotate(180, 0, 0, 1);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        float w = PYRAMID_WIDTH;
        float h = PYRAMID_HEIGHT;
        float ty = -0.4f;
        int uvMaxX = 2;
        int uvMaxY = 2;

        buf.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX);
        // 4 side faces
        buf.pos(0, ty, 0).tex(0, 0).endVertex();
        buf.pos(-w, h + ty, -w).tex(-uvMaxX, -uvMaxY).endVertex();
        buf.pos(-w, h + ty, w).tex(-uvMaxX, uvMaxY).endVertex();

        buf.pos(0, ty, 0).tex(0, 0).endVertex();
        buf.pos(-w, h + ty, w).tex(-uvMaxX, uvMaxY).endVertex();
        buf.pos(w, h + ty, w).tex(uvMaxX, uvMaxY).endVertex();

        buf.pos(0, ty, 0).tex(0, 0).endVertex();
        buf.pos(w, h + ty, w).tex(uvMaxX, uvMaxY).endVertex();
        buf.pos(w, h + ty, -w).tex(uvMaxX, -uvMaxY).endVertex();

        buf.pos(0, ty, 0).tex(0, 0).endVertex();
        buf.pos(w, h + ty, -w).tex(uvMaxX, -uvMaxY).endVertex();
        buf.pos(-w, h + ty, -w).tex(-uvMaxX, -uvMaxY).endVertex();

        // Bottom face (2 triangles)
        buf.pos(-w, h + ty, -w).tex(-uvMaxX, -uvMaxY).endVertex();
        buf.pos(-w, h + ty, w).tex(-uvMaxX, uvMaxY).endVertex();
        buf.pos(w, h + ty, w).tex(uvMaxX, uvMaxY).endVertex();

        buf.pos(w, h + ty, w).tex(uvMaxX, uvMaxY).endVertex();
        buf.pos(w, h + ty, -w).tex(uvMaxX, -uvMaxY).endVertex();
        buf.pos(-w, h + ty, -w).tex(-uvMaxX, -uvMaxY).endVertex();
        tess.draw();

        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        RenderHelper.enableStandardItemLighting();
    }

    // ========================================================================
    // Cylinder Mode
    // ========================================================================
    private static void renderCylinderMode(double x, double y, double z, float ticks) {
        float alpha = Math.min((float) (Math.sin(ticks / 10.0) / 2.0 + 1.0), 1.0f);
        beginModeRendering(x, y, z);
        hoverObject(ticks, CYLINDER_SCALE);

        int i = 0;
        for (float rx = -CYLINDER_RADIUS; rx <= CYLINDER_RADIUS; rx += CYLINDER_DETAIL) {
            for (float rz = -CYLINDER_RADIUS; rz <= CYLINDER_RADIUS; rz += CYLINDER_DETAIL) {
                for (float ry = -CYLINDER_RADIUS; ry <= CYLINDER_RADIUS; ry += CYLINDER_DETAIL) {
                    float area = rx * rx + rz * rz + RADIUS_EXPANSION;
                    boolean isWall = area <= CYLINDER_RADIUS * CYLINDER_RADIUS && area >= (CYLINDER_RADIUS - 1) * (CYLINDER_RADIUS - 1);
                    boolean isCap = (ry == 0 || ry == CYLINDER_RADIUS - 1) && area <= CYLINDER_RADIUS * CYLINDER_RADIUS;
                    if (isWall || isCap) {
                        if (i % 2 == 0) {
                            GlStateManager.pushMatrix();
                            GlStateManager.translate(rx, ry, rz);
                            renderForceCube(alpha);
                            GlStateManager.popMatrix();
                        }
                        i++;
                    }
                }
            }
        }

        endModeRendering();
    }

    // ========================================================================
    // Custom Mode (cycles through modes)
    // ========================================================================
    private static void renderCustomMode(double x, double y, double z, float ticks) {
        int max = CYCLE_MODES.size() * CUSTOM_PERIOD;
        int index = (int) ((ticks % max) / (double) CUSTOM_PERIOD);
        index = Math.min(index, CYCLE_MODES.size() - 1);
        Item mode = CYCLE_MODES.get(index);

        if (mode == ModItems.CUBE_MODE || mode == ModItems.TUBE_MODE) {
            renderCubeMode(x, y, z, ticks);
        } else if (mode == ModItems.SPHERE_MODE) {
            renderSphereMode(x, y, z, ticks);
        } else if (mode == ModItems.PYRAMID_MODE) {
            renderPyramidMode(x, y, z, ticks);
        }
    }

    private ProjectorModeRenderer() {}
}
