package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashSet;
import java.util.UUID;

public class DragonManager implements Listener {
    private static final String CRYSTAL_NAME = "CRYSTAL FROM MY PLUGIN TO YOUR HEART";

    private final PALATA_RaidPlugin plugin;
    private final long respawnInterval;
    private Location crystalLocation1;
    private Location crystalLocation2;
    private Location crystalLocation3;
    private Location crystalLocation4;
    private Location endPortalLocation;
    private final HashSet<UUID> killedDragons = new HashSet<>();

    public DragonManager(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;
        long respawnIntervalInHours = plugin.getConfig().getLong("dragon.respawnIntervalHours", 6);
        this.respawnInterval = respawnIntervalInHours * 60 * 60 * 1000L; // Перевод часов в миллисекунды

        World end = Bukkit.getWorld("world_the_end");
        setupPortalThings(end);
    }

    public void setupPortalThings(World end) {
        if (end == null) return;

        DragonBattle battle = end.getEnderDragonBattle();
        if (battle == null) return;

        if (battle.getEndPortalLocation() != null && endPortalLocation == null) {
            endPortalLocation = battle.getEndPortalLocation();

            crystalLocation1 = endPortalLocation.clone().add(3, 0, 0);
            crystalLocation2 = endPortalLocation.clone().add(-3, 0, 0);
            crystalLocation3 = endPortalLocation.clone().add(0, 0, 3);
            crystalLocation4 = endPortalLocation.clone().add(0, 0, -3);

            long deathTime = plugin.getConfig().getLong("dragon.deathTime", 0);
            long respawnTime = deathTime + respawnInterval;
            long delay = respawnTime - System.currentTimeMillis();

            if (!plugin.getGame().isDragonAlive()) {
                if (delay > 0) {
                    scheduleDragonRespawn(delay / 50L); // миллисекунды в тиках
                } else {
                    scheduleDragonRespawn(20L); // 20 тиков = 1 секунда
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.runTaskLater(plugin, () -> setupPortalThings(player.getWorld()), 20L);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!event.getEntityType().equals(EntityType.ENDER_CRYSTAL)) return;

        World world = event.getEntity().getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) return;

        EnderCrystal crystal = (EnderCrystal) event.getEntity();
        if (crystal.getCustomName() == null || !crystal.getCustomName().equals(CRYSTAL_NAME)) return;

        if (endPortalLocation != null && plugin.getGame().isWithinRadius(crystal.getLocation(), endPortalLocation, 4)) {
            if (!plugin.getGame().isDragonAlive()) {
                Bukkit.broadcastMessage(ChatColor.RED + "Возрождение Дракона было отменено! Новый дракон появится через "
                        + (respawnInterval / (60 * 60 * 1000)) + " часов.");
                scheduleDragonRespawn(respawnInterval / 50L);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) return;

        Action action = event.getAction();
        ItemStack item = event.getItem();
        Block block = event.getClickedBlock();

        if (action == Action.RIGHT_CLICK_BLOCK && item != null && item.getType() == Material.END_CRYSTAL &&
                block != null && block.getType() == Material.BEDROCK) {
            if (plugin.getGame().isDragonAlive()) return;

            if (endPortalLocation != null && plugin.getGame().isWithinRadius(block.getLocation(), endPortalLocation, 4)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Вы не можете возрождать Дракона Края.");
            }
        }
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ENDER_DRAGON) return;

        EnderDragon dragon = (EnderDragon) event.getEntity();
        UUID dragonId = dragon.getUniqueId();

        if (!killedDragons.add(dragonId)) return;  // Если дракон уже в списке, выходим

        event.setDroppedExp(12000);
        Bukkit.broadcastMessage(ChatColor.RED + "Дракон Края был убит. Новый дракон появится через "
                + (respawnInterval / (60 * 60 * 1000)) + " часов.");
        scheduleDragonRespawn(respawnInterval / 50L);
    }

    private void scheduleDragonRespawn(long delayInTicks) {
        plugin.getConfig().set("dragon.deathTime", System.currentTimeMillis());
        plugin.saveConfig();

        new BukkitRunnable() {
            @Override
            public void run() {
                World end = Bukkit.getWorld("world_the_end");
                if (end == null || endPortalLocation == null) return;

                // Создание и настройка кристаллов
                spawnCrystal(end, crystalLocation1);
                spawnCrystal(end, crystalLocation2);
                spawnCrystal(end, crystalLocation3);
                spawnCrystal(end, crystalLocation4);

                DragonBattle battle = end.getEnderDragonBattle();
                if (battle != null) {
                    battle.setRespawnPhase(DragonBattle.RespawnPhase.START);
                    battle.initiateRespawn();
                }

                Bukkit.broadcastMessage(ChatColor.GREEN + "Дракон Края возрождается!");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F);
                }
            }
        }.runTaskLater(plugin, delayInTicks);
    }

    private void spawnCrystal(World world, Location baseLocation) {
        if (baseLocation == null) return;
        Location spawnLocation = baseLocation.clone().add(0.5, 1, 0.5);
        EnderCrystal crystal = world.spawn(spawnLocation, EnderCrystal.class);
        crystal.setCustomName(CRYSTAL_NAME);
        crystal.setCustomNameVisible(false);
    }
}
