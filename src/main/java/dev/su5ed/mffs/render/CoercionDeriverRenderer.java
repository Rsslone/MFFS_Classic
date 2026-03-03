package dev.su5ed.mffs.render;

// 1.12.2 Backport: CoercionDeriverRenderer
// TileEntitySpecialRenderer that renders the rotating top plate accent.
// Ported from 1.7.10 RenderCoercionDeriver + 1.21 CoercionDeriverRenderer.

import dev.su5ed.mffs.blockentity.CoercionDeriverBlockEntity;
import dev.su5ed.mffs.render.model.CoercionDeriverTopModel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CoercionDeriverRenderer extends TileEntitySpecialRenderer<CoercionDeriverBlockEntity> {
    private static final ResourceLocation TEXTURE_OFF = new ResourceLocation("mffs", "textures/model/coercion_deriver_off.png");
    private static final ResourceLocation TEXTURE_ON  = new ResourceLocation("mffs", "textures/model/coercion_deriver_on.png");
    private static final CoercionDeriverTopModel MODEL = new CoercionDeriverTopModel();

    @Override
    public void render(CoercionDeriverBlockEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        bindTexture(te.isActive() ? TEXTURE_ON : TEXTURE_OFF);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 1.95, z + 0.5);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(1.3F, 1.3F, 1.3F);

        MODEL.render(te.getAnimation(), 0.0625F);

        GlStateManager.popMatrix();
    }
}
