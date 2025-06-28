package org.butterdevelop.chatprankmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.sound.SoundEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class ChatPrankMod implements ModInitializer {

    private ModConfig config;

    private static final UUID TARGET_UUID_FOR_WARDEN = UUID.fromString("870f3707-37e2-3a58-aed3-08e51bc34c75"); // MaXiMAA

    private void onAnyChatMessage(String raw, SignedMessage sender) {
        // Убираем префикс:
        // - либо "-> [TeamName] <Nickname> "
        // - либо просто "<Nickname> "
        String content = raw.replaceFirst("^(?:->\\s*)?\\[[^\\]]+\\]\\s*<[^>]+>\\s*|^<[^>]+>\\s*", "");

        // Приводим уже очищенный текст к нижнему регистру
        String text = content.toLowerCase();

        // троллинг со звуком вардена
        if (text.contains("warden") && sender != null && sender.getSender() != null &&
                sender.getSender().equals(TARGET_UUID_FOR_WARDEN)) {
            MinecraftClient client = MinecraftClient.getInstance();
            assert client.player != null;
            client.player.playSound(SoundEvents.ENTITY_WARDEN_EMERGE, 1.0F, 1.0F);
            return;
        }

        // своё имя (никнейм из сессии)
        String selfNick = MinecraftClient.getInstance()
                .getSession()
                .getUsername();
        String selfLower = selfNick.toLowerCase();

        // реальное имя из конфига (или fallback на сам ник)
        String real = config.nickToReal.getOrDefault(selfLower, selfLower);

        // проверяем наличие слова никнейма ИЛИ реального имени с границами слова \b для точного матча
        String nickPattern = "(?<=^|[^\\p{L}\\p{N}])"
                + Pattern.quote(selfLower)
                + "(?=$|[^\\p{L}\\p{N}])";
        String realPattern = "(?<=^|[^\\p{L}\\p{N}])"
                + Pattern.quote(real)
                + "(?=$|[^\\p{L}\\p{N}])";
        Pattern pNick = Pattern.compile(nickPattern);
        Pattern pReal = Pattern.compile(realPattern);

        if ((pNick.matcher(text).find() || pReal.matcher(text).find()) && text.contains("сосал")) {
            // сохраняем восклицательный знак, если он нужен
            boolean isTeamChat = raw.matches("^(?:->\\s*)?\\[[^\\]]+\\]\\s*<[^>]+>.*");
            String prefix = isTeamChat ? "" : "!";
            String reply = prefix + "Да";

            // и отправляем его в чат
            MinecraftClient client = MinecraftClient.getInstance();
            Objects.requireNonNull(client.getNetworkHandler()).sendChatMessage(reply);
        }
    }

    @Override
    public void onInitialize() {
        // 1. Загружаем конфиг
        config = ModConfig.load();

        // 2. Перехват исходящих чатов
        ClientSendMessageEvents.MODIFY_CHAT.register((message) -> {
            // Если в сообщении нет букв/цифр — возвращаем как есть
            if (!message.matches(".*[\\p{L}\\p{N}].*")) {
                return message;
            }
            // Если нет запятых — нечего заменять
            if (!message.contains(",")) {
                return message;
            }
            // Иначе — ставим нашу вставку после каждой запятой
            return message.replaceAll(",", ", бля,");
        });

        // 3. Прослушиваем входящие чаты
        ClientReceiveMessageEvents.CHAT.register((msg, sender, sig, comps, sys) -> {
            onAnyChatMessage(msg.getString(), sender);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            onAnyChatMessage(message.getString(), null);
        });
    }
}
