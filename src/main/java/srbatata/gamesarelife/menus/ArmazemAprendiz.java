package srbatata.gamesarelife.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.core.Principal;

public class ArmazemAprendiz implements Listener {

    private final Principal plugin;
    private final NamespacedKey keyPicareta;
    private final NamespacedKey keyMachado;
    private final NamespacedKey keyPa;

    public ArmazemAprendiz(Principal plugin) {
        this.plugin = plugin;
        this.keyPicareta = new NamespacedKey(plugin, "blocos_quebrados");
        this.keyMachado = new NamespacedKey(plugin, "blocos_quebrados_machado");
        this.keyPa = new NamespacedKey(plugin, "blocos_quebrados_pa");
    }

    public void abrirArmazem(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "§8Armazém do Aprendiz");
        String path = "armazem_aprendiz." + player.getUniqueId();

        // NOVO CARREGAMENTO: Lê slot por slot de forma segura
        if (plugin.getSalvos().contains(path)) {
            for (int i = 0; i < 9; i++) {
                String itemPath = path + "." + i;
                if (plugin.getSalvos().contains(itemPath)) {
                    ItemStack item = plugin.getSalvos().getItemStack(itemPath);
                    if (item != null) {
                        inv.setItem(i, item);
                    }
                }
            }
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    private boolean isProtegido(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyPicareta, PersistentDataType.INTEGER) ||
                item.getItemMeta().getPersistentDataContainer().has(keyMachado, PersistentDataType.INTEGER) ||
                item.getItemMeta().getPersistentDataContainer().has(keyPa, PersistentDataType.INTEGER);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§8Armazém do Aprendiz")) {
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();

            // Bloqueia itens que NÃO são do aprendiz de serem colocados no armazém
            if (cursor != null && cursor.getType() != Material.AIR && !isProtegido(cursor)) {
                if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                    event.setCancelled(true);
                    ((Player) event.getWhoClicked()).sendMessage("§cVocê só pode guardar Ferramentas do Aprendiz aqui!");
                }
            }

            // Tratamento especial para "Shift+Click" do inventário do jogador para o Armazém
            if (event.isShiftClick() && current != null && current.getType() != Material.AIR) {
                if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                    if (!isProtegido(current)) {
                        event.setCancelled(true);
                        ((Player) event.getWhoClicked()).sendMessage("§cVocê só pode guardar Ferramentas do Aprendiz aqui!");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("§8Armazém do Aprendiz")) {
            Player player = (Player) event.getPlayer();
            Inventory inv = event.getInventory();
            String path = "armazem_aprendiz." + player.getUniqueId();

            // Limpa os dados antigos para não haver itens "fantasmas"
            plugin.getSalvos().set(path, null);

            // NOVO SALVAMENTO: Salva slot por slot (ignora os vazios)
            ItemStack[] contents = inv.getContents();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null && contents[i].getType() != Material.AIR) {
                    plugin.getSalvos().set(path + "." + i, contents[i]);
                }
            }

            plugin.saveSalvos();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.2f);
        }
    }
}