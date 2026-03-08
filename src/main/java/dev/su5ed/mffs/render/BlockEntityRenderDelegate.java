package dev.su5ed.mffs.render;

import dev.su5ed.mffs.MFFSMod;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * SOURCE: Geforce132/SecurityCraft
 * Delegates rendering to another block entity's TESR when the camouflage block
 * has its own TileEntitySpecialRenderer (e.g. chests, ender chests).
 * For plain blocks like stone, no delegation is needed — the baked model handles it.
 */
public final class BlockEntityRenderDelegate {
    public static final BlockEntityRenderDelegate INSTANCE = new BlockEntityRenderDelegate();

    private final Map<TileEntity, DelegateRendererInfo> renderDelegates = new HashMap<>();

    private BlockEntityRenderDelegate() {}

    public void putDelegateFor(TileEntity originalBlockEntity, IBlockState delegateState) {
        if (this.renderDelegates.containsKey(originalBlockEntity)) {
            DelegateRendererInfo delegateInfo = this.renderDelegates.get(originalBlockEntity);
            if (delegateInfo.delegateBlockEntity.getBlockType() == delegateState.getBlock()) {
                return;
            }
        }

        if (delegateState != null) {
            Minecraft mc = Minecraft.getMinecraft();
            TileEntity delegateBe = delegateState.getBlock().createTileEntity(mc.world, delegateState);
            if (delegateBe != null) {
                delegateBe.setPos(BlockPos.ORIGIN);
                delegateBe.setWorld(mc.world);
                @SuppressWarnings("unchecked")
                TileEntitySpecialRenderer<TileEntity> delegateBeRenderer =
                    (TileEntitySpecialRenderer<TileEntity>) TileEntityRendererDispatcher.instance.getRenderer(delegateBe);
                if (delegateBeRenderer != null) {
                    this.renderDelegates.put(originalBlockEntity, new DelegateRendererInfo(delegateBe, delegateBeRenderer));
                }
            }
        }
    }

    public void removeDelegateOf(TileEntity originalBlockEntity) {
        this.renderDelegates.remove(originalBlockEntity);
    }

    public void tryRenderDelegate(TileEntity originalBlockEntity, double x, double y, double z, float partialTicks, int destroyStage) {
        DelegateRendererInfo delegateRendererInfo = this.renderDelegates.get(originalBlockEntity);
        if (delegateRendererInfo != null && delegateRendererInfo.renderer != null) {
            try {
                delegateRendererInfo.renderer.render(delegateRendererInfo.delegateBlockEntity, x, y, z, partialTicks, destroyStage, 1.0f);
            } catch (Exception e) {
                MFFSMod.LOGGER.warn("Error rendering delegate TileEntity {}: {}", delegateRendererInfo.delegateBlockEntity, e);
                removeDelegateOf(originalBlockEntity);
            }
        }
    }

    private static class DelegateRendererInfo {
        final TileEntity delegateBlockEntity;
        final TileEntitySpecialRenderer<TileEntity> renderer;

        DelegateRendererInfo(TileEntity delegateBlockEntity, TileEntitySpecialRenderer<TileEntity> renderer) {
            this.delegateBlockEntity = delegateBlockEntity;
            this.renderer = renderer;
        }
    }
}

