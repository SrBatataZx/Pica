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
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.SistemaMissoes;
import srbatata.gamesarelife.core.Principal;

import java.util.Arrays;

public class MenuPrincipal implements Listener, CommandExecutor {

    private final Principal plugin;
    private final SistemaMissoes sistemaMissoes;
    private final MenuLoja sistemaLoja;
    private final SistemaKits sistemaKits;
    private final NamespacedKey keyItemMenu;
    private final ArmazemAprendiz armazemAprendiz;

    public MenuPrincipal(Principal plugin, SistemaMissoes sistemaMissoes, MenuLoja sistemaLoja, SistemaKits sistemaKits, ArmazemAprendiz armazemAprendiz) {
        this.plugin = plugin;
        this.sistemaMissoes = sistemaMissoes;
        this.sistemaLoja = sistemaLoja;
        this.sistemaKits = sistemaKits;
        this.armazemAprendiz = armazemAprendiz;
        this.keyItemMenu = new NamespacedKey(plugin, "item_menu_principal");
    }

    private ItemStack criarItemMenu() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lMenu Principal");
        meta.setLore(Arrays.asList("§7Clique para abrir", "§7os sistemas do servidor."));
        meta.getPersistentDataContainer().set(keyItemMenu, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    // Método auxiliar para identificar o item facilmente
    private boolean isItemMenu(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyItemMenu, PersistentDataType.BYTE);
    }

    // Remove qualquer estrela duplicada que o jogador possa ter "bugado" antes
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
        // Impede que o jogador aperte "F" para colocar o menu na segunda mão
        if (isItemMenu(event.getMainHandItem()) || isItemMenu(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        // Impede de arrastar itens sobre o menu ou arrastar o próprio menu
        if (isItemMenu(event.getOldCursor()) || isItemMenu(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // 1. BLINDAGEM DO SLOT 8 E DO ITEM
        boolean isMenuClick = isItemMenu(current) || isItemMenu(cursor);
        boolean isSlot8 = event.getSlot() == 8 && event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory());
        boolean isTecladoSlot8 = event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() == 8;

        if (isMenuClick || isSlot8 || isTecladoSlot8) {
            event.setCancelled(true);

            // Se ele tentou clicar na estrela dentro do próprio inventário, apenas abre o menu
            if (isItemMenu(current) && event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                Bukkit.getScheduler().runTask(plugin, () -> abrirMenuPrincipal(player));
            }
            return;
        }

        // 2. LÓGICA DO MENU QUANDO ELE ESTIVER ABERTO (GUI)
        if (event.getView().getTitle().equals("§8Menu Principal")) {
            event.setCancelled(true);

            if (current == null || current.getType() == Material.AIR) return;

            if (current.getType() == Material.ENCHANTED_BOOK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                Bukkit.getScheduler().runTask(plugin, () -> sistemaMissoes.abrirMenuMissoes(player));
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
            else if (current.getType() == Material.ENDER_CHEST) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                // Fecha o menu principal e abre o armazém no próximo tick
                Bukkit.getScheduler().runTask(plugin, () -> armazemAprendiz.abrirArmazem(player));
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

        ItemStack missoes = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta metaMissoes = missoes.getItemMeta();
        metaMissoes.setDisplayName("§e§lMenu de Missões");
        metaMissoes.setLore(Arrays.asList("§7Complete tarefas e", "§7ganhe recompensas.", "", "§eClique para acessar!"));
        missoes.setItemMeta(metaMissoes);

        ItemStack loja = new ItemStack(Material.EMERALD);
        ItemMeta metaLoja = loja.getItemMeta();
        metaLoja.setDisplayName("§a§lMenu Loja");
        metaLoja.setLore(Arrays.asList("§7Compre e venda itens.", "", "§eClique para acessar!"));
        loja.setItemMeta(metaLoja);

        ItemStack kits = new ItemStack(Material.MINECART);
        ItemMeta metaKits = kits.getItemMeta();
        metaKits.setDisplayName("§b§lMenu de Kits");
        metaKits.setLore(Arrays.asList("§7Pegue suas recompensas", "§7e ferramentas iniciais.", "", "§eClique para acessar!"));
        kits.setItemMeta(metaKits);

        ItemStack discord = new ItemStack(Material.NAME_TAG);
        ItemMeta metaDiscord = discord.getItemMeta();
        metaDiscord.setDisplayName("§9§lAcesse nosso Discord");
        metaDiscord.setLore(Arrays.asList("§7Faça parte da nossa comunidade!", "", "§b§nGames Are Life", "", "§eClique para receber no chat!"));
        discord.setItemMeta(metaDiscord);

        ItemStack armazem = new ItemStack(Material.ENDER_CHEST);
        ItemMeta metaArmazem = armazem.getItemMeta();
        metaArmazem.setDisplayName("§d§lArmazém do Aprendiz");
        metaArmazem.setLore(Arrays.asList("§7Guarde suas ferramentas", "§7iniciais aqui com segurança.", "", "§eClique para abrir!"));
        armazem.setItemMeta(metaArmazem);

        inv.setItem(11, missoes);
        inv.setItem(4, armazem);
        inv.setItem(13, loja);
        inv.setItem(15, kits);
        inv.setItem(22, discord);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            abrirMenuPrincipal((Player) sender);
        }
        return true;
    }
}