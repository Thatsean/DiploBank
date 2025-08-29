package thatsean.diplobank;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AdminCommands implements CommandExecutor, TabCompleter {

    public AdminCommands() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bankadmin.use")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("bankadmin")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /bankadmin deposit <player> <currency> <amount>");
                return true;
            }

            String subCommand = args[0];
            if (subCommand.equalsIgnoreCase("deposit")) {
                String playerName = args[1];
                String currencyName = args[2];
                int amount;

                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid amount. Please enter a valid number.");
                    return true;
                }

                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                if (player == null || !player.hasPlayedBefore()) {
                    sender.sendMessage("Player not found.");
                    return true;
                }

                Material currencyItem = Material.getMaterial(currencyName.toUpperCase());
                if (currencyItem == null) {
                    sender.sendMessage("Invalid currency. Please enter a valid currency item.");
                    return true;
                }

                if (player.isOnline() && player.getPlayer() != null) {
                    Player onlinePlayer = player.getPlayer();
                    boolean success = deposit.addCurrencyToInventory(onlinePlayer, currencyItem, amount);
                    if (success) {
                        sender.sendMessage("Successfully deposited " + amount + " of " + currencyName + " to player " + player.getName());
                    } else {
                        sender.sendMessage("Failed to deposit. Player's inventory might be full.");
                    }
                } else {
                    sender.sendMessage("Player is not online.");
                }
                return true;
            } else if (subCommand.equalsIgnoreCase("withdraw")) {
                if (args.length < 4) {
                    sender.sendMessage("Usage: /bankadmin withdraw <player> <currency> <amount>");
                    return true;
                }

                String playerName = args[1];
                String currencyName = args[2];
                int amount;

                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid amount. Please enter a valid number.");
                    return true;
                }

                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                if (player == null || !player.hasPlayedBefore()) {
                    sender.sendMessage("Player not found.");
                    return true;
                }

                Material currencyItem = Material.getMaterial(currencyName.toUpperCase());
                if (currencyItem == null) {
                    sender.sendMessage("Invalid currency. Please enter a valid currency item.");
                    return true;
                }

                if (player.isOnline() && player.getPlayer() != null) {
                    Player onlinePlayer = player.getPlayer();
                    boolean success = withdraw.removeCurrencyFromInventory(onlinePlayer, currencyItem, amount);
                    if (success) {
                        sender.sendMessage("Successfully withdrew " + amount + " of " + currencyName + " from player " + player.getName());
                    } else {
                        sender.sendMessage("Failed to withdraw. Player might not have enough currency.");
                    }
                } else {
                    sender.sendMessage("Player is not online.");
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("bankadmin")) {
            if (args.length == 1) {
                suggestions.add("deposit");
                suggestions.add("withdraw");
            } else if (args.length == 2) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
            } else if (args.length == 3) {
                for (Material material : Material.values()) {
                    suggestions.add(material.name());
                }
            }
        }
        return suggestions;
    }
}
