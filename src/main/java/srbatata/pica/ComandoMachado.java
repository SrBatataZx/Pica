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
import srbatata.pica.core.PicaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ComandoMachado implements CommandExecutor {

    private final PicaPlugin plugin;

    public ComandoMachado(PicaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;
        int blocosIniciais = plugin.getConfig().getInt("blocos_iniciais", 50);

        ItemStack machado = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = machado.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§eMachado Do Aprendiz");
            meta.setUnbreakable(true);

            List<String> lore = new ArrayList<>();
            lore.add("§cUnico");
            lore.add("");
            lore.add("§fModo Lenhador: §cDesativado"); // Novo modo
            lore.add("§fProgresso para evoluir:");
            lore.add("§8[§7||||||||||§8] §e0/" + blocosIniciais);

            meta.setLore(lore);

            NamespacedKey keyBlocos = new NamespacedKey(plugin, "blocos_quebrados_machado");
            NamespacedKey keyLenhador = new NamespacedKey(plugin, "modo_lenhador_machado");

            meta.getPersistentDataContainer().set(keyBlocos, PersistentDataType.INTEGER, 0);
            meta.getPersistentDataContainer().set(keyLenhador, PersistentDataType.INTEGER, 0);

            machado.setItemMeta(meta);
        }

        player.getInventory().addItem(machado);
        player.sendMessage("§aVocê recebeu o Machado Do Aprendiz!");
        return true;
    }
}