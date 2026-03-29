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
import java.util.List;

public class AprendizConstruct implements CommandExecutor {

    private final Principal plugin;

    public AprendizConstruct(Principal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Uso de Pattern Matching (Java 16+) para deixar o código mais limpo
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        int blocosIniciais = plugin.getConfig().getInt("blocos_iniciais", 50);

        // ==========================================
        // 1. CRIANDO O MACHADO DO APRENDIZ
        // ==========================================
        ItemStack machado = new ItemStack(Material.WOODEN_AXE);
        ItemMeta metaMachado = machado.getItemMeta();

        if (metaMachado != null) {
            metaMachado.setDisplayName("§eMachado Do Aprendiz");
            metaMachado.setUnbreakable(true);

            List<String> loreMachado = new ArrayList<>();
            loreMachado.add("§cUnico");
            loreMachado.add("");
            loreMachado.add("§fModo Lenhador: §cDesativado");
            loreMachado.add("§fProgresso para evoluir:");
            loreMachado.add("§8[§7||||||||||§8] §e0/" + blocosIniciais);
            metaMachado.setLore(loreMachado);

            // Tags exclusivas do Machado
            NamespacedKey keyBlocosMachado = new NamespacedKey(plugin, "blocos_quebrados_machado");
            NamespacedKey keyLenhador = new NamespacedKey(plugin, "modo_lenhador_machado");

            metaMachado.getPersistentDataContainer().set(keyBlocosMachado, PersistentDataType.INTEGER, 0);
            metaMachado.getPersistentDataContainer().set(keyLenhador, PersistentDataType.INTEGER, 0);

            machado.setItemMeta(metaMachado);
        }

        // ==========================================
        // 2. CRIANDO A PICARETA DO APRENDIZ
        // ==========================================
        ItemStack picareta = new ItemStack(Material.WOODEN_PICKAXE);
        ItemMeta metaPicareta = picareta.getItemMeta();

        if (metaPicareta != null) {
            metaPicareta.setDisplayName("§ePicareta Do Aprendiz");
            metaPicareta.setUnbreakable(true);

            List<String> lorePicareta = new ArrayList<>();
            lorePicareta.add("§cUnico");
            lorePicareta.add("");
            lorePicareta.add("§fModo Lixeira: §cDesativado");
            lorePicareta.add("§fProgresso para evoluir:");
            lorePicareta.add("§8[§7||||||||||§8] §e0/" + blocosIniciais);
            metaPicareta.setLore(lorePicareta);

            // Tags exclusivas da Picareta
            NamespacedKey keyBlocosPicareta = new NamespacedKey(plugin, "blocos_quebrados");
            NamespacedKey keyLixeira = new NamespacedKey(plugin, "modo_lixeira");

            metaPicareta.getPersistentDataContainer().set(keyBlocosPicareta, PersistentDataType.INTEGER, 0);
            metaPicareta.getPersistentDataContainer().set(keyLixeira, PersistentDataType.INTEGER, 0);

            picareta.setItemMeta(metaPicareta);
        }

        // ==========================================
        // 3. CRIANDO A PÁ DO APRENDIZ
        // ==========================================
        ItemStack pa = new ItemStack(Material.WOODEN_SHOVEL);
        ItemMeta metaPa = pa.getItemMeta();

        if (metaPa != null) {
            metaPa.setDisplayName("§ePá Do Aprendiz");
            metaPa.setUnbreakable(true);

            List<String> lorePa = new ArrayList<>();
            lorePa.add("§cUnico");
            lorePa.add(""); // Índice 1
            lorePa.add("§fModo Lixeira: §cDesativado");
            lorePa.add("§fProgresso para evoluir:");
            lorePa.add("§8[§7||||||||||§8] §e0/" + blocosIniciais);
            metaPa.setLore(lorePa);

            NamespacedKey keyBlocosPa = new NamespacedKey(plugin, "blocos_quebrados_pa");
            NamespacedKey keyLixeiraPa = new NamespacedKey(plugin, "modo_lixeira_pa");

            metaPa.getPersistentDataContainer().set(keyBlocosPa, PersistentDataType.INTEGER, 0);
            metaPa.getPersistentDataContainer().set(keyLixeiraPa, PersistentDataType.INTEGER, 0);

            pa.setItemMeta(metaPa);
        }

        // ==========================================
        // 4. ENTREGANDO OS ITENS AO JOGADOR
        // ==========================================
        // O método addItem do Bukkit suporta Varargs (vários parâmetros do mesmo tipo)
        player.getInventory().addItem(machado, picareta, pa);
        player.sendMessage("§aVocê recebeu o Kit de Ferramentas do Aprendiz!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);

        return true;
    }
}