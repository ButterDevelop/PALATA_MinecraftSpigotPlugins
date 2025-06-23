package org.butterdevelop.slashSex;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StatsManager {
    private static StatsManager instance;
    private final JavaPlugin plugin;
    private final File file;
    private final FileConfiguration cfg;

    private StatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "sex_stats.yml");
        // Создаем папку и файл, если не существует
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать sex_stats.yml: " + e.getMessage());
            }
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new StatsManager(plugin);
        }
    }

    public static StatsManager get() {
        return instance;
    }

    public void recordUse(UUID actor, UUID target) {
        // Активность
        String aKey = actor.toString();
        cfg.set("active." + aKey, cfg.getInt("active." + aKey, 0) + 1);
        // Пассивность
        String tKey = target.toString();
        cfg.set("passive." + tKey, cfg.getInt("passive." + tKey, 0) + 1);
        save();
    }

    public List<Map.Entry<String, Integer>> topActive(int limit) {
        return Objects.requireNonNull(cfg.getConfigurationSection("active")).getKeys(false).stream()
                .map(k -> Map.entry(k, cfg.getInt("active." + k)))
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<String, Integer>> topPassive(int limit) {
        return Objects.requireNonNull(cfg.getConfigurationSection("passive")).getKeys(false).stream()
                .map(k -> Map.entry(k, cfg.getInt("passive." + k)))
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить sex_stats.yml");
        }
    }
}