package com.butterdevelop.dotachatwheel;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static KeyBinding openChatWheel;

    public static KeyBinding keyBinding;

    public static void register() {
        openChatWheel = new KeyBinding("key.mod.open_chat_wheel", GLFW.GLFW_KEY_Y, "key.categories.gameplay");
        keyBinding = KeyBindingHelper.registerKeyBinding(openChatWheel);
    }
}
