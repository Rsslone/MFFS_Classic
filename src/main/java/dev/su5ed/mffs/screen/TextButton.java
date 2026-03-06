package dev.su5ed.mffs.screen;

// 1.12.2 Backport: TextButton
// Button.Plain → GuiButton; OnPress → Runnable (button param ignored)
// getMessage() → still used via getDisplayString() override
// Component → ITextComponent

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.ITextComponent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class TextButton extends GuiButton {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(600);

    private final Supplier<ITextComponent> messageSupplier;
    private final Runnable onPress;

    public TextButton(int x, int y, int width, int height, Supplier<ITextComponent> messageSupplier, Runnable onPress) {
        super(NEXT_ID.getAndIncrement(), x, y, width, height, "");
        this.messageSupplier = messageSupplier;
        this.onPress = onPress;
    }

    public String getCurrentMessage() {
        return this.messageSupplier.get().getFormattedText();
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        // Temporarily set displayString for vanilla rendering
        this.displayString = getCurrentMessage();
        super.drawButton(mc, mouseX, mouseY, partialTicks);
        this.displayString = "";
    }

    /** Called by BaseScreen.actionPerformed() */
    public void firePress() {
        if (this.enabled) {
            this.onPress.run();
        }
    }
}

/* class TextButton_NeoForge_1_21_x:
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

public class TextButton extends Button.Plain {
    private final Supplier<Component> messageSupplier;

    public TextButton(int x, int y, int width, int height, Supplier<Component> messageSupplier, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, s -> Component.empty());
        this.messageSupplier = messageSupplier;
    }

    @Override
    public Component getMessage() {
        return this.messageSupplier.get();
    }
}
*/
