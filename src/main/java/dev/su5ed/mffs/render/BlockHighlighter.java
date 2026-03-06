/*
 * Copyright (c) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package dev.su5ed.mffs.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * 1.12.2 backport of BlockHighlighter.
 * Reference (1.20.1): uses {@code PoseStack}, {@code Vec3}, {@code VoxelShape.forAllBoxes/Edges}.
 * In 1.12.2: uses {@code GlStateManager}, {@code AxisAlignedBB}, manual 12-edge enumeration.
 *
 * Modified version of DarkKronicle's BetterBlockOutline renderer.
 * SOURCE: https://github.com/DarkKronicle/BetterBlockOutline
 */
public final class BlockHighlighter {
    private static final float OUTLINE_WIDTH = 1.0F;
    private static final Color OUTLINE_COLOR = new Color(1, 1, 1, 0.5F);
    public static final Color LIGHT_GREEN = new Color(0, 1, 0, 0.15F);
    public static final Color LIGHT_RED   = new Color(1, 0, 0, 0.25F);

    /**
     * Highlight a single block position.
     *
     * @param partialTick render interpolation factor [0, 1)
     * @param pos         block to highlight
     * @param color       fill color (also used for outline base hue)
     */
    public static void highlightBlock(float partialTick, BlockPos pos, Color color) {
        AxisAlignedBB area = new AxisAlignedBB(pos);
        double[] cam = getCameraPos(partialTick);
        highlightAreaInternal(cam[0], cam[1], cam[2], area, color);
    }

    /**
     * Highlight the region between two block positions (no fill – outline only).
     *
     * @param partialTick render interpolation factor
     * @param from        first corner
     * @param to          second corner (inclusive)
     */
    public static void highlightArea(float partialTick, BlockPos from, BlockPos to) {
        double[] cam = getCameraPos(partialTick);
        double minX = Math.min(from.getX(), to.getX());
        double minY = Math.min(from.getY(), to.getY());
        double minZ = Math.min(from.getZ(), to.getZ());
        double maxX = Math.max(from.getX(), to.getX()) + 1;
        double maxY = Math.max(from.getY(), to.getY()) + 1;
        double maxZ = Math.max(from.getZ(), to.getZ()) + 1;
        highlightAreaInternal(cam[0], cam[1], cam[2], new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ), null);
    }

    /**
     * Highlight an arbitrary AABB with an optional fill colour.
     * Pass {@code null} for {@code fillColor} to draw the outline only.
     */
    public static void highlightArea(float partialTick, AxisAlignedBB area, @Nullable Color fillColor) {
        double[] cam = getCameraPos(partialTick);
        highlightAreaInternal(cam[0], cam[1], cam[2], area, fillColor);
    }

    // -------------------------------------------------------------------------
    // Internal – all render calls end up here
    // -------------------------------------------------------------------------

    private static void highlightAreaInternal(double camX, double camY, double camZ,
                                              AxisAlignedBB area, @Nullable Color fillColor) {
        // Expand slightly to avoid Z-fighting with block faces
        double minX = area.minX - 0.001 - camX;
        double minY = area.minY - 0.001 - camY;
        double minZ = area.minZ - 0.001 - camZ;
        double maxX = area.maxX + 0.001 - camX;
        double maxY = area.maxY + 0.001 - camY;
        double maxZ = area.maxZ + 0.001 - camZ;

        // Set up GL state (see-through, no cull, no depth write)
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        // Draw filled faces (if fill colour provided)
        if (fillColor != null) {
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            drawBox(buf, minX, minY, minZ, maxX, maxY, maxZ, fillColor);
            tess.draw();
        }

        // Draw outline edges
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(OUTLINE_WIDTH);

        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        drawEdges(buf, minX, minY, minZ, maxX, maxY, maxZ, OUTLINE_COLOR);
        tess.draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        // Restore GL state
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /** Draw the 6 faces of a box as quads (for the fill pass). */
    private static void drawBox(BufferBuilder buf,
                                double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ,
                                Color c) {
        float r = c.red(), g = c.green(), b = c.blue(), a = c.alpha();

        // West face (minX)
        buf.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        buf.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        // East face (maxX)
        buf.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        // North face (minZ)
        buf.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        buf.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        buf.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        // South face (maxZ)
        buf.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        // Top face (maxY)
        buf.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        buf.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        // Bottom face (minY)
        buf.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
    }

    /** Draw the 12 edges of a box as GL_LINES vertex pairs (for the outline pass). */
    private static void drawEdges(BufferBuilder buf,
                                  double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ,
                                  Color c) {
        float r = c.red(), g = c.green(), b = c.blue(), a = c.alpha();
        // Bottom edges
        buf.pos(minX, minY, minZ).color(r, g, b, a).endVertex(); buf.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        buf.pos(minX, minY, maxZ).color(r, g, b, a).endVertex(); buf.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(minX, minY, minZ).color(r, g, b, a).endVertex(); buf.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, minY, minZ).color(r, g, b, a).endVertex(); buf.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        // Top edges
        buf.pos(minX, maxY, minZ).color(r, g, b, a).endVertex(); buf.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        buf.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex(); buf.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(minX, maxY, minZ).color(r, g, b, a).endVertex(); buf.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex(); buf.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        // Vertical edges
        buf.pos(minX, minY, minZ).color(r, g, b, a).endVertex(); buf.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, minY, minZ).color(r, g, b, a).endVertex(); buf.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        buf.pos(minX, minY, maxZ).color(r, g, b, a).endVertex(); buf.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        buf.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex(); buf.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
    }

    /** Get the interpolated camera position for this render frame. */
    private static double[] getCameraPos(float partialTick) {
        Entity e = Minecraft.getMinecraft().getRenderViewEntity();
        return new double[]{
            e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTick,
            e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTick,
            e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTick
        };
    }

    // -------------------------------------------------------------------------
    // Color – equivalent to the reference's Color record
    // -------------------------------------------------------------------------

    /** Immutable float-component RGBA color. Mirrors the reference {@code record Color(...)}. */
    public static final class Color {
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;

        public Color(float red, float green, float blue, float alpha) {
            this.red   = red;
            this.green = green;
            this.blue  = blue;
            this.alpha = alpha;
        }

        public float red()   { return red; }
        public float green() { return green; }
        public float blue()  { return blue; }
        public float alpha() { return alpha; }

        /** Return a copy with a different alpha. */
        public Color withAlpha(float newAlpha) {
            return new Color(red, green, blue, newAlpha);
        }
    }

    private BlockHighlighter() {}
}

/* class_NeoForge_1_21_x (BlockHighlighter):
/*
 * Copyright (c) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * /
package dev.su5ed.mffs.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL11;

/**
 * Modified version of DarkKronicle's BetterBlockOutline renderer.<br>
 * <b>SOURCE</b>: <a href="https://github.com/DarkKronicle/BetterBlockOutline">DarkKronicle's BetterBlockOutline</a><br>
 * <ul>
 *     <li>
 *         <a href="https://github.com/DarkKronicle/BetterBlockOutline/blob/607b0c3b280af1516a91aac41457c6cf0abf3508/src/main/java/io/github/darkkronicle/betterblockoutline/renderers/BasicOutlineRenderer.java">BasicOutlineRenderer</a>
 *     </li>
 *     <li>
 *         <a href="https://github.com/DarkKronicle/BetterBlockOutline/blob/607b0c3b280af1516a91aac41457c6cf0abf3508/src/main/java/io/github/darkkronicle/betterblockoutline/util/RenderingUtil.java">RenderingUtil</a>
 *     </li>
 * </ul>
 * /
public final class BlockHighlighter {
    private static final Color OUTLINE_COLOR = new Color(1, 1, 1, 0.5F);
    public static final Color LIGHT_GREEN = new Color(0, 1, 0, 0.15F);
    public static final Color LIGHT_RED = new Color(1, 0, 0, 0.25F);

    public static void highlightBlock(PoseStack pose, Vec3 cameraPos, BlockPos pos, Color color) {
        highlightArea(pose, cameraPos, Shapes.create(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1), color);
    }

    public static void highlightArea(PoseStack pose, Vec3 cameraPos, BlockPos from, BlockPos to) {
        AABB area = AABB.encapsulatingFullBlocks(from, to);
        VoxelShape shape = Shapes.create(area);
        highlightArea(pose, cameraPos, shape, null);
    }

    public static void highlightArea(PoseStack pose, Vec3 cameraPos, VoxelShape shape, @Nullable Color fillColor) {
        pose.pushPose();
        pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Allow glass and other translucent/transparent objects to render properly
        if (fillColor != null) {
            VertexConsumer fillBuffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(ModRenderType.BLOCK_FILL);
            drawOutlineBoxes(fillBuffer, pose, fillColor, shape);
        }

        VertexConsumer outlineBuffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(ModRenderType.BLOCK_OUTLINE);
        drawOutlineLines(outlineBuffer, pose, OUTLINE_COLOR, shape);

        pose.popPose();
    }

    /**
     * Draws boxes for an outline. Depth and blending should be set before this is called.
     * /
    private static void drawOutlineBoxes(VertexConsumer buffer, PoseStack matrices, Color color, VoxelShape outline) {
        PoseStack.Pose entry = matrices.last();

        // Divide into each edge and draw all of them
        outline.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            // Fix Z fighting
            minX -= .001;
            minY -= .001;
            minZ -= .001;
            maxX += .001;
            maxY += .001;
            maxZ += .001;
            drawBox(entry, buffer, (float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, color);
        });
    }

    /**
     * Renders an outline, sets shader and smooth lines.
     * Before calling blend and depth should be set
     * /
    private static void drawOutlineLines(VertexConsumer buffer, PoseStack matrices, Color color, VoxelShape outline) {
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        drawOutlineLine(matrices.last(), buffer, color, outline);

        // Revert some changes
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    /**
     * Draws an outline. Setup should be done before this method is called.
     * /
    private static void drawOutlineLine(PoseStack.Pose entry, VertexConsumer buffer, Color color, VoxelShape outline) {
        outline.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            // Fix Z fighting
            minX -= .001;
            minY -= .001;
            minZ -= .001;
            maxX += .001;
            maxY += .001;
            maxZ += .001;
            drawLine(entry, buffer, new Vector3d(minX, minY, minZ), new Vector3d(maxX, maxY, maxZ), color);
        });
    }

    private static void drawBox(PoseStack.Pose entry, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Color color) {
        Matrix4f position = entry.pose();

        float r = color.red();
        float g = color.green();
        float b = color.blue();
        float a = color.alpha();

        // West
        buffer.addVertex(position, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(position, minX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(position, minX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(position, minX, maxY, minZ).setColor(r, g, b, a);

        // East
        buffer.addVertex(position, maxX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(position, maxX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(position, maxX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(position, maxX, maxY, maxZ).setColor(r, g, b, a);

        // North
        buffer.addVertex(position, maxX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(position, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(position, minX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(position, maxX, maxY, minZ).setColor(r, g, b, a);

        // South
        buffer.addVertex(position, minX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(position, maxX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(position, maxX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(position, minX, maxY, maxZ).setColor(r, g, b, a);

        // Top
        buffer.addVertex(position, minX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(position, maxX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(position, maxX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(position, minX, maxY, minZ).setColor(r, g, b, a);

        // Bottom
        buffer.addVertex(position, maxX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(position, minX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(position, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(position, maxX, minY, minZ).setColor(r, g, b, a);
    }

    /**
     * Gets the normal line from a starting and ending point.
     *
     * @param start Starting point
     * @param end   Ending point
     * @return Normal line
     * /
    private static Vector3d getNormalAngle(Vector3d start, Vector3d end) {
        double xLength = end.x - start.x;
        double yLength = end.y - start.y;
        double zLength = end.z - start.z;
        double distance = Math.sqrt(xLength * xLength + yLength * yLength + zLength * zLength);
        xLength /= distance;
        yLength /= distance;
        zLength /= distance;
        return new Vector3d(xLength, yLength, zLength);
    }

    /**
     * This method doesn't do any of the {@link RenderSystem} setting up. Should be setup before call.
     *
     * @param entry  Matrix entry
     * @param buffer Buffer builder that is already setup
     * @param start  Starting point
     * @param end    Ending point
     * @param color  Color to render
     * /
    private static void drawLine(PoseStack.Pose entry, VertexConsumer buffer, Vector3d start, Vector3d end, Color color) {
        Vector3d normal = getNormalAngle(start, end);
        float red = color.red();
        float green = color.green();
        float blue = color.blue();
        float alpha = color.alpha();
        float lineWidth = Minecraft.getInstance().getWindow().getAppropriateLineWidth();

        buffer.addVertex(entry.pose(), (float) start.x, (float) start.y, (float) start.z)
            .setColor(red, green, blue, alpha)
            .setNormal(entry, (float) normal.x, (float) normal.y, (float) normal.z)
            .setLineWidth(lineWidth);

        buffer.addVertex(entry.pose(), (float) end.x, (float) end.y, (float) end.z)
            .setColor(red, green, blue, alpha)
            .setNormal(entry, (float) normal.x, (float) normal.y, (float) normal.z)
            .setLineWidth(lineWidth);
    }

    public record Color(float red, float green, float blue, float alpha) {
        public Color withAlpha(float alpha) {
            return new Color(this.red, this.green, this.blue, alpha);
        }
    }

    private BlockHighlighter() {}
}


*/
