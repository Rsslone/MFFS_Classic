package dev.su5ed.mffs.render.model;

/**
 * 1.12.2 backport of DelegateBlockModelPart.
 *
 * Reference (1.21): a {@code ModelPart} subclass used by the 1.21
 * {@code BlockEntityRenderDelegate} to chain a block entity's renderer
 * as a sub-part of another entity's model hierarchy,
 * using {@code SubmitNodeCollector}.
 *
 * In 1.12.2, neither {@code ModelPart} (the layered-model system introduced
 * in 1.17) nor the {@code SubmitNodeCollector} render pipeline exist.
 * TESR chaining is not used; camo-TESR delegation is handled directly by
 * {@link dev.su5ed.mffs.render.BlockEntityRenderDelegate#renderAllDelegates} via
 * {@code RenderWorldLastEvent}.
 *
 * This class is retained as an empty structural placeholder.
 */
public final class DelegateBlockModelPart {
    private DelegateBlockModelPart() {}
}

/* class_NeoForge_1_21_x (DelegateBlockModelPart):
package dev.su5ed.mffs.render.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DelegateBlockModelPart implements BlockModelPart {
    private final BlockModelPart delegate;
    private final BlockState state;

    public DelegateBlockModelPart(BlockModelPart delegate, BlockState state) {
        this.delegate = delegate;
        this.state = state;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        return this.delegate.getQuads(direction);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.delegate.useAmbientOcclusion();
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return this.delegate.particleIcon();
    }

    @Override
    public ChunkSectionLayer getRenderType(BlockState state) {
        return this.delegate.getRenderType(this.state);
    }

    @Override
    public TriState ambientOcclusion() {
        return this.delegate.ambientOcclusion();
    }
}

*/
