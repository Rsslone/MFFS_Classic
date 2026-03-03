package dev.su5ed.mffs.render;

// 1.12.2 Backport: ForceFieldBlockEntityRenderer
// Renders the camouflage block model when a ForceFieldBlockEntity has a camouflage state.
// Uses TESR with BlockRendererDispatcher to render the disguise block's model.

import dev.su5ed.mffs.blockentity.ForceFieldBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class ForceFieldBlockEntityRenderer extends TileEntitySpecialRenderer<ForceFieldBlockEntity> {

    @Override
    public void render(ForceFieldBlockEntity te, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        IBlockState camouflage = te.getCamouflage();
        if (camouflage == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
        BlockPos pos = te.getPos();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        // Bind block texture atlas
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        // Render the camouflage block model
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        // Offset buffer to block position relative
        buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());

        try {
            dispatcher.getModelForState(camouflage);
            dispatcher.renderBlock(camouflage, pos, te.getWorld(), buffer);
        } catch (Exception ignored) {
            // Fail silently if block model can't render
        }

        buffer.setTranslation(0, 0, 0);
        tessellator.draw();

        GlStateManager.popMatrix();
    }
}
