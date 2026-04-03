package srbatata.gamesarelife.itens;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.core.Principal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AprendizConstruct implements CommandExecutor {

    private final Principal plugin;

    public AprendizConstruct(Principal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        // Usa os métodos que criamos abaixo para entregar o kit completo
        player.getInventory().addItem(getMachado(plugin), getPicareta(plugin), getPa(plugin));

        player.sendMessage("§aVocê recebeu o Kit de Ferramentas do Aprendiz!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        return true;
    }

    // --- MÉTODOS ESTÁTICOS PARA GERAR AS FERRAMENTAS REAIS ---

    public static ItemStack getMachado(Principal plugin) {
        int blocos = plugin.getConfig().getInt("blocos_iniciais", 50);
        ItemStack item = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eMachado Do Aprendiz");
        meta.setUnbreakable(true);
        meta.setLore(Arrays.asList("§cUnico", "", "§fModo Lenhador: §cDesativado", "§fProgresso para evoluir:", "§8[§7||||||||||§8] §e0/" + blocos));

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "blocos_quebrados_machado"), PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "modo_lenhador_machado"), PersistentDataType.INTEGER, 0);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getPicareta(Principal plugin) {
        int blocos = plugin.getConfig().getInt("blocos_iniciais", 50);
        ItemStack item = new ItemStack(Material.WOODEN_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§ePicareta Do Aprendiz");
        meta.setUnbreakable(true);
        meta.setLore(Arrays.asList("§cUnico", "", "§fModo Lixeira: §cDesativado", "§fProgresso para evoluir:", "§8[§7||||||||||§8] §e0/" + blocos));

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "blocos_quebrados"), PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "modo_lixeira"), PersistentDataType.INTEGER, 0);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getPa(Principal plugin) {
        int blocos = plugin.getConfig().getInt("blocos_iniciais", 50);
        ItemStack item = new ItemStack(Material.WOODEN_SHOVEL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§ePá Do Aprendiz");
        meta.setUnbreakable(true);
        meta.setLore(Arrays.asList("§cUnico", "", "§fModo Lixeira: §cDesativado", "§fProgresso para evoluir:", "§8[§7||||||||||§8] §e0/" + blocos));

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "blocos_quebrados_pa"), PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "modo_lixeira_pa"), PersistentDataType.INTEGER, 0);

        item.setItemMeta(meta);
        return item;
    }
}