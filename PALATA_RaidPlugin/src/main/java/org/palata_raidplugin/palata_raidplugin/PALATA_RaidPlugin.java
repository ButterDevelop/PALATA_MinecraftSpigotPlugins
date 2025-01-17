package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class PALATA_RaidPlugin extends JavaPlugin {

    private Game game;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        game = new Game(this);
        this.getCommand("setraidbase").setExecutor(new SetRaidBaseCommand(this));
        this.getCommand("sethomebase").setExecutor(new SetHomeBaseCommand(this));
        this.getCommand("privatecheck").setExecutor(new PrivateCheck(this));
        this.getCommand("openraid").setExecutor(new OpenRaidCommand(this));
        this.getCommand("cancelraid").setExecutor(new CancelRaidCommand(game));
        this.getCommand("joinraid").setExecutor(new JoinRaidCommand(game));
        this.getCommand("startraid").setExecutor(new StartRaidCommand(game));
        this.getCommand("raid").setExecutor(new RaidCommand(game));
        this.getCommand("getenemyraidbasecoords").setExecutor(new GetEnemyRaidBaseCoordsCommand(game));

        getServer().getPluginManager().registerEvents(new BlockBreakListener(game), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PrivateListener(this), this);
        getServer().getPluginManager().registerEvents(new DragonManager(this), this);
        getServer().getPluginManager().registerEvents(new SoundManager(this), this);

        ArenaManager arenaManager = new ArenaManager(this);
        getServer().getPluginManager().registerEvents(arenaManager, this);
        this.getCommand("joinarena").setExecutor(new JoinArenaCommand(this, arenaManager));
        this.getCommand("schedulearenainseconds").setExecutor(new ScheduleArenaInSecondsCommand(arenaManager));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public Game getGame() {
        return game;
    }
}
