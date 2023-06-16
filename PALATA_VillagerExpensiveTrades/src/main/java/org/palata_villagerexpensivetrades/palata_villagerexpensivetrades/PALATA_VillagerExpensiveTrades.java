package org.palata_villagerexpensivetrades.palata_villagerexpensivetrades;

import org.bukkit.plugin.java.JavaPlugin;

public final class PALATA_VillagerExpensiveTrades extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new VillagerTradeListener(), this);
    }

}
