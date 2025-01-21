package com.butterdevelop.dotachatwheel;

import me.shedaniel.clothconfig.api.ConfigScreenFactory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class DotachatwheelClient implements ClientModInitializer {
	private static final MinecraftClient client = MinecraftClient.getInstance();

	@Override
	public void onInitializeClient() {
		// Регистрация клавиши для открытия меню настроек
		KeyBinding openConfigKey = new KeyBinding("Open Chat Wheel Config", GLFW.GLFW_KEY_C, "key.categories.gameplay");
		KeyBindingHelper.registerKeyBinding(openConfigKey);

		// Добавляем обработчик клавиши для открытия конфигурации
		ClientTickCallback.EVENT.register(client -> {
			if (openConfigKey.isPressed()) {
				openConfigScreen();
			}
		});
	}

	private void openConfigScreen() {
		Screen configScreen = ConfigScreen.createConfigScreen().create(MinecraftClient.getInstance().currentScreen);
		MinecraftClient.getInstance().openScreen(configScreen);
	}
}
