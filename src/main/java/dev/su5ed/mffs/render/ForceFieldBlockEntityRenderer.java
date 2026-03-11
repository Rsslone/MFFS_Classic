package dev.su5ed.mffs.render;

import dev.su5ed.mffs.compat.CodeChickenLibEmissiveCompat;
import dev.su5ed.mffs.blockentity.ForceFieldBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * TESR for force field blocks. Only handles camo blocks that have their own
 * TileEntitySpecialRenderer (chests, ender chests, etc.) via delegation.
 * Plain block camouflage (stone, planks, etc.) is handled entirely at the
 * baked model level by {@link dev.su5ed.mffs.render.model.ForceFieldBlockModel}.
 */
@SideOnly(Side.CLIENT)
public class ForceFieldBlockEntityRenderer extends TileEntitySpecialRenderer<ForceFieldBlockEntity> {

    @Override
    public void render(ForceFieldBlockEntity te, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        CodeChickenLibEmissiveCompat.render(te, x, y, z);

        IBlockState camouflage = te.getCamouflage();
        if (camouflage != null) {
            BlockEntityRenderDelegate.INSTANCE.tryRenderDelegate(te, x, y, z, partialTicks, destroyStage);
        }
    }
}
