package dev.su5ed.mffs.render;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * 1.12.2 backport of LazyRenderer.
 * Reference (1.20.1): render(PoseStack, VertexConsumer, int ticks, float partialTick)
 * In 1.12.2 there is no PoseStack/VertexConsumer; each renderer manages its own
 * GlStateManager/Tessellator state. The camera-relative translation is applied by
 * RenderTickHandler before invoking render().
 */
public interface LazyRenderer {
    /**
     * Render this object. Called during RenderWorldLastEvent with the GL matrix already
     * translated so that world origin (0,0,0) matches the render camera position.
     *
     * @param ticks       accumulated client tick counter
     * @param partialTick interpolation factor [0, 1)
     */
    void render(int ticks, float partialTick);

    /**
     * World-space centre position used for back-to-front sorting among render types.
     * May return null if distance-sorting is not needed.
     */
    @Nullable
    Vec3d centerPos();
}
