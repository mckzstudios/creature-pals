package com.owlmaddie.ui;

import org.lwjgl.glfw.GLFW;

import com.owlmaddie.player2.TTS;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class TTSToggleButton {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 20;
    private static final int PADDING = 10;

    private static boolean _wasMouseDown;

    // Screen-space coords, calculated each frame
    private static int x, y;

    public static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        x = client.getWindow().getScaledWidth() - WIDTH - PADDING;
        y = 2*PADDING;

        // Draw button background (semi-transparent black)
        // fill(matrices, x, y, x + WIDTH, y + HEIGHT, 0xAA000000);

        // Draw centered label with shadow
        String label = TTS.enabled ? "TTS: ON" : "TTS: OFF";
        int textWidth = textRenderer.getWidth(label);
        int textHeight = textRenderer.fontHeight;
        float textX = x + (WIDTH - textWidth) / 2f;
        float textY = y + 6;

        boolean hovering = isMouseHovering();

        int color = hovering ? 0xFFFF00 : (TTS.enabled ? 0x00FF00 : 0xFF0000);

        // TODO: fill doesn't seem to do anthing here
        int pad = 6;
        ctx.fill((int) (textX - pad), (int) (textY - pad), (int) (textX + textWidth + pad*2), (int) (textY + textHeight + pad*2), 0x555555);

        // DrawableHelper.fill(matrices, rectX, rectY, rectX + rectWidth, rectY + rectHeight, 0xAA000000);

        ctx.drawText(textRenderer,
            label,
            (int)textX,
            (int)textY,
            color,
            true // <-- this enables the shadow
        );
    }
    
    public static boolean tick() {
        MinecraftClient client = MinecraftClient.getInstance();

        boolean isMouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        boolean clicked = false;

        if (isMouseDown && !_wasMouseDown) {
            if (isMouseHovering()) {
                clicked = true;
            }
        }
        _wasMouseDown = isMouseDown;

        return clicked;
    }

    private static boolean isMouseHovering() {
        MinecraftClient client = MinecraftClient.getInstance();
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        return mouseX >= x && mouseX <= x + WIDTH &&
               mouseY >= y && mouseY <= y + HEIGHT;
    }
}
