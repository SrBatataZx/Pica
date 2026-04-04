package srbatata.gamesarelife.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

// Utilizando um Record do Java para criar um Holder limpo e imutável
public record FiltroHolder() implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
        // Retornamos null porque não precisamos de gerir a instância por aqui
        return null;
    }
}