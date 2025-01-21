package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class CavaAntiCheatFabricClient implements ClientModInitializer {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final String MODID = "butterdevelopanticheat";

	@Override
	public void onInitializeClient() {
		// Регистрация канала и сообщений
		PayloadTypeRegistry.playC2S().register(ClientInfoPayload.ID, ClientInfoPayload.CODEC);

		// Регистрируем событие подключения к игре (вместо EntityJoinWorldEvent)
		ClientPlayConnectionEvents.JOIN.register(
				(handler, sender, client) -> sendInfo()
		);

		// Регистрируем событие перезагрузки ресурсов
		InvalidateRenderStateCallback.EVENT.register(this::sendInfo);
	}

	private void sendInfo() {
		// Отправка пакета на сервер после подключения игрока
		ClientInfoPacket packet = inspectClient();
		// Отправка сообщения через зарегистрированный канал
		ForgeSpigotChannel.sendToServer(packet);
	}

	private ClientInfoPacket inspectClient(){
		ClientInfoPacket clientInfoPacket  = new ClientInfoPacket();
		clientInfoPacket.setModsInspectionResult(InspectionResult.NORMAL);
		clientInfoPacket.setTextureInspectionResult(InspectionResult.NORMAL);
		clientInfoPacket.setShadersInspectionResult(InspectionResult.NORMAL);
		inspectMods(clientInfoPacket);
		inspectResourcePacks(clientInfoPacket);
		inspectShaderPacks(clientInfoPacket);
		return clientInfoPacket;
	}

	private void inspectMods(ClientInfoPacket clientInfoPacket) {
		// Получаем список модов с помощью Fabric Loader API
		List<ModContainer> mods = (List<ModContainer>) FabricLoader.getInstance().getAllMods();
		for (ModContainer modContainer : mods) {
			try {
				ModOrigin origin = modContainer.getOrigin();

				// Проверяем, что origin не имеет типа NESTED, так как они не имеют путей
				if (origin.getKind() == ModOrigin.Kind.NESTED) {
					continue;  // Пропускаем моды типа NESTED
				}

				// Получаем путь к файлу мода
				Path modPath = origin.getPaths().get(0);

				// Фильтрация системных путей, чтобы избежать работы с ненужными файлами (например, java-runtime)
				if (modPath.toString().contains("java-runtime") || modPath.toString().contains("jre")) {
					continue;  // Пропускаем такие пути
				}

				File modFile = modPath.toFile();

				// Проверяем существование файла
				if (modFile.exists()) {
					String checksum = HashingFunction.getFileChecksum(modFile);
					clientInfoPacket.addModChecksum(modContainer.getMetadata().getId(), checksum);
					clientInfoPacket.setModsInspectionResult(InspectionResult.NORMAL);
					clientInfoPacket.setModsInspectionMessage("Every mod has been successfully hashed");
				} else {
					clientInfoPacket.setModsInspectionResult(InspectionResult.FILE_NOT_FOUND);
					clientInfoPacket.setModsInspectionMessage(modFile.getName() + " not found");
					break; // Прерываем обработку, если файл не найден
				}
			} catch (IOException e) {
				clientInfoPacket.setModsInspectionResult(InspectionResult.IO_EXCEPTION);
				clientInfoPacket.setModsInspectionMessage(e.getMessage());
				LOGGER.error("IOException while inspecting mod: ", e);
				break; // Прерываем обработку в случае ошибки ввода/вывода
			} catch (NoSuchAlgorithmException e) {
				clientInfoPacket.setModsInspectionResult(InspectionResult.HASH_FUNCTION_NOT_FOUND);
				clientInfoPacket.setModsInspectionMessage(e.getMessage());
				LOGGER.error("NoSuchAlgorithmException while hashing mod: ", e);
				break; // Прерываем обработку в случае ошибки хеширования
			}
		}
	}

	private void inspectResourcePacks(ClientInfoPacket clientInfoPacket){
		File resourcePacksFolder = MinecraftClient.getInstance().getResourcePackDir().toFile();
		File[] files = resourcePacksFolder.listFiles();
		if (files == null) return;

		for(File file : files){
			if(file.isDirectory()){
				clientInfoPacket.setTextureInspectionResult(InspectionResult.INVALID_TEXTURE_FORMAT);
				clientInfoPacket.setTextureInspectionMessage("Texture called " + file.getName() + " as directory found");
				break;
			} else {
				try {
					clientInfoPacket.addTextureChecksum(file.getName(), HashingFunction.getFileChecksum(file));
					clientInfoPacket.setTextureInspectionResult(InspectionResult.NORMAL);
					clientInfoPacket.setTextureInspectionMessage("Every texture has been successfully hashed");
				} catch (NoSuchAlgorithmException e) {
					clientInfoPacket.setTextureInspectionResult(InspectionResult.HASH_FUNCTION_NOT_FOUND);
					clientInfoPacket.setTextureInspectionMessage(e.getMessage());
					LOGGER.error(e);
					break;
				} catch (IOException e) {
					clientInfoPacket.setTextureInspectionResult(InspectionResult.IO_EXCEPTION);
					clientInfoPacket.setTextureInspectionMessage(e.getMessage());
					LOGGER.error(e);
					break;
				}
			}
		}
	}

	private void inspectShaderPacks(ClientInfoPacket clientInfoPacket) {
		// Получаем путь к папке с ресурсами (resourcepacks)
		File resourcePacksFolder = MinecraftClient.getInstance().getResourcePackDir().toFile();

		// Папка shaderpacks расположена рядом с resourcepacks
		File shaderPacksFolder = new File(resourcePacksFolder.getParentFile(), "shaderpacks");

		// Получаем все файлы и папки в папке shaderpacks
		File[] files = shaderPacksFolder.listFiles();

		// Если папка пуста или не существует, выходим
		if (files == null) return;

		// Обрабатываем все файлы в папке
		for (File file : files) {
			if (file.isDirectory()) {
				// Если это директория, ставим ошибку, так как текстуры не могут быть каталогами
				clientInfoPacket.setShadersInspectionResult(InspectionResult.INVALID_SHADER_FORMAT);
				clientInfoPacket.setShadersInspectionMessage("Shader called " + file.getName() + " as directory found");
				break;
			} else {
				try {
					// Для файлов, считаем их хеш
					clientInfoPacket.addShadersChecksum(file.getName(), HashingFunction.getFileChecksum(file));
					clientInfoPacket.setShadersInspectionResult(InspectionResult.NORMAL);
					clientInfoPacket.setShadersInspectionMessage("Every shader has been successfully hashed");
				} catch (NoSuchAlgorithmException e) {
					clientInfoPacket.setShadersInspectionResult(InspectionResult.HASH_FUNCTION_NOT_FOUND);
					clientInfoPacket.setShadersInspectionMessage(e.getMessage());
					LOGGER.error(e);
					break;
				} catch (IOException e) {
					clientInfoPacket.setShadersInspectionResult(InspectionResult.IO_EXCEPTION);
					clientInfoPacket.setShadersInspectionMessage(e.getMessage());
					LOGGER.error(e);
					break;
				}
			}
		}
	}
}
