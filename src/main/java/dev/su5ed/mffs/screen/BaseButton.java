package dev.su5ed.mffs.screen;

// 1.12.2 Backport: BaseButton
// AbstractButton → GuiButton (net.minecraft.client.gui.GuiButton)
// onPress(InputWithModifiers) → firePress() called by BaseScreen.actionPerformed()
// renderContents(GuiGraphics) → drawButton(Minecraft, int, int, float)
// SPRITES / blitSprite removed; custom renderFg() for subclasses
// Identifier → ResourceLocation; ARGB removed

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseButton extends GuiButton {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(200);

    private final Runnable onPress;

    public BaseButton(int x, int y, int width, int height, Runnable onPress) {
        super(NEXT_ID.getAndIncrement(), x, y, width, height, "");
        this.onPress = onPress;
    }

    /** Called by BaseScreen.actionPerformed() when this button is clicked. */
    public void firePress() {
        if (this.enabled) {
            this.onPress.run();
        }
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        this.hovered = mouseX >= this.x && mouseY >= this.y
            && mouseX < this.x + this.width && mouseY < this.y + this.height;
        renderFg(mc, mouseX, mouseY, partialTicks);
    }

    protected abstract void renderFg(Minecraft mc, int mouseX, int mouseY, float partialTicks);
}

/* class BaseButton_NeoForge_1_21_x:

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public abstract class BaseButton extends AbstractButton {
    private final Runnable onPress;

    public BaseButton(int x, int y, int width, int height, Runnable onPress) {
        super(x, y, width, height, Component.empty());
        this.onPress = onPress;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.run();
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Identifier widgetsLocation = SPRITES.get(this.active, this.isHovered());
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, widgetsLocation, getX(), getY(), this.width, this.height, ARGB.white(this.alpha));
        renderFg(guiGraphics, minecraft, mouseX, mouseY, partialTick);
    }

    protected abstract void renderFg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY, float partialTick);

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
*/
