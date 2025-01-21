package com.butterdevelop.dotachatwheel;

import me.shedaniel.clothconfig.api.ConfigBuilder;
import me.shedaniel.clothconfig.api.ConfigCategory;
import me.shedaniel.clothconfig.api.ConfigScreenFactory;
import me.shedaniel.clothconfig.api.entries.StringListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreen {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static ConfigScreenFactory<?> createConfigScreen() {
        ConfigBuilder builder = ConfigBuilder.create();
        builder.setTitle(Text.of("Chat Wheel Config"));

        // Добавляем категорию с фразами
        ConfigCategory phrasesCategory = builder.getOrCreateCategory(Text.of("Phrases"));

        StringListEntry phrasesEntry = new StringListEntry(Text.of("Chat Phrases"), ChatWheelRenderer.PHRASES);
        phrasesCategory.addEntry(phrasesEntry);

        // Строим и возвращаем экран настроек
        return builder.build();
    }
}
