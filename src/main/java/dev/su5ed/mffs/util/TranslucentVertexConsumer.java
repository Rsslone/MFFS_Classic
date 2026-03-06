package dev.su5ed.mffs.util;

/**
 * 1.12.2 backport of TranslucentVertexConsumer.
 *
 * Reference (1.21): extends {@code VertexConsumerWrapper}; overrides
 * {@code setColor()} to multiply the incoming alpha by a fixed factor,
 * used to render semi-transparent hologram / force-field overlays.
 *
 * In 1.12.2, there is no {@code VertexConsumer} / {@code PoseStack} pipeline.
 * Alpha-scaled rendering is handled in {@link dev.su5ed.mffs.render.BlockHighlighter}
 * by passing the alpha value directly to {@code GlStateManager.color(r,g,b,a)}
 * and using {@code GL11.GL_BLEND} / {@code GL11.GL_SRC_ALPHA}.
 * This class is an empty structural placeholder.
 */
public final class TranslucentVertexConsumer {
    private TranslucentVertexConsumer() {}
}

/* class_NeoForge_1_21_x (TranslucentVertexConsumer):
package dev.su5ed.mffs.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;

public final class TranslucentVertexConsumer extends VertexConsumerWrapper {
    private final VertexConsumer wrapped;
    private final int alpha;

    public TranslucentVertexConsumer(VertexConsumer wrapped, int alpha) {
        super(wrapped);
        this.wrapped = wrapped;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        return this.wrapped.setColor(red, green, blue, alpha * this.alpha / 0xFF);
    }

    @Override
    public VertexConsumer setColor(float red, float green, float blue, float alpha) {
        return super.setColor(red, green, blue, alpha * this.alpha / 0xFF);
    }

    @Override
    public VertexConsumer setColor(int packedColor) {
        return super.setColor(ARGB.color(ARGB.alpha(packedColor) * this.alpha / 0xFF, ARGB.red(packedColor), ARGB.green(packedColor), ARGB.blue(packedColor)));
    }
}

*/
