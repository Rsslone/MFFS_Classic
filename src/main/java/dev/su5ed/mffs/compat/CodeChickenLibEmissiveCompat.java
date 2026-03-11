package dev.su5ed.mffs.compat;

import dev.su5ed.mffs.MFFSConfig;
import dev.su5ed.mffs.api.ForceFieldBlock;
import dev.su5ed.mffs.blockentity.ForceFieldBlockEntity;
import dev.su5ed.mffs.setup.ModModules;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public final class CodeChickenLibEmissiveCompat {
    private static final String CCL_MODID = "codechickenlib";
    private static final boolean AVAILABLE = Loader.isModLoaded(CCL_MODID);

    private static final int MAX_GLOW_MODULES = 64;
    private static final int MIN_ALPHA = 40;
    private static final int MAX_ALPHA = 104;
    private static final double FACE_OFFSET = 0.002;

    private CodeChickenLibEmissiveCompat() {}

    public static boolean isAvailable() {
        return AVAILABLE && MFFSConfig.enableCodeChickenLibEmissiveForceFields;
    }

    public static void render(ForceFieldBlockEntity tile, double x, double y, double z) {
        if (!isAvailable() || tile == null || tile.getWorld() == null || !tile.getWorld().isRemote) {
            return;
        }

        if (getGlowModuleCount(tile) <= 0) {
            return;
        }

        renderEmissiveFaces(tile, x, y, z);
    }

    private static void renderEmissiveFaces(ForceFieldBlockEntity forceField, double x, double y, double z) {
        World world = forceField.getWorld();
        BlockPos pos = forceField.getPos();
        TextureAtlasSprite sprite = getForceFieldSprite(world, pos);
        if (sprite == null) {
            return;
        }

        int glowModules = getGlowModuleCount(forceField);
        if (glowModules <= 0) {
            return;
        }

        int alpha = MIN_ALPHA + Math.round((MAX_ALPHA - MIN_ALPHA) * (glowModules / (float) MAX_GLOW_MODULES));
        float previousLightX = OpenGlHelper.lastBrightnessX;
        float previousLightY = OpenGlHelper.lastBrightnessY;

        RenderHelper.disableStandardItemLighting();
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GlStateManager.enableCull();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        addExposedFaces(buffer, world, pos, sprite, alpha);
        tessellator.draw();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        RenderHelper.enableStandardItemLighting();
    }

    private static void addExposedFaces(BufferBuilder buffer, World world, BlockPos pos, TextureAtlasSprite sprite, int alpha) {
        double minX = 0.0;
        double minY = 0.0;
        double minZ = 0.0;
        double maxX = minX + 1.0;
        double maxY = minY + 1.0;
        double maxZ = minZ + 1.0;
        double minU = sprite.getMinU();
        double maxU = sprite.getMaxU();
        double minV = sprite.getMinV();
        double maxV = sprite.getMaxV();

        if (isExposed(world, pos, EnumFacing.DOWN)) {
            double y = minY - FACE_OFFSET;
            addQuad(buffer,
                minX, y, maxZ,
                minX, y, minZ,
                maxX, y, minZ,
                maxX, y, maxZ,
                minU, maxV,
                minU, minV,
                maxU, minV,
                maxU, maxV,
                alpha);
        }
        if (isExposed(world, pos, EnumFacing.UP)) {
            double y = maxY + FACE_OFFSET;
            addQuad(buffer,
                maxX, y, maxZ,
                maxX, y, minZ,
                minX, y, minZ,
                minX, y, maxZ,
                maxU, maxV,
                maxU, minV,
                minU, minV,
                minU, maxV,
                alpha);
        }
        if (isExposed(world, pos, EnumFacing.NORTH)) {
            double z = minZ - FACE_OFFSET;
            addQuad(buffer,
                minX, minY, z,
                minX, maxY, z,
                maxX, maxY, z,
                maxX, minY, z,
                minU, maxV,
                minU, minV,
                maxU, minV,
                maxU, maxV,
                alpha);
        }
        if (isExposed(world, pos, EnumFacing.SOUTH)) {
            double z = maxZ + FACE_OFFSET;
            addQuad(buffer,
                maxX, minY, z,
                maxX, maxY, z,
                minX, maxY, z,
                minX, minY, z,
                minU, maxV,
                minU, minV,
                maxU, minV,
                maxU, maxV,
                alpha);
        }
        if (isExposed(world, pos, EnumFacing.WEST)) {
            double x = minX - FACE_OFFSET;
            addQuad(buffer,
                x, minY, maxZ,
                x, maxY, maxZ,
                x, maxY, minZ,
                x, minY, minZ,
                minU, maxV,
                minU, minV,
                maxU, minV,
                maxU, maxV,
                alpha);
        }
        if (isExposed(world, pos, EnumFacing.EAST)) {
            double x = maxX + FACE_OFFSET;
            addQuad(buffer,
                x, minY, minZ,
                x, maxY, minZ,
                x, maxY, maxZ,
                x, minY, maxZ,
                minU, maxV,
                minU, minV,
                maxU, minV,
                maxU, maxV,
                alpha);
        }
    }

    private static TextureAtlasSprite getForceFieldSprite(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return Minecraft.getMinecraft()
            .getBlockRendererDispatcher()
            .getModelForState(state)
            .getParticleTexture();
    }

    private static boolean isExposed(World world, BlockPos pos, EnumFacing facing) {
        return !(world.getBlockState(pos.offset(facing)).getBlock() instanceof ForceFieldBlock);
    }

    private static void addQuad(BufferBuilder buffer,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                double x3, double y3, double z3,
                                double x4, double y4, double z4,
                                double u1, double v1,
                                double u2, double v2,
                                double u3, double v3,
                                double u4, double v4,
                                int alpha) {
        buffer.pos(x1, y1, z1).tex(u1, v1).color(255, 255, 255, alpha).endVertex();
        buffer.pos(x2, y2, z2).tex(u2, v2).color(255, 255, 255, alpha).endVertex();
        buffer.pos(x3, y3, z3).tex(u3, v3).color(255, 255, 255, alpha).endVertex();
        buffer.pos(x4, y4, z4).tex(u4, v4).color(255, 255, 255, alpha).endVertex();
    }

    private static int getGlowModuleCount(ForceFieldBlockEntity tile) {
        return tile.getProjector()
            .map(projector -> Math.min(projector.getModuleCount(ModModules.GLOW), MAX_GLOW_MODULES))
            .orElseGet(() -> approximateModuleCount(tile.getClientBlockLight()));
    }

    private static int approximateModuleCount(int clientBlockLight) {
        if (clientBlockLight <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(MAX_GLOW_MODULES, Math.round(clientBlockLight / 15.0F * MAX_GLOW_MODULES)));
    }
}