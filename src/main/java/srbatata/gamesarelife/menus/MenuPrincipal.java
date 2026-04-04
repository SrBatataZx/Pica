package srbatata.gamesarelife.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.core.Principal;

import java.util.List;

public class MenuPrincipal implements Listener, CommandExecutor {

    private final Principal plugin;
    private final MenuPerfil menuPerfil; // Nova injeção de dependência
    private final MenuLoja sistemaLoja;
    private final SistemaKits sistemaKits;
    private final NamespacedKey keyItemMenu;

    public MenuPrincipal(Principal plugin, MenuPerfil menuPerfil, MenuLoja sistemaLoja, SistemaKits sistemaKits) {
        this.plugin = plugin;
        this.menuPerfil = menuPerfil;
        this.sistemaLoja = sistemaLoja;
        this.sistemaKits = sistemaKits;
        this.keyItemMenu = new NamespacedKey(plugin, "item_menu_principal");
    }

    private ItemStack criarItemMenu() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lMenu Principal");
        meta.setLore(List.of("§7Clique para abrir", "§7os sistemas do servidor."));
        meta.getPersistentDataContainer().set(keyItemMenu, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isItemMenu(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyItemMenu, PersistentDataType.BYTE);
    }

    private void limparMenusDuplicados(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (isItemMenu(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        limparMenusDuplicados(event.getPlayer());
        event.getPlayer().getInventory().setItem(8, criarItemMenu());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        limparMenusDuplicados(event.getPlayer());
        event.getPlayer().getInventory().setItem(8, criarItemMenu());
    }

    // ==========================================
    // BLOQUEIOS ANTI-BUG DO ITEM
    // ==========================================

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isItemMenu(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (isItemMenu(event.getMainHandItem()) || isItemMenu(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isItemMenu(event.getOldCursor()) || isItemMenu(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Pattern Matching: Se não for player, encerra a execução e faz o cast automático
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // 1. BLINDAGEM DO SLOT 8 E DO ITEM
        boolean isMenuClick = isItemMenu(current) || isItemMenu(cursor);
        boolean isSlot8 = event.getSlot() == 8 && event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory());
        boolean isTecladoSlot8 = event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() == 8;

        if (isMenuClick || isSlot8 || isTecladoSlot8) {
            event.setCancelled(true);

            if (isItemMenu(current) && event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                Bukkit.getScheduler().runTask(plugin, () -> abrirMenuPrincipal(player));
            }
            return;
        }

        // 2. LÓGICA DO MENU QUANDO ELE ESTIVER ABERTO (GUI)
        if (event.getView().getTitle().equals("§8Menu Principal")) {
            event.setCancelled(true);

            if (current == null || current.getType() == Material.AIR) return;

            if (current.getType() == Material.PLAYER_HEAD) { // ABRIR O SUB-MENU
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                Bukkit.getScheduler().runTask(plugin, () -> menuPerfil.abrirMenuPerfil(player));
            }
            else if (current.getType() == Material.EMERALD) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                Bukkit.getScheduler().runTask(plugin, () -> sistemaLoja.abrirMenuLoja(player));
            }
            else if (current.getType() == Material.MINECART) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                Bukkit.getScheduler().runTask(plugin, () -> sistemaKits.abrirMenuKits(player));
            }
            else if (current.getType() == Material.NAME_TAG) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.closeInventory();

                player.sendMessage("");
                player.sendMessage("§9§lDISCORD DO SERVIDOR");
                player.sendMessage("§fJunte-se a nós para novidades, suporte e muito mais!");
                player.sendMessage("§eClique aqui para entrar: §b§nhttp://discord.gg/euh75ek2nZ");
                player.sendMessage("");
            }
        }
    }

    // ==========================================
    // ABERTURA DO MENU E OUTROS MÉTODOS
    // ==========================================

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && isItemMenu(item)) {
            abrirMenuPrincipal(player);
        }
    }

    private void abrirMenuPrincipal(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Menu Principal");

        // Cria a cabeça do jogador
        ItemStack perfil = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta metaPerfil = (SkullMeta) perfil.getItemMeta();
        if (metaPerfil != null) {
            metaPerfil.setOwningPlayer(player); // Carrega a skin assincronamente (Paper 1.21)
            metaPerfil.setDisplayName("§b§l" + player.getName());
            metaPerfil.setLore(List.of("§7Acesse suas Missões", "§7e Armazém aqui.", "", "§eClique para acessar!"));
            perfil.setItemMeta(metaPerfil);
        }

        ItemStack loja = new ItemStack(Material.EMERALD);
        ItemMeta metaLoja = loja.getItemMeta();
        metaLoja.setDisplayName("§a§lMenu Loja");
        metaLoja.setLore(List.of("§7Compre e venda itens.", "", "§eClique para acessar!"));
        loja.setItemMeta(metaLoja);

        ItemStack kits = new ItemStack(Material.MINECART);
        ItemMeta metaKits = kits.getItemMeta();
        metaKits.setDisplayName("§b§lMenu de Kits");
        metaKits.setLore(List.of("§7Pegue suas recompensas", "§7e ferramentas iniciais.", "", "§eClique para acessar!"));
        kits.setItemMeta(metaKits);

        ItemStack discord = new ItemStack(Material.NAME_TAG);
        ItemMeta metaDiscord = discord.getItemMeta();
        metaDiscord.setDisplayName("§9§lAcesse nosso Discord");
        metaDiscord.setLore(List.of("§7Faça parte da nossa comunidade!", "", "§b§nGames Are Life", "", "§eClique para receber no chat!"));
        discord.setItemMeta(metaDiscord);

        // Posicionamento harmônico com 4 itens no GUI de tamanho 27
        inv.setItem(11, perfil);
        inv.setItem(13, loja);
        inv.setItem(15, kits);
        inv.setItem(22, discord);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            abrirMenuPrincipal(player);
        }
        return true;
    }
}