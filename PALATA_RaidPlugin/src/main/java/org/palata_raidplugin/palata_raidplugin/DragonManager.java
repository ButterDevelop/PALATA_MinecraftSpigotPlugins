package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
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
    private final PALATA_RaidPlugin plugin;
    private final long respawnInterval;
    private Location crystalLocation1 = null;
    private Location crystalLocation2 = null;
    private Location crystalLocation3 = null;
    private Location crystalLocation4 = null;
    private Location endPortalLocation = null;
    private HashSet<UUID> killedDragons = new HashSet<>();

    public DragonManager(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;
        long respawnIntervalInHours = plugin.getConfig().getLong("dragon.respawnIntervalHours", 6);
        this.respawnInterval = respawnIntervalInHours * 60 * 60 * 1000; // Convert hours to milliseconds

        World end = Bukkit.getWorld("world_the_end");
        setupPortalThings(end);
    }

    public void setupPortalThings(World end) {
        if (end != null && end.getEnderDragonBattle() != null && end.getEnderDragonBattle().getEndPortalLocation() != null && endPortalLocation == null) {
            endPortalLocation = end.getEnderDragonBattle().getEndPortalLocation(); //3 62 0, 0 62 -3, -3 62 0, 0 62 3 - 0 61 0
            crystalLocation1 = new Location(end, endPortalLocation.getBlockX() + 3, endPortalLocation.getBlockY(), endPortalLocation.getBlockZ());
            crystalLocation2 = new Location(end, endPortalLocation.getBlockX() - 3, endPortalLocation.getBlockY(), endPortalLocation.getBlockZ());
            crystalLocation3 = new Location(end, endPortalLocation.getBlockX(), endPortalLocation.getBlockY(), endPortalLocation.getBlockZ() + 3);
            crystalLocation4 = new Location(end, endPortalLocation.getBlockX(), endPortalLocation.getBlockY(), endPortalLocation.getBlockZ() - 3);

            // Schedule dragon respawn if needed
            long deathTime = plugin.getConfig().getLong("dragon.deathTime", 0);
            long respawnTime = deathTime + respawnInterval;
            long delay = respawnTime - System.currentTimeMillis();
            if (!plugin.getGame().isDragonAlive()) {
                if (delay > 0) {
                    scheduleDragonRespawn(delay / 50); // Convert milliseconds to ticks
                } else {
                    scheduleDragonRespawn(20); // 20 ticks = 1 seconds
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
            // Игрок вошёл в Энд
            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.runTaskLater(plugin, () -> {
                setupPortalThings(player.getWorld());
            }, 20); // Конвертируйте минуты в тики (20 тиков = 1 секунда)
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        World end = event.getEntity().getLocation().getWorld();

        if (!end.getEnvironment().equals(World.Environment.THE_END)) {
            return;
        }

        if (!event.getEntityType().equals(EntityType.ENDER_CRYSTAL)) {
            return;
        }

        if (event.getEntity().getCustomName() != null && !event.getEntity().getCustomName().equals("CRYSTAL FROM MY PLUGIN TO YOUR HEART")) {
            return;
        }

        if (plugin.getGame().isWithinRadius(event.getEntity().getLocation(), endPortalLocation, 4)) {
            if (!plugin.getGame().isDragonAlive()) {
                Bukkit.broadcastMessage(ChatColor.RED + "Возрождение Дракона было отменено! Новый дракон появится через " + (respawnInterval / (60 * 60 * 1000)) + " часов.");
                scheduleDragonRespawn(respawnInterval / 50);
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        World world = player.getWorld();
        if (world.getEnvironment() == World.Environment.THE_END) {
            Block block = event.getClickedBlock();
            if (action.equals(Action.RIGHT_CLICK_BLOCK) && item != null && item.getType() == Material.END_CRYSTAL &&
                    block != null && block.getType() == Material.BEDROCK) {
                if (plugin.getGame().isDragonAlive()) {
                    return;
                }
                if (plugin.getGame().isWithinRadius(block.getLocation(), endPortalLocation, 4)) { // Если попытка поставить кристалл на запрещённое место
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Вы не можете возрождать Дракона Края.");
                }
            }
        }
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            EnderDragon dragon = (EnderDragon) event.getEntity();
            UUID dragonId = dragon.getUniqueId();

            // Если этот дракон уже был убит, то просто вернемся.
            if (killedDragons.contains(dragonId)) {
                return;
            }

            // Добавляем дракона в список убитых
            killedDragons.add(dragonId);

            if (event.getEntity() instanceof EnderDragon) {
                event.setDroppedExp(12000); // Set dropped experience equivalent to 68 levels

                Bukkit.broadcastMessage(ChatColor.RED + "Дракон Края был убит. Новый дракон появится через " + (respawnInterval / (60 * 60 * 1000)) + " часов.");

                scheduleDragonRespawn(respawnInterval / 50); // Convert milliseconds to ticks
            }
        }
    }

    private void scheduleDragonRespawn(long delayInTicks) {
        plugin.getConfig().set("dragon.deathTime", System.currentTimeMillis());
        plugin.saveConfig();

        new BukkitRunnable() {
            @Override
            public void run() {
                World end = Bukkit.getWorld("world_the_end"); // The End world
                if (end != null) {
                    EnderCrystal crystal1 = end.spawn(new Location(end, crystalLocation1.getX() + 0.5, crystalLocation1.getY() + 1, crystalLocation1.getZ() + 0.5), EnderCrystal.class);
                    EnderCrystal crystal2 = end.spawn(new Location(end, crystalLocation2.getX() + 0.5, crystalLocation2.getY() + 1, crystalLocation2.getZ() + 0.5), EnderCrystal.class);
                    EnderCrystal crystal3 = end.spawn(new Location(end, crystalLocation3.getX() + 0.5, crystalLocation3.getY() + 1, crystalLocation3.getZ() + 0.5), EnderCrystal.class);
                    EnderCrystal crystal4 = end.spawn(new Location(end, crystalLocation4.getX() + 0.5, crystalLocation4.getY() + 1, crystalLocation4.getZ() + 0.5), EnderCrystal.class);
                    crystal1.setCustomName("CRYSTAL FROM MY PLUGIN TO YOUR HEART");
                    crystal2.setCustomName("CRYSTAL FROM MY PLUGIN TO YOUR HEART");
                    crystal3.setCustomName("CRYSTAL FROM MY PLUGIN TO YOUR HEART");
                    crystal4.setCustomName("CRYSTAL FROM MY PLUGIN TO YOUR HEART");
                    crystal1.setCustomNameVisible(false);
                    crystal2.setCustomNameVisible(false);
                    crystal3.setCustomNameVisible(false);
                    crystal4.setCustomNameVisible(false);
                    end.getEnderDragonBattle().setRespawnPhase(DragonBattle.RespawnPhase.START);
                    end.getEnderDragonBattle().initiateRespawn();
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Дракон Края возрождается!");

                    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F);
                    }
                }
            }
        }.runTaskLater(plugin, delayInTicks);
    }

}
