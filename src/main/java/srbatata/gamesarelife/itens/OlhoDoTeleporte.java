package srbatata.gamesarelife.itens;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import srbatata.gamesarelife.blocos.PedraTeleporte;
import srbatata.gamesarelife.dados.GereWaystone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OlhoDoTeleporte implements Listener {

    private final JavaPlugin plugin;
    private final GereWaystone gereWaystone;
    public static NamespacedKey OLHO_ITEM_KEY;
    private final String TITULO_MENU = "§8Suas Pedras de Teleporte";

    public OlhoDoTeleporte(JavaPlugin plugin, GereWaystone gereWaystone) {
        this.plugin = plugin;
        this.gereWaystone = gereWaystone;
        OLHO_ITEM_KEY = new NamespacedKey(plugin, "item_olho_teleporte");
        registrarReceita();
    }

    private void registrarReceita() {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Olho do Teleporte").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text("Clique direito para abrir suas").color(NamedTextColor.GRAY),
                Component.text("rotas de teleporte salvas.").color(NamedTextColor.GRAY)
        ));

        meta.getPersistentDataContainer().set(OLHO_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "craft_olho_teleporte"), item);
        recipe.shape(" G ", "GEG", " D ");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('E', Material.ENDER_EYE);
        recipe.setIngredient('D', Material.DIAMOND);

        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(OLHO_ITEM_KEY, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                abrirMenuWaystones(event.getPlayer());
            }
        }
    }

    private void abrirMenuWaystones(Player player) {
        String salvasStr = player.getPersistentDataContainer().getOrDefault(PedraTeleporte.SAVED_WAYSTONES_KEY, PersistentDataType.STRING, "");
        if (salvasStr.isEmpty()) {
            player.sendMessage(Component.text("Você ainda não memorizou nenhuma Pedra de Teleporte!").color(NamedTextColor.RED));
            return;
        }

        String[] waystonesSalvas = salvasStr.split(";");
        List<String> waystonesAtivas = gereWaystone.getWaystonesAtivas();
        List<String> waystonesValidas = new ArrayList<>();
        boolean precisaLimparPDC = false;

        Inventory inv = Bukkit.createInventory(null, 27, TITULO_MENU);
        int slot = 0;

        for (String w : waystonesSalvas) {
            if (w.isEmpty()) continue;

            String[] partes = w.split("@");
            String locString = partes[0];
            String nomeCustomizado = partes.length > 1 ? partes[1] : "Waystone " + (slot + 1);

            // CORREÇÃO: Usamos o nosso método auxiliar para checar se a pedra ainda existe
            if (!isWaystoneAtiva(locString, waystonesAtivas)) {
                precisaLimparPDC = true;
                continue;
            }

            waystonesValidas.add(w);

            if (slot >= 27) continue;

            Location loc = PedraTeleporte.desserializarLocation(locString);
            if (loc != null) {
                ItemStack icone = new ItemStack(Material.LODESTONE);
                ItemMeta meta = icone.getItemMeta();

                meta.displayName(Component.text(nomeCustomizado).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
                meta.lore(List.of(
                        Component.text("Mundo: " + loc.getWorld().getName()).color(NamedTextColor.GRAY),
                        Component.text("X: " + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ()).color(NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("Clique para teleportar!").color(NamedTextColor.GREEN)
                ));

                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "target_loc"), PersistentDataType.STRING, locString);
                icone.setItemMeta(meta);
                inv.setItem(slot++, icone);
            }
        }

        if (precisaLimparPDC) {
            String novaListaStr = String.join(";", waystonesValidas);
            player.getPersistentDataContainer().set(PedraTeleporte.SAVED_WAYSTONES_KEY, PersistentDataType.STRING, novaListaStr);
        }

        if (waystonesValidas.isEmpty()) {
            player.sendMessage(Component.text("Todas as suas Pedras de Teleporte foram destruídas!").color(NamedTextColor.RED));
            return;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.0f);
    }

    @EventHandler
    public void onClickMenu(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MENU)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicado = event.getCurrentItem();

        if (clicado != null && clicado.hasItemMeta()) {
            NamespacedKey locKey = new NamespacedKey(plugin, "target_loc");
            if (clicado.getItemMeta().getPersistentDataContainer().has(locKey, PersistentDataType.STRING)) {
                String locString = clicado.getItemMeta().getPersistentDataContainer().get(locKey, PersistentDataType.STRING);
                Location loc = PedraTeleporte.desserializarLocation(locString);

                List<String> waystonesAtivas = gereWaystone.getWaystonesAtivas();

                // CORREÇÃO: Usamos o método auxiliar aqui também
                if (loc != null && isWaystoneAtiva(locString, waystonesAtivas)) {
                    player.closeInventory();

                    player.teleportAsync(loc.add(0.5, 1, 0.5)).thenAccept(sucesso -> {
                        if (sucesso) {
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                            player.sendMessage(Component.text("Woosh! Teleportado!").color(NamedTextColor.LIGHT_PURPLE));
                        }
                    });
                } else {
                    player.sendMessage(Component.text("Esta Pedra de Teleporte acabou de ser destruída!").color(NamedTextColor.RED));

                    String salvasStr = player.getPersistentDataContainer().getOrDefault(PedraTeleporte.SAVED_WAYSTONES_KEY, PersistentDataType.STRING, "");
                    List<String> listaSalvas = new ArrayList<>(Arrays.asList(salvasStr.split(";")));
                    listaSalvas.removeIf(entrada -> entrada.startsWith(locString));
                    player.getPersistentDataContainer().set(PedraTeleporte.SAVED_WAYSTONES_KEY, PersistentDataType.STRING, String.join(";", listaSalvas));

                    player.closeInventory();
                }
            }
        }
    }

    // MÉTODO AUXILIAR NOVO: Verifica se a coordenada está na lista, ignorando de quem é o UUID
    private boolean isWaystoneAtiva(String locString, List<String> ativas) {
        for (String w : ativas) {
            // Verifica se começa com a localização exata, mais o ponto e vírgula para não confundir coordenadas parecidas
            if (w.startsWith(locString + ";") || w.equals(locString)) { // "w.equals" caso tenha alguma pedra antiga sem dono
                return true;
            }
        }
        return false;
    }
}