package thatsean.diplobank;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault2.economy.AccountPermission;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;

public class VaultUpdate implements Economy {
    
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
    
    public boolean isEnabled() {
        return true;
    }

    
    public String currencyNameSingular() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        return config.getString("default-currency.singular", "Coin");
    }

    
    public String currencyNamePlural() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        return config.getString("default-currency.plural", "Coins");
    }

    
    public int fractionalDigits() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        return config.getInt("default-currency.fractional-digits", 2);
    }

    
    public BigDecimal getBalance(OfflinePlayer player) {
        BigDecimal totalBalance = BigDecimal.ZERO;

        for (Currency currency : currencies.values()) {
            Material currencyItem = Material.getMaterial(currency.getItem());
            if (currencyItem != null) {
                int balance = CheckInvBal.checkInvBal(player.getPlayer(), currencyItem);
                totalBalance = totalBalance.add(BigDecimal.valueOf(balance));
            }
        }

        return totalBalance;
    }

    
    public BigDecimal getBalance(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return getBalance(player);
    }

    
    public boolean has(OfflinePlayer player, BigDecimal amount) {
        return getBalance(player).compareTo(amount) >= 0;
    }

    
    public boolean has(String playerName, BigDecimal amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return has(player, amount);
    }

    
    public EconomyResponse withdrawPlayer(OfflinePlayer player, BigDecimal amount) {
        if (!(player.isOnline() && player.getPlayer() != null)) {
            return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "Player is not online.");
        }

        Player onlinePlayer = player.getPlayer();
        Material defaultCurrency = Material.getMaterial(currencyNameSingular().toUpperCase());
        if (defaultCurrency == null) {
            return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "Invalid currency.");
        }

        // Check and update balance before validation
        CheckInvBal.checkInvBal(onlinePlayer, defaultCurrency);

        int intAmount = amount.intValue();
        boolean success = Withdraw.removeCurrencyFromInventory(onlinePlayer, defaultCurrency, intAmount);
        if (success) {
            BigDecimal newBalance = getBalance(player).subtract(amount);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "Withdrawal successful.");
        } else {
            return new EconomyResponse(BigDecimal.ZERO, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Not enough currency.");
        }
    }

    
    public EconomyResponse withdrawPlayer(String playerName, BigDecimal amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return withdrawPlayer(player, amount);
    }

    
    public EconomyResponse depositPlayer(OfflinePlayer player, BigDecimal amount) {
        if (!(player.isOnline() && player.getPlayer() != null)) {
            return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "Player is not online.");
        }

        Player onlinePlayer = player.getPlayer();
        Material defaultCurrency = Material.getMaterial(currencyNameSingular().toUpperCase());
        if (defaultCurrency == null) {
            return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "Invalid currency.");
        }

        // Check and update balance before validation
        CheckInvBal.checkInvBal(onlinePlayer, defaultCurrency);

        int intAmount = amount.intValue();
        boolean success = Deposit.addCurrencyToInventory(onlinePlayer, defaultCurrency, intAmount);
        if (success) {
            BigDecimal newBalance = getBalance(player).add(amount);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "Deposit successful.");
        } else {
            return new EconomyResponse(BigDecimal.ZERO, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Inventory full.");
        }
    }

    
    public EconomyResponse depositPlayer(String playerName, BigDecimal amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return depositPlayer(player, amount);
    }

    
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank functionality is disabled.");
    }

    
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank functionality is disabled.");
    }

    
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank functionality is disabled.");
    }

    
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank functionality is disabled.");
    }

    
    public EconomyResponse bankHas(String name, BigDecimal amount) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank functionality is disabled.");
    }

    
    public EconomyResponse bankWithdraw(String name, BigDecimal amount) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank functionality is disabled.");
    }

    
    public EconomyResponse bankDeposit(String name, BigDecimal amount) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank functionality is disabled.");
    }

    
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank functionality is disabled.");
    }

    
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank functionality is disabled.");
    }

    
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank functionality is disabled.");
    }

    
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank functionality is disabled.");
    }

    
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (currencies.containsKey(player.getUniqueId())) {
            return false; // Account already exists
        }

        currencies.put(player.getUniqueId(), new Currency());
        plugin.getLogger().info("Created account for player: " + player.getName());
        return true;
    }

    
    public boolean createPlayerAccount(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return createPlayerAccount(player);
    }

    
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    
    public boolean hasAccount(OfflinePlayer player) {
        return currencies.containsKey(player.getUniqueId());
    }

    
    public boolean hasAccount(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return hasAccount(player);
    }

    
    public boolean hasAccount(String playerName, String worldName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return hasAccount(player);
    }

    
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

    
    public boolean hasSharedAccountSupport() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean hasMultiCurrencySupport() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public int fractionalDigits(String pluginName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public String format(String pluginName, BigDecimal amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public String format(BigDecimal amount, String currency) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public String format(String pluginName, BigDecimal amount, String currency) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean hasCurrency(String currency) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public String getDefaultCurrency(String pluginName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public String defaultCurrencyNamePlural(String pluginName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public String defaultCurrencyNameSingular(String pluginName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public Collection<String> currencies() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean createAccount(UUID accountID, String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean createAccount(UUID accountID, String name, boolean player) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean createAccount(UUID accountID, String name, String worldName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean createAccount(UUID accountID, String name, String worldName, boolean player) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public Map<UUID, String> getUUIDNameMap() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public Optional<String> getAccountName(UUID accountID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean hasAccount(UUID accountID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean hasAccount(UUID accountID, String worldName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean renameAccount(UUID accountID, String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean renameAccount(String plugin, UUID accountID, String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean deleteAccount(String plugin, UUID accountID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean accountSupportsCurrency(String plugin, UUID accountID, String currency) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean accountSupportsCurrency(String plugin, UUID accountID, String currency, String world) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public BigDecimal getBalance(String pluginName, UUID accountID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public BigDecimal getBalance(String pluginName, UUID accountID, String world) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public BigDecimal getBalance(String pluginName, UUID accountID, String world, String currency) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean has(String pluginName, UUID accountID, BigDecimal amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean has(String pluginName, UUID accountID, String worldName, BigDecimal amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean has(String pluginName, UUID accountID, String worldName, String currency, BigDecimal amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public EconomyResponse withdraw(String pluginName, UUID accountID, BigDecimal amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public EconomyResponse withdraw(String pluginName, UUID accountID, String worldName, BigDecimal amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public EconomyResponse withdraw(String pluginName, UUID accountID, String worldName, String currency, BigDecimal amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public EconomyResponse deposit(String pluginName, UUID accountID, BigDecimal amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public EconomyResponse deposit(String pluginName, UUID accountID, String worldName, BigDecimal amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public EconomyResponse deposit(String pluginName, UUID accountID, String worldName, String currency, BigDecimal amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean createSharedAccount(String pluginName, UUID accountID, String name, UUID owner) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean isAccountOwner(String pluginName, UUID accountID, UUID uuid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean setOwner(String pluginName, UUID accountID, UUID uuid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean isAccountMember(String pluginName, UUID accountID, UUID uuid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean addAccountMember(String pluginName, UUID accountID, UUID uuid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean addAccountMember(String pluginName, UUID accountID, UUID uuid, AccountPermission... initialPermissions) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    public boolean removeAccountMember(String pluginName, UUID accountID, UUID uuid) {
        throw new UnsupportedOperationException("Not supported yet.");
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

    
    public EconomyResponse depositPlayer(String playerName, String worldName, BigDecimal amount) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "World-specific functionality is disabled.");
    }

    
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, BigDecimal amount) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "World-specific functionality is disabled.");
    }

    
    public String format(BigDecimal amount) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        String currencySymbol = config.getString("default-currency.symbol", "$");
        return currencySymbol + String.format("%.2f", amount);
    }

    
    public BigDecimal getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    
    public BigDecimal getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    
    public boolean has(String playerName, String worldName, BigDecimal amount) {
        return has(playerName, amount);
    }

    
    public boolean has(OfflinePlayer player, String worldName, BigDecimal amount) {
        return has(player, amount);
    }

    
    public boolean hasBankSupport() {
        return false;
    }

    
    public EconomyResponse withdrawPlayer(String playerName, String worldName, BigDecimal amount) {
        return withdrawPlayer(playerName, amount);
    }

    
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, BigDecimal amount) {
        return withdrawPlayer(player, amount);
    }

    // Vault2 specific methods
    
    public boolean updateAccountPermission(String accountName, UUID uuid, UUID uuid1, AccountPermission accountPermission, boolean b) {
        return false;
    }

    
    public boolean hasAccountPermission(String accountName, UUID uuid, UUID uuid1, AccountPermission accountPermission) {
        return false;
    }

    
    public List<String> getAccountNames(OfflinePlayer offlinePlayer) {
        return Collections.emptyList();
    }

    
    public List<String> getAccountNames(String s) {
        return Collections.emptyList();
    }

    
    public BigDecimal getBalance(String s, String s1, String s2) {
        return BigDecimal.ZERO;
    }

    
    public BigDecimal getBalance(OfflinePlayer offlinePlayer, String s, String s1) {
        return BigDecimal.ZERO;
    }

    
    public EconomyResponse withdrawPlayer(String s, String s1, String s2, BigDecimal bigDecimal) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not implemented");
    }

    
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String s, String s1, BigDecimal bigDecimal) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not implemented");
    }

    
    public EconomyResponse depositPlayer(String s, String s1, String s2, BigDecimal bigDecimal) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not implemented");
    }

    
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String s, String s1, BigDecimal bigDecimal) {
        return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Not implemented");
    }

    
    public boolean has(String s, String s1, String s2, BigDecimal bigDecimal) {
        return false;
    }

    
    public boolean has(OfflinePlayer offlinePlayer, String s, String s1, BigDecimal bigDecimal) {
        return false;
    }
}