package org.palata_tpsandping.palata_tpsandping;

import org.bukkit.plugin.java.JavaPlugin;

public final class PALATA_TPSAndPing extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("ping").setExecutor(new org.palata_tpsandping.palata_tpsandping.TpsPingCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
