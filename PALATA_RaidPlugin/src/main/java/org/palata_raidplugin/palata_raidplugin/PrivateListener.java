package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PrivateListener implements Listener {

    private final PALATA_RaidPlugin plugin;

    public PrivateListener(final PALATA_RaidPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();
        if (block == null) return;

        final String playerTeam = plugin.getGame().getPlayerTeam(player.getName());
        if (playerTeam == null) return;

        final Location blockLoc = block.getLocation();
        final String defendingTeam = plugin.getGame().getDefendingTeam(playerTeam);

        if (plugin.getGame().isWithinNexusRadius(blockLoc, defendingTeam)) {
            if (!(plugin.getGame().isRaidActive() || plugin.getGame().isDelayBegunAfterRaid())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Вы не можете взаимодействовать с блоками рядом с Нексусом другой команды вне рейда!");
            }
        }

        if (plugin.getGame().isWithinHomeRadius(blockLoc, defendingTeam)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете взаимодействовать с блоками рядом с домом другой команды!");
        }
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final String playerTeam = plugin.getGame().getPlayerTeam(player.getName());
        if (playerTeam == null) return;

        final Location blockLoc = block.getLocation();
        final String defendingTeam = plugin.getGame().getDefendingTeam(playerTeam);

        if (plugin.getGame().isWithinNexusRadius(blockLoc, defendingTeam)) {
            if (!(plugin.getGame().isRaidActive() || plugin.getGame().isDelayBegunAfterRaid())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Вы не можете разрушать блоки рядом с Нексусом другой команды вне рейда или же около их дома!");
            }
        }

        if (plugin.getGame().isWithinHomeRadius(blockLoc, defendingTeam)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете разрушать блоки рядом с домом другой команды!");
        }
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final String playerTeam = plugin.getGame().getPlayerTeam(player.getName());
        if (playerTeam == null) return;

        final Location blockLoc = block.getLocation();
        final String defendingTeam = plugin.getGame().getDefendingTeam(playerTeam);

        if (plugin.getGame().isWithinNexusRadius(blockLoc, defendingTeam)) {
            if (!(plugin.getGame().isRaidActive() || plugin.getGame().isDelayBegunAfterRaid())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Вы не можете ставить блоки рядом с Нексусом другой команды вне рейда или же около их дома!");
            }
        }

        if (plugin.getGame().isWithinHomeRadius(blockLoc, defendingTeam)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете ставить блоки рядом с домом другой команды!");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        final String arenaWorldName = plugin.getConfig().getString("arena.world", "world_arena");
        if (event.getEntity().getWorld().getName().equals(arenaWorldName)) {
            return;
        }

        if (!(event.getEntity() instanceof Player) && event.getDamager() instanceof Player) {
            final Player damager = (Player) event.getDamager();
            final String damagerTeam = plugin.getGame().getPlayerTeam(damager.getName());
            if (damagerTeam == null) {
                if (plugin.getGame().isWithinNexusRadius(event.getEntity().getLocation(), "RED")
                        || plugin.getGame().isWithinHomeRadius(event.getEntity().getLocation(), "RED")
                        || plugin.getGame().isWithinNexusRadius(event.getEntity().getLocation(), "BLUE")
                        || plugin.getGame().isWithinHomeRadius(event.getEntity().getLocation(), "BLUE")) {
                    event.setCancelled(true);
                    damager.sendMessage(ChatColor.RED + "Аборигены не могут влиять на другие команды!");
                }
                return;
            }
            if (plugin.getGame().isWithinNexusRadius(event.getEntity().getLocation(), plugin.getGame().getDefendingTeam(damagerTeam))
                    && plugin.getGame().isRaidActive()) {
                return;
            }
            if (!plugin.getGame().isWithinNexusRadius(event.getEntity().getLocation(), plugin.getGame().getDefendingTeam(damagerTeam))
                    && !plugin.getGame().isWithinHomeRadius(event.getEntity().getLocation(), plugin.getGame().getDefendingTeam(damagerTeam))) {
                return;
            }
            event.setCancelled(true);
            damager.sendMessage(ChatColor.RED + "Урон по сущностям не разрешён рядом с территорией вражеской команды.");
        } else if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            final Player damaged = (Player) event.getEntity();
            final String damagedTeam = plugin.getGame().getPlayerTeam(damaged.getName());

            final Player damager = (Player) event.getDamager();
            final String damagerTeam = plugin.getGame().getPlayerTeam(damager.getName());

            if ((damagedTeam == null && damagerTeam == null) || (damagedTeam != null && damagedTeam.equals(damagerTeam))) {
                return;
            }

            if (damagedTeam == null) {
                damager.sendMessage(ChatColor.RED + "Вы не можете наносить урон аборигенам.");
                event.setCancelled(true);
                return;
            }

            if (damagerTeam == null) {
                damager.sendMessage(ChatColor.RED + "Вы абориген. Вы не можете наносить урон другим командам.");
                event.setCancelled(true);
                return;
            }

            boolean homeRadiusFlag = plugin.getGame().isWithinHomeRadius(damaged.getLocation(), damagerTeam);

            if (plugin.getGame().isWithinNexusRadius(damaged.getLocation(), damagerTeam) || homeRadiusFlag) {
                return;
            }

            if (plugin.getGame().isWithinNexusRadius(damaged.getLocation(), damagedTeam)
                    || plugin.getGame().isWithinHomeRadius(damaged.getLocation(), damagedTeam)) {
                event.setCancelled(true);
                damager.sendMessage(ChatColor.RED + "PvP разрешено только рядом с территорией вражеской команды или с членами своей команды.");
                return;
            }

            if (plugin.getGame().isThePvPIsAlwaysOnInThisWorld(damaged.getLocation().getWorld().getName())) {
                return;
            }

            final String damagedWorld = damaged.getLocation().getWorld().getName();
            if (damagedWorld.equals("world_the_end")
                    && !plugin.getGame().isThePvPIsAlwaysOnInThisWorld(damagedWorld)
                    && plugin.getGame().isDragonAlive()
                    && plugin.getGame().isWithin2DRadius(damaged.getLocation(), new Location(damaged.getWorld(), 0, 0, 0), plugin.getGame().getEndWorldMainIslandRadius())) {
                return;
            }

            event.setCancelled(true);
            damager.sendMessage(ChatColor.RED + "PvP разрешено только рядом с территорией вражеской команды или с членами своей команды.");
        }
    }

    @EventHandler
    public void onEntityExplode(final EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Location loc = block.getLocation();
            boolean inRedNexus  = plugin.getGame().isWithinNexusRadius(loc, "RED");
            boolean inBlueNexus = plugin.getGame().isWithinNexusRadius(loc, "BLUE");
            boolean inRedHome   = plugin.getGame().isWithinHomeRadius(loc, "RED");
            boolean inBlueHome  = plugin.getGame().isWithinHomeRadius(loc, "BLUE");

            // Получаем команду, которая вызвала взрыв (псевдокод)
            String explosionSourceTeam  = getExplosionSourceTeam(event.getEntity());
            boolean blueCausesExplosion = "BLUE".equals(explosionSourceTeam);
            boolean redCausesExplosion  = "RED".equals(explosionSourceTeam);

            // Аборигены не могут влиять на процесс
            if (explosionSourceTeam.equals("neutral") && (inRedNexus || inBlueNexus || inRedHome || inBlueHome)) {
                return true;
            }

            // Проверяем, находимся ли мы где-то дома
            if ((inRedHome && blueCausesExplosion) || (inBlueHome && redCausesExplosion)) {
                return true;
            }

            // Проверяем, находимся ли мы где-то в рейде
            if (((inRedNexus && blueCausesExplosion) || (inBlueNexus && redCausesExplosion))
                    && !plugin.getGame().isRaidActive()) {
                return true;
            }

            // Блок должен взорваться
            return false;
        });
    }

    private String getExplosionSourceTeam(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            String team = plugin.getGame().getPlayerTeam(player.getName());

            if (team == null) {
                return "neutral";
            } else {
                return team;
            }
        }

        // Это вообще не игрок, может, крипер, поэтому нужно разделить обычное null и "neutral"
        return null;
    }
}
