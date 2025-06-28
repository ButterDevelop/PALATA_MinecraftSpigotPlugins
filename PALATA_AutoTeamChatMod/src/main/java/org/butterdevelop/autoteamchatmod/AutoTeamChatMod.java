package org.butterdevelop.autoteamchatmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;

public class AutoTeamChatMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // Перехватываем все чат‐сообщения до отправки
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            // Пропускаем все явные команды и глобальный чат через '!'
            if (message.startsWith("/") || message.startsWith("!")) {
                return true; // слать дальше, без изменений
            }

            // Всё остальное - в команду teammsg
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.networkHandler.sendChatCommand("teammsg " + message);
            }

            return false;
        });

        ClientSendMessageEvents.MODIFY_CHAT.register((message) -> {
            if (message.startsWith("!")) {
                return message.substring(1);
            }
            return message;
        });
    }
}
