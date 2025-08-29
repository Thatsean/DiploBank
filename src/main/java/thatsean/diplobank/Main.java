package thatsean.diplobank;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault2.economy.Economy;

public class Main extends JavaPlugin {

    private VaultUpdate economy;
    private File currencyFile;

    @Override
    public void onEnable() {
        // Save the default config.yml if it doesn't exist
        saveDefaultConfig();

        // Create or load the currency.yml file
        createOrLoadCurrencyFile();

        // Register the plugin with Vault
        economy = new VaultUpdate(this);
        getServer().getServicesManager().register(Economy.class, economy, this, ServicePriority.High);
        
        // Register command executors using Paper's recommended approach
        registerCommands();
        
        getLogger().info("Diplobank plugin enabled and registered with Vault.");
    }

    @Override
    public void onDisable() {
        // Unregister the plugin from Vault
        getServer().getServicesManager().unregister(Economy.class, economy);
        getLogger().info("Diplobank plugin disabled and unregistered from Vault.");
    }

    private void createOrLoadCurrencyFile() {
        currencyFile = new File(getDataFolder(), "currency.yml");
        if (!currencyFile.exists()) {
            try {
                getDataFolder().mkdirs();
                currencyFile.createNewFile();
                getLogger().info("Default currency.yml created.");
            } catch (IOException e) {
                getLogger().severe("Could not create currency.yml: " + e.getMessage());
            }
        }
        YamlConfiguration.loadConfiguration(currencyFile);
        getLogger().info("Currency file loaded.");
    }

    private void registerCommands() {
        // Register balance command
        registerCommandExecutor("balance", new BalanceCommand(economy), Arrays.asList("bal"), 
                       "Check your currency balances", "/balance [currency]", "mcurbank.user");
        
        // Register newcurrency command
        registerCommandExecutor("newcurrency", new CurrencyCreator(currencyFile), Arrays.asList("nc"), 
                       "Create a new currency", "/newcurrency <name> <symbol> <material>", "mcurbank.user");
        
        // Register deletecurrency command
        registerCommandExecutor("deletecurrency", new CurrencyCreator(currencyFile), Arrays.asList("dc"), 
                       "Delete a currency", "/deletecurrency <name>", "mcurbank.user");
        
        // Register bankadmin command
        registerCommandExecutor("bankadmin", new AdminCommands(), null, 
                       "Admin currency management", "/bankadmin <deposit|withdraw> <player> <currency> <amount>", "mcurbank.admin");
    }

    private void registerCommandExecutor(String name, CommandExecutor executor, java.util.List<String> aliases, 
                                String description, String usage, String permission) {
        try {
            // Use reflection to create PluginCommand instance
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            constructor.setAccessible(true);
            PluginCommand command = constructor.newInstance(name, this);
            
            // Set command properties
            command.setExecutor(executor);
            if (aliases != null) command.setAliases(aliases);
            command.setDescription(description);
            command.setUsage(usage);
            if (permission != null) command.setPermission(permission);
            
            // Register the command
            getServer().getCommandMap().register("diplobank", command);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            getLogger().severe("Failed to register command " + name + ": " + e.getMessage());
        }
    }
}