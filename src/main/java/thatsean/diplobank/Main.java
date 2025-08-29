package thatsean.diplobank;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin {

    private VaultUpdate economy;
    private File currencyFile;
    // private FileConfiguration currencyConfig; // Removed unused field

    @Override
    public void onEnable() {
        // Save the default config.yml if it doesn't exist
        saveDefaultConfig();

        // Create or load the currency.yml file
        createOrLoadCurrencyFile();

        // Register the plugin with Vault
        economy = new VaultUpdate(this); // Replace with your implementation
        getServer().getServicesManager().register(Economy.class, economy, this, ServicePriority.High);
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
        YamlConfiguration.loadConfiguration(currencyFile); // Removed assignment to unused field
        getLogger().info("Currency file loaded.");
    }
}