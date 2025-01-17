package org.butterdevelop.server;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Config {

    private final File configFile;
    private final Logger logger;
    private final JavaPlugin plugin;
    private FileConfiguration config;

    /**
     * Конструктор для создания или загрузки конфигурационного файла из ресурсов плагина.
     *
     * @param configName Имя конфигурационного файла.
     * @param plugin     Инстанс плагина.
     * @throws IOException При ошибках создания файла.
     */
    public Config(@NotNull String configName, @NotNull JavaPlugin plugin) throws IOException {
        this(new File(plugin.getDataFolder(), configName), plugin);
    }

    /**
     * Конструктор для загрузки существующего конфигурационного файла.
     *
     * @param configFile Файл конфигурации.
     * @param plugin     Инстанс плагина.
     * @throws IOException При ошибках создания или загрузки файла.
     */
    public Config(@NotNull File configFile, @NotNull JavaPlugin plugin) throws IOException {
        this.configFile = configFile;
        this.plugin = plugin;
        this.logger = Bukkit.getLogger();

        if (!exists()) {
            createConfig();
        }
        loadConfig();
    }

    /**
     * Возвращает загруженную конфигурацию.
     *
     * @return FileConfiguration
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Сохраняет текущие значения конфигурации в файл.
     */
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while saving the config file " + ChatColor.RED + configFile.getName(), e);
        }
    }

    /**
     * Проверяет, существует ли конфигурационный файл.
     *
     * @return true, если файл существует, иначе false.
     */
    private boolean exists() {
        boolean exists = configFile.exists();
        logger.info(configFile.getName() + (exists ? " has been found" : " has not been found"));
        return exists;
    }

    /**
     * Создаёт конфигурационный файл, если он отсутствует.
     *
     * @throws IOException При ошибках создания файла.
     */
    private void createConfig() throws IOException {
        logger.info("Creating " + configFile.getName() + "...");
        File parentDir = configFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Error creating directory: " + parentDir.getName());
        }

        // Пытаемся загрузить ресурс из JAR плагина
        if (plugin.getResource(configFile.getName()) != null) {
            plugin.saveResource(configFile.getName(), false);
        } else if (!configFile.exists()) {
            if (configFile.createNewFile()) {
                logger.log(Level.INFO, configFile.getName() + " has been created");
            } else {
                logger.log(Level.SEVERE, "Error creating " + configFile.getName());
                throw new IOException("Error creating " + configFile.getName());
            }
        }
    }

    /**
     * Загружает конфигурацию из файла.
     */
    private void loadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        logger.info(configFile.getName() + " has been loaded");
    }
}
