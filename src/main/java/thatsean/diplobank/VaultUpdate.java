package thatsean.diplobank;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VaultUpdate implements Economy {
    @Override
    public String getName() {
        return "DiplomacyBank";
    }

    private final JavaPlugin plugin;
    private final Map<UUID, Currency> currencies = new HashMap<>();
    private static VaultUpdate instance;

    public VaultUpdate(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        loadCurrencies();
    }

    public static VaultUpdate getInstance() {
        return instance;
    }

    private void loadCurrencies() {
        File currencyFile = new File(plugin.getDataFolder(), "currency.yml");
        if (!currencyFile.exists()) {
            plugin.getLogger().warning("currency.yml not found. No currencies loaded.");
            return;
        }

        FileConfiguration currencyConfig = YamlConfiguration.loadConfiguration(currencyFile);
        Set<String> currencyKeys = currencyConfig.getKeys(false);

        for (String currencyUUID : currencyKeys) {
            String name = currencyConfig.getString(currencyUUID + ".currency-name", "");
            String symbol = currencyConfig.getString(currencyUUID + ".currency-symbol", "");
            String item = currencyConfig.getString(currencyUUID + ".currency-item", "");
            String owner = currencyConfig.getString(currencyUUID + ".owner", "");

            if (!name.isEmpty() && !symbol.isEmpty() && !item.isEmpty() && !owner.isEmpty()) {
                currencies.put(UUID.fromString(currencyUUID), new Currency(name, symbol, item, owner));
                plugin.getLogger().info("Registered currency: " + name);
            } else {
                plugin.getLogger().warning("Invalid currency definition for UUID: " + currencyUUID);
            }
        }
    }

    public Map<UUID, Currency> getCurrencies() {
        return currencies;
    }

    public void updatePlayerCurrency(OfflinePlayer player, Material item) {
        int balance = CheckInvBal.checkInvBal(player.getPlayer(), item);
        plugin.getLogger().info("Updated currency for player " + player.getName() + " to " + balance);
    }

    // Implement all required methods from the Economy interface
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String currencyNameSingular() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        return config.getString("default-currency.singular", "Coin");
    }

    @Override
    public String currencyNamePlural() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        return config.getString("default-currency.plural", "Coins");
    }

    @Override
    public int fractionalDigits() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        return config.getInt("default-currency.fractional-digits", 2);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        double totalBalance = 0;

        for (Currency currency : currencies.values()) {
            Material currencyItem = Material.getMaterial(currency.getItem());
            if (currencyItem != null) {
                int balance = CheckInvBal.checkInvBal(player.getPlayer(), currencyItem);
                totalBalance += balance;
            }
        }

        return totalBalance;
    }

    @Override
    public double getBalance(String playerName) {
        return 0; // Replace with actual logic
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, double amount) {
        return false; // Replace with actual logic
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (!(player.isOnline() && player.getPlayer() != null)) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player is not online.");
        }

        Player onlinePlayer = player.getPlayer();
        Material defaultCurrency = Material.getMaterial(currencyNameSingular().toUpperCase());
        if (defaultCurrency == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Invalid currency.");
        }

        // Check and update balance before validation
        CheckInvBal.checkInvBal(onlinePlayer, defaultCurrency);

        int intAmount = (int) amount;
        boolean success = withdraw.removeCurrencyFromInventory(onlinePlayer, defaultCurrency, intAmount);
        if (success) {
            double newBalance = getBalance(player) - amount;
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "Withdrawal successful.");
        } else {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Not enough currency.");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Withdrawals are not supported.");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (!(player.isOnline() && player.getPlayer() != null)) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player is not online.");
        }

        Player onlinePlayer = player.getPlayer();
        Material defaultCurrency = Material.getMaterial(currencyNameSingular().toUpperCase());
        if (defaultCurrency == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Invalid currency.");
        }

        // Check and update balance before validation
        CheckInvBal.checkInvBal(onlinePlayer, defaultCurrency);

        int intAmount = (int) amount;
        boolean success = deposit.addCurrencyToInventory(onlinePlayer, defaultCurrency, intAmount);
        if (success) {
            double newBalance = getBalance(player) + amount;
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "Deposit successful.");
        } else {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Inventory full.");
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Deposits are not supported.");
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public List<String> getBanks() {
        throw new UnsupportedOperationException("Bank functionality is disabled.");
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (currencies.containsKey(player.getUniqueId())) {
            return false; // Account already exists
        }

        currencies.put(player.getUniqueId(), new Currency());
        plugin.getLogger().info("Created account for player: " + player.getName());
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return createPlayerAccount(player);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return currencies.containsKey(player.getUniqueId());
    }

    @Override
    public boolean hasAccount(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return hasAccount(player);
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return hasAccount(player);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return currencies.containsKey(player.getUniqueId());
    }

    public String getCurrencyName(UUID currencyUUID) {
        Currency currency = currencies.get(currencyUUID);
        return currency != null ? currency.getName() : "Unknown";
    }

    public String getCurrencySymbol(UUID currencyUUID) {
        Currency currency = currencies.get(currencyUUID);
        return currency != null ? currency.getSymbol() : "?";
    }

    public String getCurrencyOwner(UUID currencyUUID) {
        Currency currency = currencies.get(currencyUUID);
        return currency != null ? currency.getOwner() : "Unknown";
    }

    public void registerCurrenciesToVault() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        String defaultCurrencyName = config.getString("default-currency.singular", "Coin");
        plugin.getLogger().info("Default currency registered: " + defaultCurrencyName);

        for (Currency currency : currencies.values()) {
            plugin.getLogger().info("Custom currency registered: " + currency.getName());
        }
    }

    public static class Currency {
        private final String name;
        private final String symbol;
        private final String item;
        private final String owner;
        private final String materialName;

        public Currency(String name, String symbol, String item, String owner) {
            this.name = name;
            this.symbol = symbol;
            this.item = item;
            this.owner = owner;
            this.materialName = item; // Assuming item string is the material name
        }

        public Currency() {
            this.name = "Coin";
            this.symbol = "$";
            this.item = "GOLD_INGOT";
            this.owner = "Server";
            this.materialName = "GOLD_INGOT";
        }

        public String getName() {
            return name;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getItem() {
            return item;
        }

        public String getOwner() {
            return owner;
        }

        public String getMaterialName() {
            return materialName;
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        throw new UnsupportedOperationException("World-specific functionality is disabled.");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        throw new UnsupportedOperationException("World-specific functionality is disabled.");
    }


    @Override
    public String format(double amount) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        String currencySymbol = config.getString("default-currency.symbol", "$");
        return currencySymbol + String.format("%.2f", amount);
    }

    @Override
    public double getBalance(String playerName, String world) {
        throw new UnsupportedOperationException("World-specific functionality is disabled.");
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        throw new UnsupportedOperationException("World-specific functionality is disabled.");
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        throw new UnsupportedOperationException("World-specific functionality is disabled.");
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        throw new UnsupportedOperationException("World-specific functionality is disabled.");
    }

    @Override
    public boolean hasBankSupport() {
        return false; // Bank functionality is not supported in this implementation
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        throw new UnsupportedOperationException("World-specific functionality is disabled.");
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        throw new UnsupportedOperationException("World-specific functionality is disabled.");
    }

    // Removed unused withdrawCurrency method to fix the unused method warning.
}
