package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;


public class PrivateListener implements Listener {

    private final PALATA_RaidPlugin plugin;

    public PrivateListener(PALATA_RaidPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        // Пропускаем событие, если игрок не взаимодействует с блоком
        if (block == null) {
            return;
        }

        // Получаем команду игрока
        String playerTeam = plugin.getGame().getPlayerTeam(player.getName());

        // Проверяем, является ли блок в пределах дистанции от Нексуса, указанной в конфигурации
        if (plugin.getGame().isWithinNexusRadius(block.getLocation(), plugin.getGame().getDefendingTeam(playerTeam))) {
            if (plugin.getGame().isRaidActive() || plugin.getGame().isDelayBegunAfterRaid()) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете взаимодействовать с блоками рядом с Нексусом другой команды вне рейда!");
        }

        // Проверяем, является ли блок в пределах дистанции от дома, указанной в конфигурации
        if (plugin.getGame().isWithinHomeRadius(block.getLocation(), plugin.getGame().getDefendingTeam(playerTeam))) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете взаимодействовать с блоками рядом с домом другой команды!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Получаем команду игрока
        String playerTeam = plugin.getGame().getPlayerTeam(player.getName());

        // Проверяем, является ли блок в пределах дистанции от Нексуса, указанной в конфигурации
        if (plugin.getGame().isWithinNexusRadius(block.getLocation(), plugin.getGame().getDefendingTeam(playerTeam))) {
            if (plugin.getGame().isRaidActive() || plugin.getGame().isDelayBegunAfterRaid()) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете разрушать блоки рядом с Нексусом другой команды вне рейда или же около их дома!");
        }

        // Проверяем, является ли блок в пределах дистанции от дома, указанной в конфигурации
        if (plugin.getGame().isWithinHomeRadius(block.getLocation(), plugin.getGame().getDefendingTeam(playerTeam))) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете разрушать блоки рядом с домом другой команды!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Получаем команду игрока
        String playerTeam = plugin.getGame().getPlayerTeam(player.getName());

        // Проверяем, является ли блок в пределах дистанции от Нексуса, указанной в конфигурации
        if (plugin.getGame().isWithinNexusRadius(block.getLocation(), plugin.getGame().getDefendingTeam(playerTeam))) {
            if (plugin.getGame().isRaidActive() || plugin.getGame().isDelayBegunAfterRaid()) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете ставить блоки рядом с Нексусом другой команды вне рейда или же около их дома!");
        }

        // Проверяем, является ли блок в пределах дистанции от дома, указанной в конфигурации
        if (plugin.getGame().isWithinHomeRadius(block.getLocation(), plugin.getGame().getDefendingTeam(playerTeam))) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете ставить блоки рядом с домом другой команды!");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) && event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            String damagerTeam = plugin.getGame().getPlayerTeam(damager.getName());
            if (plugin.getGame().isWithinNexusRadius(event.getEntity().getLocation(), plugin.getGame().getDefendingTeam(damagerTeam)) && plugin.getGame().isRaidActive()) {
                //Рейд активен, можно нанести урон рядом с Нексусом
                return;
            } else
            // Здесь мы узнаем, находится ли сущность на территории базы враждебной команды
            if (!(plugin.getGame().isWithinNexusRadius(event.getEntity().getLocation(), plugin.getGame().getDefendingTeam(damagerTeam)) ||
                    plugin.getGame().isWithinHomeRadius(event.getEntity().getLocation(), plugin.getGame().getDefendingTeam(damagerTeam)))) {
                // Сущность НЕ на территории враждебной команды, можно убить её
                return;
            }

            // Если мы здесь, значит урон должен быть запрещён
            event.setCancelled(true);
            damager.sendMessage(ChatColor.RED + "Урон по сущностям не разрешён рядом с территорией вражеской команды.");
        }
        else
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            String damagedTeam = plugin.getGame().getPlayerTeam(damaged.getName());

            Player damager = (Player) event.getDamager();
            String damagerTeam = plugin.getGame().getPlayerTeam(damager.getName());

            if (damagedTeam.equals(damagerTeam)) {
                // Игроки в одной команде, PvP разрешен
                return;
            }

            // Здесь мы узнаем, находится ли игрок на территории базы враждебной команды
            if (plugin.getGame().isWithinNexusRadius(damaged.getLocation(), damagerTeam) ||
                    plugin.getGame().isWithinHomeRadius(damaged.getLocation(), damagerTeam)) {
                // Игрок на территории враждебной команды, PvP разрешен
                return;
            }

            // Если мы здесь, значит PvP должно быть запрещено
            event.setCancelled(true);
            damager.sendMessage(ChatColor.RED + "PvP разрешено только рядом с территорией вражеской команды или с членами своей команды.");
        }
    }

}
