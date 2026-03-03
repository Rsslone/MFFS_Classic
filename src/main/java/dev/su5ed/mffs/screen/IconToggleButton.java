package dev.su5ed.mffs.screen;

// 1.12.2 Backport: IconToggleButton
// AbstractButton → GuiButton; onPress(InputWithModifiers) → firePress()
// Tooltip.create() removed (no tooltip widget in 1.12.2; screen renders tooltip for hovered button)
// BooleanConsumer (fastutil) → java.util.function.Consumer<Boolean>
// GuiGraphics.blit() → bindTexture() + Tessellator
// isHoveredOrFocused() → this.hovered

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class IconToggleButton extends GuiButton {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(400);

    private final ResourceLocation image;
    private final int imageU;
    private final int imageV;
    private final ITextComponent tooltip;
    private final BooleanSupplier value;
    private final Consumer<Boolean> onPress;

    public IconToggleButton(int x, int y, int width, int height, ITextComponent tooltip, int imageU, int imageV, BooleanSupplier value, Consumer<Boolean> onPress) {
        this(x, y, width, height, tooltip, IconCycleButton.GUI_BUTTONS, imageU, imageV, value, onPress);
    }

    public IconToggleButton(int x, int y, int width, int height, ITextComponent tooltip, ResourceLocation image, int imageU, int imageV, BooleanSupplier value, Consumer<Boolean> onPress) {
        super(NEXT_ID.getAndIncrement(), x, y, width, height, "");
        this.image = image;
        this.imageU = imageU;
        this.imageV = imageV;
        this.tooltip = tooltip;
        this.value = value;
        this.onPress = onPress;
    }

    public ITextComponent getTooltip() {
        return this.tooltip;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    /** Called by BaseScreen.actionPerformed() */
    public void firePress() {
        if (this.enabled) {
            this.onPress.accept(this.value.getAsBoolean());
        }
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        this.hovered = mouseX >= this.x && mouseY >= this.y
            && mouseX < this.x + this.width && mouseY < this.y + this.height;
        float col;
        if (this.value.getAsBoolean()) {
            col = 0.6F;
        } else if (this.hovered) {
            col = 0.85F;
        } else {
            col = 1.0F;
        }
        GlStateManager.color(col, col, col, 1.0F);
        mc.getTextureManager().bindTexture(this.image);
        blitRegion(this.x, this.y, this.imageU, this.imageV, this.width / 2, this.height);
        blitRegion(this.x + this.width / 2, this.y, 200 - this.width / 2, this.imageV, this.width / 2, this.height);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void blitRegion(int x, int y, int u, int v, int w, int h) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(x,     y + h, 0).tex(u / 256.0,       (v + h) / 256.0).endVertex();
        buf.pos(x + w, y + h, 0).tex((u + w) / 256.0, (v + h) / 256.0).endVertex();
        buf.pos(x + w, y,     0).tex((u + w) / 256.0,  v / 256.0).endVertex();
        buf.pos(x,     y,     0).tex(u / 256.0,         v / 256.0).endVertex();
        tess.draw();
    }
}

/* class IconToggleButton_NeoForge_1_21_x:
... original NeoForge source preserved for reference (BooleanConsumer from fastutil, Tooltip.create(), AbstractButton, etc.) ...
*/
