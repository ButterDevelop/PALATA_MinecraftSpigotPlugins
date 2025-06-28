package org.butterdevelop.chatprankmod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

public class ModConfig {
    @SerializedName("nicknames")
    public List<String> nicknames;
    @SerializedName("realNames")
    public List<String> realNames;

    // Словарь никнейм -> реальное имя
    public Map<String, String> nickToReal;

    public static ModConfig load() {
        try {
            Path cfgDir = FabricLoader.getInstance().getConfigDir();
            Files.createDirectories(cfgDir);
            Path cfgFile = cfgDir.resolve("chatprankmod.json");
            if (Files.notExists(cfgFile)) {
                // копируем шаблон из ресурсов при первом запуске
                try (InputStream in = ModConfig.class.getResourceAsStream("/defaults/chatprankmod.json")) {
                    assert in != null;
                    Files.copy(in, cfgFile);
                }
            }
            String json = Files.readString(cfgFile);
            ModConfig cfg = new Gson().fromJson(json, ModConfig.class);

            // Строим map из двух параллельных списков
            cfg.nickToReal = new HashMap<>();
            int size = Math.min(cfg.nicknames.size(), cfg.realNames.size());
            for (int i = 0; i < size; i++) {
                // приводим к нижнему регистру для нечувствительности
                cfg.nickToReal.put(
                        cfg.nicknames.get(i).toLowerCase(),
                        cfg.realNames.get(i).toLowerCase()
                );
            }

            return cfg;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load chatprankmod.json", e);
        }
    }
}
