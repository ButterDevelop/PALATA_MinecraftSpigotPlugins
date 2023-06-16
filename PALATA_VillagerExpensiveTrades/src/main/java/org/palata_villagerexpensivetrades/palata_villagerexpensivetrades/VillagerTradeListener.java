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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Merchant)) {
            return;
        }

        Merchant merchant = (Merchant) event.getRightClicked();
        List<MerchantRecipe> newRecipes = new ArrayList<>();

        for (MerchantRecipe recipe : merchant.getRecipes()) {
            if (recipe.getIngredients().get(0).getType() == Material.EMERALD) {
                List<ItemStack> ingredients = recipe.getIngredients();
                ingredients.set(0, new ItemStack(Material.EMERALD_BLOCK, ingredients.get(0).getAmount()));
                MerchantRecipe newRecipe = new MerchantRecipe(recipe.getResult(), recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(), recipe.getVillagerExperience(), recipe.getPriceMultiplier());
                newRecipe.setIngredients(ingredients);
                newRecipes.add(newRecipe);
            } else {
                newRecipes.add(recipe);
            }
        }

        merchant.setRecipes(newRecipes);
    }

}
