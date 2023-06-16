package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class PALATA_RaidPlugin extends JavaPlugin {

    private Game game;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        game = new Game(this);
        this.getCommand("setraidbase").setExecutor(new SetRaidBaseCommand(this));
        this.getCommand("openraid").setExecutor(new OpenRaidCommand(game));
        this.getCommand("joinraid").setExecutor(new JoinRaidCommand(game));
        this.getCommand("startraid").setExecutor(new StartRaidCommand(game));
        getServer().getPluginManager().registerEvents(new BlockBreakListener(game), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public Game getGame() {
        return game;
    }
}
