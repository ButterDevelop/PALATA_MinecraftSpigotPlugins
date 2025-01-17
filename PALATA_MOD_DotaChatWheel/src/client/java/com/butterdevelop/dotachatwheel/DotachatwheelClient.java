package com.butterdevelop.dotachatwheel;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

public class DotachatwheelClient implements ClientModInitializer {
	private static final MinecraftClient client = MinecraftClient.getInstance();

	@Override
	public static void onInitializeClient() {
		// Регистрируем обработку нажатия клавиш
		ClientTickCallback.EVENT.register(client -> ChatWheelHandler.handleKeyPress());

		// Регистрируем обработку движения мыши
		MouseEventCallback.EVENT.register((mouseX, mouseY) -> ChatWheelHandler.handleMouseMovement(mouseX, mouseY));
	}

	@Override
	public static void onRender(MatrixStack matrices) {
		int mouseX = (int) client.mouse.getX();
		int mouseY = (int) client.mouse.getY();

		// Отображаем колесо чата
		ChatWheelHandler.renderWheel(matrices, mouseX, mouseY);
	}
}