package org.butterdevelop.slashSex;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

class SexStatsCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public SexStatsCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        StatsManager sm = StatsManager.get();
        sender.sendMessage(" ");
        sender.sendMessage(ChatColor.GREEN + "Используйте кастомный крафт для создания дилдо.");
        sender.sendMessage(ChatColor.GREEN + "Подберитесь поближе и нападайте.");
        sender.sendMessage(ChatColor.GOLD + "=== Top активов ===");
        for (Map.Entry<String,Integer> e : sm.topActive(5)) {
            OfflinePlayer p = Bukkit.getPlayer(UUID.fromString(e.getKey()));
            String name = (p != null ? p.getName() : e.getKey().substring(0,8));
            sender.sendMessage(ChatColor.GREEN + name + ": " + ChatColor.WHITE + e.getValue());
        }
        sender.sendMessage(ChatColor.GOLD + "=== Top пассивов ===");
        for (Map.Entry<String,Integer> e : sm.topPassive(5)) {
            OfflinePlayer p = Bukkit.getPlayer(UUID.fromString(e.getKey()));
            String name = (p != null ? p.getName() : e.getKey().substring(0,8));
            sender.sendMessage(ChatColor.RED + name + ": " + ChatColor.WHITE + e.getValue());
        }
        return true;
    }
}