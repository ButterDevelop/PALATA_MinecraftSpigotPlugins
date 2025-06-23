package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.List;

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

    // Шаг 1: подставляем правильный результат и сбрасываем cost
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent ev) {
        AnvilInventory inv = ev.getInventory();
        ItemStack base = inv.getItem(0);
        ItemStack star = inv.getItem(1);

        if (base == null || star == null) {
            ev.setResult(null);
            return;
        }
        Material t = base.getType();
        if ((t == Material.POTION || t == Material.SPLASH_POTION || t == Material.LINGERING_POTION)
                && star.getType() == Material.NETHER_STAR) {

            // 1) Создаём результат того же типа бутылки
            ItemStack result = new ItemStack(t);
            PotionMeta pm = (PotionMeta) base.getItemMeta();
            assert pm != null;
            PotionData pd = pm.getBasePotionData();

            // 2) Делаем extended (≈2× длительнее) невидимость
            pm.setBasePotionData(new PotionData(pd.getType(), true, pd.isUpgraded()));
            // 3) Скрываем пузырьки
            pm.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);

            // 4) Делаем «звёздное» имя и описание
            pm.setDisplayName(ChatColor.LIGHT_PURPLE + "Зелье Звёздной Невидимости");
            pm.setLore(List.of(
                    ChatColor.GRAY + "• Длительность: " +
                            (pd.isExtended() ? "32:00" : "12:00") + " мин",
                    ChatColor.GRAY + "• Без пузырьков",
                    ChatColor.DARK_PURPLE + "Используй наковальню для создания"
            ));

            // 5) Добавляем фейковое зачарование для «переливающегося» вида
            pm.addEnchant(Enchantment.LUCK, 1, true);
            pm.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            result.setItemMeta(pm);

            // разблокировать слот (low repair cost)
            inv.setRepairCost(1);
            ev.setResult(result);
        } else {
            ev.setResult(null);
        }
    }

    // Шаг 2: ловим клик по "результату" и вручную списываем ингредиенты + выдаём предмет
    @EventHandler
    public void onAnvilClick(InventoryClickEvent ev) {
        if (!(ev.getInventory() instanceof AnvilInventory)) return;
        // rawSlot == 2 — это слот результата
        if (ev.getRawSlot() != 2) return;

        AnvilInventory inv = (AnvilInventory) ev.getInventory();
        ItemStack result = inv.getItem(2);
        if (result == null) return;

        // наш же результат
        if (!(result.getItemMeta() instanceof PotionMeta)) return;
        PotionMeta pm = (PotionMeta) result.getItemMeta();
        if (!pm.getBasePotionData().getType().equals(PotionType.INVISIBILITY)) return;
        if (!pm.hasItemFlag(ItemFlag.HIDE_POTION_EFFECTS)) return;

        // убираем результат из слота наковальни
        ev.setCurrentItem(null);
        ev.setCancelled(true);

        // списываем ингредиенты
        ItemStack s0 = inv.getItem(0);
        ItemStack s1 = inv.getItem(1);
        if (s0 != null) {
            s0.setAmount(s0.getAmount() - 1);
            inv.setItem(0, (s0.getAmount() > 0 ? s0 : null));
        }
        if (s1 != null) {
            s1.setAmount(s1.getAmount() - 1);
            inv.setItem(1, (s1.getAmount() > 0 ? s1 : null));
        }

        // отдаём игроку результат
        ev.getWhoClicked().getInventory().addItem(result);

        // сбрасываем cost, чтобы сразу можно было перекрафтить
        inv.setRepairCost(1);
    }

    // Шаг 3: игрок пьёт зелье
    @EventHandler
    public void onPlayerDrink(PlayerItemConsumeEvent ev) {
        ItemStack item = ev.getItem();
        if (item.getType() != Material.POTION
                || !(item.getItemMeta() instanceof PotionMeta)) return;

        PotionMeta pm = (PotionMeta) item.getItemMeta();
        PotionData pd = pm.getBasePotionData();
        if (pd.getType() != PotionType.INVISIBILITY
                || !pm.hasItemFlag(ItemFlag.HIDE_POTION_EFFECTS)) return;

        // Делаем на следующий тик, чтобы перехватить собранный эффект
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ev.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
        }, 1L);

        // Делаем на следующий тик, чтобы перехватить собранный эффект
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = ev.getPlayer();
            // Убираем стандартный
            p.removePotionEffect(PotionEffectType.INVISIBILITY);

            // Длину берём как extended=8min иначе 3min, умножаем на 4
            int base = pm.getBasePotionData().isExtended() ? 9600 : 3600;
            int ticks = base * 4;

            // Накладываем заново, теперь с showIcon=true
            PotionEffect custom = new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    ticks,
                    0,
                    false,  // ambient
                    false,  // particles
                    true    // showIcon
            );
            p.addPotionEffect(custom);
        }, 2L);
    }
}
