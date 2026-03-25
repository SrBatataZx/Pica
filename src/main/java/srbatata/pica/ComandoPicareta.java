package srbatata.pica;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import srbatata.pica.core.Pica;

import java.util.ArrayList;
import java.util.List;

public record ComandoPicareta(Pica plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        int blocosIniciais = plugin.getConfig().getInt("blocos_iniciais", 50);

        ItemStack picareta = new ItemStack(Material.WOODEN_PICKAXE);
        ItemMeta meta = picareta.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§ePicareta Do Aprendiz");
            meta.setUnbreakable(true);

            List<String> lore = new ArrayList<>();
            lore.add("§cUnico");
            lore.add("");
            lore.add("§fModo Lixeira: §cDesativado"); // Índice 2 da Lore
            lore.add("§fProgresso para evoluir:");   // Índice 3
            lore.add("§8[§7||||||||||§8] §e0/" + blocosIniciais); // Índice 4

            meta.setLore(lore);

            // Tags ocultas
            NamespacedKey keyBlocos = new NamespacedKey(plugin, "blocos_quebrados");
            meta.getPersistentDataContainer().set(keyBlocos, PersistentDataType.INTEGER, 0);

            // 0 = Desativado, 1 = Ativado
            NamespacedKey keyLixeira = new NamespacedKey(plugin, "modo_lixeira");
            meta.getPersistentDataContainer().set(keyLixeira, PersistentDataType.INTEGER, 0);

            picareta.setItemMeta(meta);
        }

        player.getInventory().addItem(picareta);
        player.sendMessage("§aVocê recebeu a Picareta Do Aprendiz!");
        return true;
    }
}