package org.palata_raidplugin.palata_raidplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RaidCommand implements CommandExecutor {
    private final Game game;
    public RaidCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("raid")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("on")) {
                    if (!sender.isOp()) {
                        sender.sendMessage(ChatColor.RED + "Только администратор может это сделать.");
                        return true;
                    }
                    game.setIsEnabled(true);
                    sender.sendMessage(ChatColor.GREEN + "RaidPlugin включен.");
                    return true;
                } else if (args[0].equalsIgnoreCase("off")) {
                    if (!sender.isOp()) {
                        sender.sendMessage(ChatColor.RED + "Только администратор может это сделать.");
                        return true;
                    }
                    game.setIsEnabled(false);
                    sender.sendMessage(ChatColor.RED + "RaidPlugin выключен.");
                    return true;
                } else if (args[0].equalsIgnoreCase("info")) {
                    if (game.getIsEnabled()) {
                        sender.sendMessage(ChatColor.GREEN + "RaidPlugin в данный момент включен.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "RaidPlugin в данный момент выключен.");
                    }
                    return true;
                }
            }
        }

        return false;
    }
}
