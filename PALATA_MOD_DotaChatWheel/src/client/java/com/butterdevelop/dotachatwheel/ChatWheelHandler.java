package com.butterdevelop.dotachatwheel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import static com.butterdevelop.dotachatwheel.ChatWheelRenderer.WHEEL_RADIUS;

public class ChatWheelHandler {
    private static boolean wheelActive = false;
    private static int selectedPhrase = -1;

    public static void handleKeyPress() {
        if (ModKeyBindings.openChatWheel.isPressed()) {
            wheelActive = true;
        } else {
            if (wheelActive && selectedPhrase != -1) {
                sendChatMessage(selectedPhrase);
            }
            wheelActive = false;
        }
    }

    public static void handleMouseMovement(int mouseX, int mouseY) {
        if (wheelActive) {
            // Определяем, на какую фразу наводит мышь
            selectedPhrase = getSelectedPhrase(mouseX, mouseY);
        }
    }

    private static void sendChatMessage(int selectedPhrase) {
        String message = ChatWheelRenderer.PHRASES[selectedPhrase];

        // Отправляем команду /teammsg
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(Text.literal("/teammsg " + message), false);
    }

    private static int getSelectedPhrase(int mouseX, int mouseY) {
        for (int i = 0; i < ChatWheelRenderer.PHRASES.length; i++) {
            float angle = i * (float) (2 * Math.PI / ChatWheelRenderer.PHRASES.length);
            int x = (int) (Math.cos(angle) * WHEEL_RADIUS);
            int y = (int) (Math.sin(angle) * WHEEL_RADIUS);

            if (ChatWheelRenderer.isMouseOver(mouseX, mouseY, x, y)) {
                return i;
            }
        }
        return -1;
    }

    public static void renderWheel(DrawContext context, int mouseX, int mouseY) {
        if (wheelActive) {
            ChatWheelRenderer.renderWheel(context, mouseX, mouseY);
        }
    }
}
