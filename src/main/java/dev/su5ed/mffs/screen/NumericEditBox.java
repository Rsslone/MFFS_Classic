package dev.su5ed.mffs.screen;

// 1.12.2 Backport: NumericEditBox
// EditBox → GuiTextField (net.minecraft.client.gui.GuiTextField)
// charTyped(CharacterEvent) → textboxKeyTyped(char, int)
// canConsumeInput() → isFocused()
// insertText() → writeText()
// Font → FontRenderer; Component → ITextComponent

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.ITextComponent;

public class NumericEditBox extends GuiTextField {
    private Runnable responder;

    public NumericEditBox(FontRenderer font, int x, int y, int width, int height, ITextComponent message) {
        super(0, font, x, y, width, height);
    }

    public void setResponder(Runnable responder) {
        this.responder = responder;
    }

    public void setValue(String text) {
        setText(text);
    }

    public String getValue() {
        return getText();
    }

    @Override
    public boolean textboxKeyTyped(char typedChar, int keyCode) {
        // Allow control characters (typedChar == 0), backspace, and digits
        if (typedChar == 0 || keyCode == 14 /* Keyboard.KEY_BACK */ || Character.isDigit(typedChar)) {
            boolean changed = super.textboxKeyTyped(typedChar, keyCode);
            if (changed && this.responder != null) {
                this.responder.run();
            }
            return changed;
        }
        return false;
    }
}

/* class NumericEditBox_NeoForge_1_21_x:
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;

public class NumericEditBox extends EditBox {
    public NumericEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int codePoint = event.codepoint();
        if (canConsumeInput() && Character.isDigit(codePoint)) {
            insertText(Character.toString(codePoint));
            return true;
        }
        return false;
    }
}
*/
