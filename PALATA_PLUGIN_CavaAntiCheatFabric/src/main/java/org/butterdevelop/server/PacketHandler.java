package org.butterdevelop.server;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.butterdevelop.common.ClientInfoPacket;
import org.butterdevelop.common.InspectionResult;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public class PacketHandler implements PluginMessageListener {

    private final Config          whitelistConfig;
    private final Config          config;
    private final ServerAntiCheat instance;

    public PacketHandler(ServerAntiCheat instance, Config config, Config whitelistConfig) {
        this.instance        = instance;
        this.config          = config;
        this.whitelistConfig = whitelistConfig;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!"butterdevelop:anticheat".equals(channel)) {
            return;
        }

        try {
            ClientInfoPacket clientInfoPacket = parsePacket(message);
            instance.getPlayerSet().remove(player);

            if (clientInfoPacket.getModsInspectionResult() != InspectionResult.NORMAL ||
                    clientInfoPacket.getTextureInspectionResult() != InspectionResult.NORMAL) {

                String kickReason = evaluateInspectionResult(clientInfoPacket.getModsInspectionResult()) + "\n" +
                        evaluateInspectionResult(clientInfoPacket.getTextureInspectionResult());

                player.kickPlayer(kickReason);
                instance.log(Level.WARNING, player.getName() + " kicked for reason: " + clientInfoPacket.getModsInspectionMessage());
            } else {
                String checksumEvaluationResult = evaluateChecksums(
                        clientInfoPacket.getModsChecksumMap(),
                        clientInfoPacket.getTextureChecksumMap()
                );

                if (!checksumEvaluationResult.isEmpty()) {
                    player.kickPlayer(checksumEvaluationResult);
                    instance.log(Level.WARNING, player.getName() + " kicked for reason:\n" + checksumEvaluationResult);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            player.kickPlayer("Unable to verify the presence of the client anti cheat");
            instance.log(Level.SEVERE, player.getName() + " kicked for: " + e.getMessage());
        }
    }

    private ClientInfoPacket parsePacket(byte[] message) throws IOException {
        try {
            // Извлекаем строку JSON из пакета
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String messageStringJson = in.readLine();

            // Преобразуем JSON обратно в объект ClientInfoPacket
            Gson gson = new Gson();
            return gson.fromJson(messageStringJson.substring(messageStringJson.indexOf('{')), ClientInfoPacket.class); // Десериализация JSON в объект
        } catch (JsonSyntaxException e) {
            throw new IOException("Ошибка десериализации JSON", e);
        }
    }

    private String evaluateInspectionResult(InspectionResult result) {
        return switch (result) {
            case FILE_NOT_FOUND          -> "Mod file has been deleted by a third party";
            case IO_EXCEPTION            -> "Unable to fetch mod information";
            case HASH_FUNCTION_NOT_FOUND -> "Hash function not found";
            case INVALID_TEXTURE_FORMAT  -> "Invalid texture format";
            default -> "";
        };
    }

    private String evaluateChecksums(Map<String, String> modsChecksum, Map<String, String> textureChecksum) {
        StringBuilder report = new StringBuilder();

        ConfigurationSection whitelistedMods =
                whitelistConfig.getConfig().getConfigurationSection("whitelisted-mods");
        if (whitelistedMods == null) {
            whitelistedMods = whitelistConfig.getConfig().createSection("whitelisted-mods");
        }

        ConfigurationSection finalWhitelistedMods = whitelistedMods;
        modsChecksum.forEach((modName, checksum) -> {
            if (modName.equals("minecraft") && !config.getConfig().getBoolean("inspect-minecraft")) return;
            if (modName.equals("forge") && !config.getConfig().getBoolean("inspect-forge")) return;

            if (finalWhitelistedMods.contains(modName)) {
                if (!Objects.equals(finalWhitelistedMods.getString(modName), checksum)) {
                    report.append("Mod tampered: ").append(modName).append("\n");
                }
            } else {
                report.append("Mod not allowed: ").append(modName).append("\n");
            }
        });

        ConfigurationSection whitelistedTextures =
                whitelistConfig.getConfig().getConfigurationSection("whitelisted-textures");
        if (whitelistedTextures == null) {
            whitelistedTextures = whitelistConfig.getConfig().createSection("whitelisted-textures");
        }

        ConfigurationSection finalWhitelistedTextures = whitelistedTextures;
        textureChecksum.forEach((textureName, checksum) -> {
            if (finalWhitelistedTextures.contains(textureName)) {
                if (!Objects.equals(finalWhitelistedTextures.getString(textureName), checksum)) {
                    report.append("Texture tampered: ").append(textureName).append("\n");
                }
            } else {
                report.append("Texture not allowed: ").append(textureName).append("\n");
            }
        });

        return report.toString();
    }
}
