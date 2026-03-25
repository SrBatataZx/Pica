package srbatata.pica;

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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class MenuPrincipal implements Listener, CommandExecutor {

    private final Pica plugin;
    private final SistemaMissoes sistemaMissoes;
    private final SistemaLoja sistemaLoja;
    private final SistemaKits sistemaKits;
    private final NamespacedKey keyItemMenu;

    public MenuPrincipal(Pica plugin, SistemaMissoes sistemaMissoes, SistemaLoja sistemaLoja, SistemaKits sistemaKits) {
        this.plugin = plugin;
        this.sistemaMissoes = sistemaMissoes;
        this.sistemaLoja = sistemaLoja;
        this.sistemaKits = sistemaKits;
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

    @EventHandler public void onJoin(PlayerJoinEvent event) { event.getPlayer().getInventory().setItem(9, criarItemMenu()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent event) { event.getPlayer().getInventory().setItem(9, criarItemMenu()); }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(keyItemMenu, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(keyItemMenu, PersistentDataType.BYTE)) {
            abrirMenuPrincipal(player);
        }
    }

    private void abrirMenuPrincipal(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Menu Principal");

        // Botão das Missões (Slot 11)
        ItemStack missoes = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta metaMissoes = missoes.getItemMeta();
        metaMissoes.setDisplayName("§e§lMenu de Missões");
        metaMissoes.setLore(Arrays.asList("§7Complete tarefas e", "§7ganhe recompensas.", "", "§eClique para acessar!"));
        missoes.setItemMeta(metaMissoes);

        // Botão da Loja (Slot 13)
        ItemStack loja = new ItemStack(Material.EMERALD);
        ItemMeta metaLoja = loja.getItemMeta();
        metaLoja.setDisplayName("§a§lMenu Loja");
        metaLoja.setLore(Arrays.asList("§7Compre e venda itens.", "", "§eClique para acessar!"));
        loja.setItemMeta(metaLoja);

        // Botão dos Kits (Slot 15)
        ItemStack kits = new ItemStack(Material.MINECART);
        ItemMeta metaKits = kits.getItemMeta();
        metaKits.setDisplayName("§b§lMenu de Kits");
        metaKits.setLore(Arrays.asList("§7Pegue suas recompensas", "§7e ferramentas iniciais.", "", "§eClique para acessar!"));
        kits.setItemMeta(metaKits);

        // Botão do Discord (Slot 22 - Abaixo da Loja)
        ItemStack discord = new ItemStack(Material.NAME_TAG);
        ItemMeta metaDiscord = discord.getItemMeta();
        metaDiscord.setDisplayName("§9§lAcesse nosso Discord");
        metaDiscord.setLore(Arrays.asList("§7Faça parte da nossa comunidade!", "", "§b§nGames Are Life", "", "§eClique para receber no chat!"));
        discord.setItemMeta(metaDiscord);

        inv.setItem(11, missoes);
        inv.setItem(13, loja);
        inv.setItem(15, kits);
        inv.setItem(22, discord); // Adicionado exatamente embaixo do slot 13

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack itemClicado = event.getCurrentItem();

        if (itemClicado == null || itemClicado.getType() == Material.AIR) return;

        // Impede de mover a Estrela do Nether fixa
        if (itemClicado.hasItemMeta() && itemClicado.getItemMeta().getPersistentDataContainer().has(keyItemMenu, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> abrirMenuPrincipal(player));
            return;
        }

        // Cliques dentro do Menu Principal
        if (event.getView().getTitle().equals("§8Menu Principal")) {
            event.setCancelled(true);

            if (itemClicado.getType() == Material.ENCHANTED_BOOK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                Bukkit.getScheduler().runTask(plugin, () -> sistemaMissoes.abrirMenuMissoes(player));
            }
            else if (itemClicado.getType() == Material.EMERALD) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                Bukkit.getScheduler().runTask(plugin, () -> sistemaLoja.abrirMenuLoja(player));
            }
            else if (itemClicado.getType() == Material.MINECART) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                Bukkit.getScheduler().runTask(plugin, () -> sistemaKits.abrirMenuKits(player));
            }
            // NOVO CLIQUE: Discord
            else if (itemClicado.getType() == Material.NAME_TAG) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.closeInventory();

                // Envia as mensagens no chat (o Minecraft reconhece links automaticamente e deixa clicável)
                player.sendMessage("");
                player.sendMessage("§9§lDISCORD DO SERVIDOR");
                player.sendMessage("§fJunte-se a nós para novidades, suporte e muito mais!");
                player.sendMessage("§eClique aqui para entrar: §b§nhttp://discord.gg/euh75ek2nZ");
                player.sendMessage("");
            }
        }
    }

    // Comando oculto /menu
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            abrirMenuPrincipal((Player) sender);
        }
        return true;
    }
}