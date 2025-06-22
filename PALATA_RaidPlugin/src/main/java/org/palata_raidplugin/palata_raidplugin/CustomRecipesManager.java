package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

/**
 * Менеджер кастомных рецептов и обработки варки зелий.
 * Убирает старый крафт золотых яблок и smithing-улучшения брони,
 * добавляет крафт netherite-брони из слитков,
 * и делает любое зелье, сваренное со звездой Незера, невидимым (скрывает пузырьки).
 */
public class CustomRecipesManager implements Listener {
    private final JavaPlugin plugin;

    public CustomRecipesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // Регистрируем рецепты
        registerRecipes();
        // Регистрируем слушатель варки зелий
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Вызывать из onEnable() главного плагина: */
    public static void init(JavaPlugin plugin) {
        new CustomRecipesManager(plugin);
    }

    private void registerRecipes() {
        removeDefaultGoldenAppleRecipe();
        registerCustomGoldenAppleRecipe();
        removeNetheriteArmorSmithing();
        registerCraftingNetheriteArmor();
    }

    /**
     * Удаляет стандартный рецепт золотого яблока
     */
    private void removeDefaultGoldenAppleRecipe() {
        Bukkit.getServer().getRecipesFor(new ItemStack(Material.GOLDEN_APPLE))
                .forEach(recipe -> {
                    if (recipe instanceof Keyed) {
                        Bukkit.removeRecipe(((Keyed) recipe).getKey());
                    }
                });
    }

    /**
     * Новый рецепт золотого яблока: обычное яблоко + 2 блока золота
     * Два варианта: блоки сверху или по бокам
     */
    private void registerCustomGoldenAppleRecipe() {
        NamespacedKey key1 = new NamespacedKey(plugin, "golden_apple_vertical");
        ShapedRecipe vert = new ShapedRecipe(key1, new ItemStack(Material.GOLDEN_APPLE));
        vert.shape(" B ", " A ", " B ");
        vert.setIngredient('B', Material.GOLD_BLOCK);
        vert.setIngredient('A', Material.APPLE);
        Bukkit.addRecipe(vert);

        NamespacedKey key2 = new NamespacedKey(plugin, "golden_apple_horizontal");
        ShapedRecipe hor = new ShapedRecipe(key2, new ItemStack(Material.GOLDEN_APPLE));
        // Два золотых блока по бокам от яблока
        hor.shape("   ", "BAB", "   ");
        hor.setIngredient('B', Material.GOLD_BLOCK);
        hor.setIngredient('A', Material.APPLE);
        Bukkit.addRecipe(hor);
    }

    private void removeNetheriteArmorSmithing() {
        Material[] armors = {
                Material.NETHERITE_HELMET,
                Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS,
                Material.NETHERITE_BOOTS
        };
        for (Material mat : armors) {
            for (Recipe recipe : Bukkit.getRecipesFor(new ItemStack(mat))) {
                if (recipe instanceof SmithingRecipe) {
                    Bukkit.removeRecipe(((Keyed) recipe).getKey());
                }
            }
        }
    }

    private void registerCraftingNetheriteArmor() {
        Material ingot = Material.NETHERITE_INGOT;
        // Шлем
        NamespacedKey helmKey = new NamespacedKey(plugin, "netherite_helmet_ingot");
        ShapedRecipe helm = new ShapedRecipe(helmKey, new ItemStack(Material.NETHERITE_HELMET));
        helm.shape("AAA", "A A"); helm.setIngredient('A', ingot);
        Bukkit.addRecipe(helm);
        // Нагрудник
        NamespacedKey chestKey = new NamespacedKey(plugin, "netherite_chestplate_ingot");
        ShapedRecipe chest = new ShapedRecipe(chestKey, new ItemStack(Material.NETHERITE_CHESTPLATE));
        chest.shape("A A", "AAA", "AAA"); chest.setIngredient('A', ingot);
        Bukkit.addRecipe(chest);
        // Поножи
        NamespacedKey legsKey = new NamespacedKey(plugin, "netherite_leggings_ingot");
        ShapedRecipe legs = new ShapedRecipe(legsKey, new ItemStack(Material.NETHERITE_LEGGINGS));
        legs.shape("AAA", "A A", "A A"); legs.setIngredient('A', ingot);
        Bukkit.addRecipe(legs);
        // Ботинки
        NamespacedKey bootsKey = new NamespacedKey(plugin, "netherite_boots_ingot");
        ShapedRecipe boots = new ShapedRecipe(bootsKey, new ItemStack(Material.NETHERITE_BOOTS));
        boots.shape("A A", "A A"); boots.setIngredient('A', ingot);
        Bukkit.addRecipe(boots);
    }
}
