package thatsean.diplobank;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final VaultUpdate vaultUpdate;

    public BalanceCommand(VaultUpdate vaultUpdate) {
        this.vaultUpdate = vaultUpdate;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Display all currencies
            StringBuilder balances = new StringBuilder("Your balances:\n");
            for (VaultUpdate.Currency currency : vaultUpdate.getCurrencies().values()) {
                Material material = Material.getMaterial(currency.getMaterialName()); // Assuming Currency has a method getMaterialName
                if (material != null) {
                    double balance = CheckInvBal.checkInvBal(player, material);
                    balances.append(material.name()).append(": ").append(balance).append("\n");
                }
            }
            player.sendMessage(balances.toString());
        } else {
            // Display specific currency
            String currencyName = args[0].toUpperCase();
            Material currency = Material.getMaterial(currencyName);
            if (currency == null) {
                player.sendMessage("Invalid currency: " + currencyName);
                return true;
            }

            double balance = CheckInvBal.checkInvBal(player, currency);
            player.sendMessage("Your balance of " + currencyName + " is: " + balance);
        }

        // Update balance in Vault
        vaultUpdate.updatePlayerCurrency(player, null);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            for (VaultUpdate.Currency currency : vaultUpdate.getCurrencies().values()) {
                suggestions.add(currency.getMaterialName());
            }
        }
        return suggestions;
    }
}
