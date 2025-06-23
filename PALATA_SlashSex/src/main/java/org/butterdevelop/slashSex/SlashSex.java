package org.butterdevelop.slashSex;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Objects;

public final class SlashSex extends JavaPlugin {

    @Override
    public void onEnable() {
        registerRecipes();
        getServer().getPluginManager().registerEvents(new SexListener(this), this);

        StatsManager.init(this);
        Objects.requireNonNull(getCommand("sex")).setExecutor(new SexStatsCommand(this));
    }

    private void registerRecipes() {
        // Обычный дилдо: красивое имя, цвет, шрифт, описание, эффект сияния
        ItemStack normal = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta nm = normal.getItemMeta();
        if (nm != null) {
            nm.setDisplayName("§d§lДилдо"); // светло-фиолетовый и жирный
            nm.setLore(Arrays.asList(
                    "§7Забавная игрушка из комнаты мамы",
                    "§7Используй, чтобы немного развлечься"
            ));
            // Добавляем "светящийся" эффект без настоящего зачарования
            nm.addEnchant(Enchantment.UNBREAKING, 1, true);
            nm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            normal.setItemMeta(nm);
        }

        ShapedRecipe rec1 = new ShapedRecipe(new NamespacedKey(this, "dildo"), normal);
        rec1.shape(" L ", " L ", "LLL");
        rec1.setIngredient('L', Material.LIGHTNING_ROD);
        getServer().addRecipe(rec1);

        // Усиленный дилдо: более насыщенный цвет, дополнительная глиттер-подсветка
        ItemStack strong = new ItemStack(Material.END_ROD);
        ItemMeta sm = strong.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§5§lСупер дилдо"); // тёмно-фиолетовый и жирный
            sm.setLore(Arrays.asList(
                    "§7Усиленная версия игрушки",
                    "§7Больше удовольствия, больше эффекта"
            ));
            // Эффект переливающегося сияния
            sm.addEnchant(Enchantment.UNBREAKING, 1, true);
            sm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            strong.setItemMeta(sm);
        }

        ShapedRecipe rec2 = new ShapedRecipe(new NamespacedKey(this, "super_dildo"), strong);
        rec2.shape(" E ", " E ", "EEE");
        rec2.setIngredient('E', Material.END_ROD);
        getServer().addRecipe(rec2);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
