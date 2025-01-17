package com.butterdevelop.dotachatwheel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;

public class ChatWheelRenderer {

    // Список фраз
    public static final String[] PHRASES = {"> Hello!", "> Good Luck", "> Push!", "> Retreat!"};

    private static final MinecraftClient client = MinecraftClient.getInstance();
    public static final int WHEEL_RADIUS = 100;

    public static void renderWheel(DrawContext context, int mouseX, int mouseY) {
        // Начинаем рендеринг
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR); // Режим рисования (линии)

        // Рисуем фразы по окружности
        for (int i = 0; i < PHRASES.length; i++) {
            float angle = i * (float) (2 * Math.PI / PHRASES.length);
            int x = (int) (Math.cos(angle) * WHEEL_RADIUS);
            int y = (int) (Math.sin(angle) * WHEEL_RADIUS);

            // Определяем подсветку фразы
            if (isMouseOver(mouseX, mouseY, x, y)) {
                // Увеличиваем фразу при наведении
                renderText(context, PHRASES[i], x, y, 1.5f);
            } else {
                renderText(context, PHRASES[i], x, y, 1.0f);
            }
        }

        // Завершаем рендеринг
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void renderText(DrawContext context, String text, int x, int y, float scale) {
        int color = 0xFFFFFF; // Цвет текста

        // Масштабируем текст
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);

        // Используем метод drawText для рендеринга текста
        context.drawText(client.textRenderer, text, x, y, color, false);

        context.getMatrices().pop();
    }

    public static boolean isMouseOver(int mouseX, int mouseY, int x, int y) {
        // Вычисляем расстояние от центра до текущего положения мыши
        int dx = mouseX - client.getWindow().getWidth() / 2 - x;
        int dy = mouseY - client.getWindow().getHeight() / 2 - y;
        return Math.sqrt(dx * dx + dy * dy) < WHEEL_RADIUS;
    }
}
