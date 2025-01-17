package org.butterdevelop.server;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.butterdevelop.server.listener.PlayerListener;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServerAntiCheat extends JavaPlugin {

    private final Logger logger = getLogger();
    private final Set<Player> playerSet = new HashSet<>();
    private FileLogger fileLogger;

    @Override
    public void onEnable() {
        Config whitelistConfig;
        Config config;

        try {
            whitelistConfig = new Config("whitelist.yml", this);
            config = new Config("config.yml", this);
            File logFile = new File(getDataFolder(), "logs.txt");
            fileLogger = new FileLogger(logFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Configuration or logger initialization failed: " + e.getMessage(), e);
            // Завершаем инициализацию плагина, если критическая ошибка при загрузке конфигов или логгера
            getPluginLoader().disablePlugin(this);
            return;
        }

        // Регистрируем обработчик плагин-сообщений
        var packetHandler = new PacketHandler(this, config, whitelistConfig);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "butterdevelop:anticheat", packetHandler);

        // Регистрируем слушателя событий
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this, config), this);
    }

    @Override
    public void onDisable() {
        if (fileLogger != null) {
            fileLogger.close();
        }
    }

    public Set<Player> getPlayerSet() {
        return playerSet;
    }

    /**
     * Логгирует сообщение на консоль и в файл с отметкой времени.
     *
     * @param level   уровень логирования
     * @param message сообщение для логирования
     */
    public void log(Level level, String message) {
        logger.log(level, message);
        if (fileLogger != null) {
            String timestampedMessage = String.format("[%s] %s\n",
                    new Timestamp(System.currentTimeMillis()), message);
            fileLogger.log(timestampedMessage);
        }
    }
}
