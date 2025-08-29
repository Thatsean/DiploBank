package thatsean.diplobank;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class deposit {

    public static boolean addCurrencyToInventory(Player player, Material currency, int amount) {
        ItemStack itemStack = new ItemStack(currency, amount);
        if (player.getInventory().addItem(itemStack).isEmpty()) {
            return true; // Successfully added all items
        } else {
            return false; // Inventory full, could not add all items
        }
    }
}
