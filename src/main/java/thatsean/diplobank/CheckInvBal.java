package thatsean.diplobank;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class CheckInvBal {
    public static int checkInvBal(Player player, Material item) {
        if (player == null || item == null) {
            return 0;
        }
        int bal = 0;
        PlayerInventory inv = player.getInventory();
        for (ItemStack stack : inv.getContents()) {
            if (stack == null) {
                continue;
            }
            if (stack.getType() == item) {
                bal += stack.getAmount();
            }
        }
        ItemStack offHand = inv.getItemInOffHand();
        if (offHand != null && offHand.getType() == item) {
            bal += offHand.getAmount();
        }

        // Update Vault balance
        VaultUpdate vaultUpdate = VaultUpdate.getInstance(); // Assuming a singleton instance
        vaultUpdate.updatePlayerCurrency(player, item);

        return bal;
    }
}