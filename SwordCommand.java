package com.soulstealer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class SwordCommand implements CommandExecutor {

    private final SoulstealerPlugin plugin;

    public SwordCommand(SoulstealerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            player.getInventory().addItem(SwordManager.createSword(0));
            player.getPersistentDataContainer().set(plugin.craftedKey, PersistentDataType.BYTE, (byte) 1);
            player.sendMessage(ChatColor.GREEN + "You have been given the Soulstealer Sword!");
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /soulstealer give");
        }

        return true;
    }
}
