package thatsean.diplobank;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CurrencyCreator implements CommandExecutor, TabCompleter {

    private final File currencyFile;
    private final FileConfiguration currencyConfig;

    public CurrencyCreator(File currencyFile) {
        this.currencyFile = currencyFile;
        this.currencyConfig = YamlConfiguration.loadConfiguration(currencyFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID ownerUUID = player.getUniqueId();

        if (command.getName().equalsIgnoreCase("newcurrency")) {
            if (args.length < 3) {
                player.sendMessage("Usage: /newcurrency <currency_name> <currency_symbol> <currency_item>");
                return true;
            }

            String currencyName = args[0];
            String currencySymbol = args[1];
            String currencyItem = args[2];

            UUID currencyUUID = UUID.randomUUID();

            if (currencyConfig.contains(currencyUUID.toString())) {
                player.sendMessage("Currency already exists.");
                return true;
            }

            currencyConfig.set(currencyUUID + ".currency-name", currencyName);
            currencyConfig.set(currencyUUID + ".currency-item", currencyItem);
            currencyConfig.set(currencyUUID + ".currency-symbol", currencySymbol);
            currencyConfig.set(currencyUUID + ".owner", ownerUUID.toString());

            try {
                currencyConfig.save(currencyFile);
                player.sendMessage("Currency " + currencyName + " created successfully with UUID: " + currencyUUID);
            } catch (IOException e) {
                player.sendMessage("An error occurred while saving the currency.");
            }

            return true;
        }

        if (command.getName().equalsIgnoreCase("deletecurrency")) {
            if (args.length < 1) {
                player.sendMessage("Usage: /deletecurrency <currency_name>");
                return true;
            }

            String currencyName = args[0];
            UUID currencyUUIDToDelete = null;

            for (String key : currencyConfig.getKeys(false)) {
                if (currencyName.equals(currencyConfig.getString(key + ".currency-name"))) {
                    currencyUUIDToDelete = UUID.fromString(key);
                    break;
                }
            }

            if (currencyUUIDToDelete == null) {
                player.sendMessage("Currency does not exist.");
                return true;
            }

            String ownerUUIDString = currencyConfig.getString(currencyUUIDToDelete.toString() + ".owner");
            if (!ownerUUIDString.equals(ownerUUID.toString())) {
                player.sendMessage("You do not have permission to delete this currency.");
                return true;
            }

            currencyConfig.set(currencyUUIDToDelete.toString(), null);

            try {
                currencyConfig.save(currencyFile);
                player.sendMessage("Currency " + currencyName + " deleted successfully.");
            } catch (IOException e) {
                player.sendMessage("An error occurred while deleting the currency.");
            }

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("deletecurrency") && args.length == 1) {
            for (String key : currencyConfig.getKeys(false)) {
                suggestions.add(currencyConfig.getString(key + ".currency-name"));
            }
        }
        return suggestions;
    }
}
