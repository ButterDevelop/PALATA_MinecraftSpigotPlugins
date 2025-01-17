package com.example;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ForgeSpigotChannel {

    private static final Logger LOGGER = LogManager.getLogger();

    // Метод для отправки пакета на сервер
    public static void sendToServer(ClientInfoPacket packet) {
        try {
            // Преобразуем объект в JSON
            Gson gson = new Gson();
            String json = gson.toJson(packet); // Сериализация объекта в строку JSON

            // Отправляем строку JSON на сервер
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(json); // Записываем JSON строку в буфер

            // Отправляем через ClientPlayNetworking
            ClientPlayNetworking.send(new ClientInfoPayload(buf.readByteArray()));

            LOGGER.info("Пакет отправлен на сервер.");
        } catch (Exception e) {
            LOGGER.error("Ошибка при отправке пакета на сервер", e);
        }
    }
}
