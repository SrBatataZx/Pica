package srbatata.gamesarelife.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ConfigHolder implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
        // Retornamos nulo pois não utilizaremos isso para resgatar a instância do inventário,
        // e sim para fazer a verificação com o "instanceof"
        return null;
    }
}
