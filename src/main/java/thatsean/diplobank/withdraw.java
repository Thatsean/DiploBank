package thatsean.diplobank;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Withdraw { // Fixed class name to PascalCase

    public static boolean removeCurrencyFromInventory(Player player, Material currency, int amount) {
        ItemStack itemStack = new ItemStack(currency, amount);
        if (player.getInventory().containsAtLeast(itemStack, amount)) {
            player.getInventory().removeItem(itemStack);
            return true; // Successfully removed items
        } else {
            return false; // Not enough items to remove
        }
    }
}