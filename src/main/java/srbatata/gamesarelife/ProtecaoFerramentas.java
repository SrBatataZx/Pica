package srbatata.gamesarelife;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.core.Principal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProtecaoFerramentas implements Listener {

    private final Principal plugin;
    private final NamespacedKey keyPicareta;
    private final NamespacedKey keyMachado;

    public ProtecaoFerramentas(Principal plugin) {
        this.plugin = plugin;
        this.keyPicareta = new NamespacedKey(plugin, "blocos_quebrados");
        this.keyMachado = new NamespacedKey(plugin, "blocos_quebrados_machado");
    }

    // Método auxiliar para saber se o item é uma das nossas ferramentas
    private boolean isProtegido(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyPicareta, PersistentDataType.INTEGER) ||
                item.getItemMeta().getPersistentDataContainer().has(keyMachado, PersistentDataType.INTEGER);
    }

    // 1. Impede de jogar no chão (Tecla Q)
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isProtegido(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode jogar suas ferramentas de Aprendiz no chão!");
        }
    }

    // 2. Impede de colocar em Baús, Fornalhas, etc.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        Player player = (Player) event.getWhoClicked();

        // Se o jogador usar "Shift + Clique" para mandar rápido pro baú
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (isProtegido(event.getCurrentItem())) {
                // Só bloqueia se o outro inventário não for o próprio inventário do jogador (CRAFTING)
                if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
                    event.setCancelled(true);
                    player.sendMessage("§cVocê não pode guardar esta ferramenta!");
                }
            }
        }

        // Se o jogador clicar num slot do Baú com a ferramenta presa no mouse
        if (event.getClickedInventory().equals(event.getView().getTopInventory()) && event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
            if (isProtegido(event.getCursor())) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não pode guardar esta ferramenta!");
            }

            // Impede de usar os atalhos do teclado (1-9) para trocar o item direto pro baú
            if (event.getClick() == ClickType.NUMBER_KEY) {
                ItemStack itemNaHotbar = player.getInventory().getItem(event.getHotbarButton());
                if (isProtegido(itemNaHotbar)) {
                    event.setCancelled(true);
                    player.sendMessage("§cVocê não pode guardar esta ferramenta!");
                }
            }
        }
    }

    // 3. Impede de "arrastar" (segurar o clique) a ferramenta para dentro de um baú
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isProtegido(event.getOldCursor())) {
            // Verifica se algum dos slots arrastados pertence ao baú (Top Inventory)
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    ((Player) event.getWhoClicked()).sendMessage("§cVocê não pode guardar esta ferramenta!");
                    return;
                }
            }
        }
    }

    // 4. Impede de colocar em Molduras ou Suportes de Armaduras
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.ITEM_FRAME ||
                event.getRightClicked().getType() == EntityType.GLOW_ITEM_FRAME ||
                event.getRightClicked().getType() == EntityType.ARMOR_STAND) {

            ItemStack itemHand = event.getPlayer().getInventory().getItem(event.getHand());
            if (isProtegido(itemHand)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cVocê não pode colocar esta ferramenta aqui!");
            }
        }
    }

    // 5. BÔNUS: Não perde ao morrer!
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        List<ItemStack> itensParaSalvar = new ArrayList<>();

        Iterator<ItemStack> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (isProtegido(item)) {
                itensParaSalvar.add(item); // Salva o item
                iterator.remove();         // Remove do chão
            }
        }

        // Adiciona os itens de volta ao jogador para quando ele renascer
        event.getItemsToKeep().addAll(itensParaSalvar);
    }
}