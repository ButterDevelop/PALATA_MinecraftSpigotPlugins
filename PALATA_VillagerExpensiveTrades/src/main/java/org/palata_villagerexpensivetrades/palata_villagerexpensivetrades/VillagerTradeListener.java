package org.palata_villagerexpensivetrades.palata_villagerexpensivetrades;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public class VillagerTradeListener implements Listener {

    /**
     * Модифицирует рецепты конкретного торговца.
     */
    private void modifyMerchantRecipes(Merchant merchant) {
        List<MerchantRecipe> newRecipes = new ArrayList<>();

        for (MerchantRecipe recipe : merchant.getRecipes()) {
            boolean hasEmerald = false;
            boolean alreadyModified = false;
            List<ItemStack> ingredients = new ArrayList<>(recipe.getIngredients());

            // Проверяем, содержит ли рецепт уже EMERALD_BLOCK
            for (ItemStack ingredient : ingredients) {
                if (ingredient.getType() == Material.EMERALD_BLOCK) {
                    alreadyModified = true;
                    break;
                }
            }

            if (alreadyModified) {
                // Добавляем рецепт без изменений, если он уже был модифицирован
                newRecipes.add(recipe);
                continue;
            }

            // Проходимся по ингредиентам рецепта и заменяем EMERALD на EMERALD_BLOCK
            for (int i = 0; i < ingredients.size(); i++) {
                if (ingredients.get(i).getType() == Material.EMERALD) {
                    // Заменяем EMERALD на EMERALD_BLOCK с той же количеством
                    ingredients.set(i, new ItemStack(Material.EMERALD_BLOCK, ingredients.get(i).getAmount()));
                    hasEmerald = true;
                }
            }

            if (hasEmerald) {
                // Создаём новый рецепт с изменёнными ингредиентами
                MerchantRecipe newRecipe = new MerchantRecipe(
                        recipe.getResult(),
                        recipe.getUses(),
                        recipe.getMaxUses(),
                        recipe.hasExperienceReward(),
                        recipe.getVillagerExperience(),
                        recipe.getPriceMultiplier()
                );
                newRecipe.setIngredients(ingredients);
                newRecipes.add(newRecipe);
            } else {
                // Добавляем оригинальный рецепт без изменений
                newRecipes.add(recipe);
            }
        }

        // Устанавливаем новые рецепты для торговца
        merchant.setRecipes(newRecipes);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        // Проверяем, что взаимодействие происходит с торговцем
        if (!(event.getRightClicked() instanceof Merchant)) {
            return;
        }

        Merchant merchant = (Merchant) event.getRightClicked();
        modifyMerchantRecipes(merchant);
    }
}

