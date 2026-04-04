package srbatata.gamesarelife.menus;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.sistemas.SistemaTerrenos;
import srbatata.gamesarelife.core.Principal;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MenuLoja implements Listener {

    private final Principal plugin;
    private final Economy econ;
    private final SistemaTerrenos sistemaTerrenos;
    private final NamespacedKey lojaTargetKey;

    public MenuLoja(Principal plugin, Economy econ, SistemaTerrenos sistemaTerrenos) {
        this.plugin = plugin;
        this.econ = econ;
        this.sistemaTerrenos = sistemaTerrenos;
        this.lojaTargetKey = new NamespacedKey(plugin, "loja_target_uuid");
    }

    // ==========================================
    // 1. MENU SELETOR INICIAL (MENU PRINCIPAL)
    // ==========================================
    public void abrirMenuLoja(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Menu Loja: Categorias");

        // Ícone: Loja do Servidor
        inv.setItem(11, criarItem(Material.NETHER_STAR, "§b§lLoja do Servidor",
                List.of("§7Compre itens oficiais e blocos", "§7de dimensões exploradas.", "", "§eClique para acessar!")));

        // Ícone: Loja dos Jogadores
        inv.setItem(15, criarItem(Material.PLAYER_HEAD, "§6§lLojas dos Jogadores",
                List.of("§7Visite lojas criadas por outros", "§7jogadores da comunidade.", "", "§eClique para acessar!")));

        // Botão Voltar (Comando /menu)
        inv.setItem(26, criarItem(Material.ARROW, "§cVoltar", List.of()));

        player.openInventory(inv);
    }

    // ==========================================
    // 2. CATEGORIAS DO SERVIDOR (DIMENSÕES)
    // ==========================================
    public void abrirMenuServidor(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, "§8Loja: Servidor");

        inv.setItem(11, criarItem(Material.GRASS_BLOCK, "§a§lLoja: Overworld", List.of("§c🚧 Em manutenção 🚧")));
        inv.setItem(13, criarItem(Material.NETHERRACK, "§c§lLoja: Nether", List.of("§c🚧 Em manutenção 🚧")));
        inv.setItem(15, criarItem(Material.END_STONE, "§d§lLoja: The End", List.of("§7Itens raros do Fim.", "", "§eClique para acessar!")));

        // Compra de limite de blocos (Movido para cá por ser um serviço do servidor)
        double precoLimite = plugin.getConfig().getDouble("loja.terrenos.preco_limite", 10000.0);
        inv.setItem(31, criarItem(Material.GOLDEN_SHOVEL, "§e§l+500 Blocos de Proteção",
                List.of("§7Aumente seus terrenos.", "", "§fPreço: §c" + econ.format(precoLimite), "", "§eClique para comprar!")));

        inv.setItem(35, criarItem(Material.ARROW, "§cVoltar", List.of()));

        player.openInventory(inv);
    }

    // ==========================================
    // 3. LISTA DE LOJAS DOS JOGADORES (COMMUNITY)
    // ==========================================
    public void abrirLojasJogadores(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Lojas: Jogadores");

        ConfigurationSection secao = plugin.getSalvos().getConfigurationSection("lojas_publicas");
        if (secao != null) {
            for (String uuidStr : secao.getKeys(false)) {
                String nomeDono = secao.getString(uuidStr + ".owner_name", "Desconhecido");

                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)));
                    meta.setDisplayName("§aLoja de §f" + nomeDono);
                    meta.setLore(List.of("§7Clique para visitar a loja", "§7deste jogador!", "", "§eClique para teleportar"));
                    meta.getPersistentDataContainer().set(lojaTargetKey, PersistentDataType.STRING, uuidStr);
                    skull.setItemMeta(meta);
                }
                inv.addItem(skull);
            }
        }

        inv.setItem(53, criarItem(Material.ARROW, "§cVoltar", List.of()));
        player.openInventory(inv);
    }

    // ==========================================
    // 4. SUB-MENU DINÂMICO: LOJA DO FIM
    // ==========================================
    public void abrirLojaEnd(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Loja: The End");
        ConfigurationSection secaoItens = plugin.getConfig().getConfigurationSection("loja.end.itens");

        if (secaoItens != null) {
            int slot = 0;
            for (String key : secaoItens.getKeys(false)) {
                if (slot >= 26) break;
                Material material = Material.getMaterial(secaoItens.getString(key + ".material", "STONE").toUpperCase());
                if (material != null) {
                    inv.setItem(slot++, criarIconeVenda(material, secaoItens.getString(key + ".nome", "Item"),
                            secaoItens.getInt(key + ".quantidade", 1), secaoItens.getDouble(key + ".preco", 100.0)));
                }
            }
        }
        inv.setItem(26, criarItem(Material.ARROW, "§cVoltar", List.of()));
        player.openInventory(inv);
    }

    // ==========================================
    // 5. EVENTO DE CLIQUES UNIFICADO
    // ==========================================
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        ItemStack item = event.getCurrentItem();

        if (item == null || item.getType() == Material.AIR) return;

        // --- LÓGICA: MENU INICIAL (SELEÇÃO) ---
        if (title.equals("§8Menu Loja: Categorias")) {
            event.setCancelled(true);
            switch (item.getType()) {
                case NETHER_STAR -> abrirMenuServidor(player);
                case PLAYER_HEAD -> abrirLojasJogadores(player);
                case ARROW -> player.performCommand("menu");
                default -> { return; }
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }

        // --- LÓGICA: LOJA SERVIDOR (DIMENSÕES) ---
        else if (title.equals("§8Loja: Servidor")) {
            event.setCancelled(true);
            int slot = event.getSlot();
            switch (slot) {
                case 11, 13 -> {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage("§cEsta loja ainda está em manutenção!");
                }
                case 15 -> abrirLojaEnd(player);
                case 31 -> processarCompraLimite(player);
                case 35 -> abrirMenuLoja(player);
            }
            if (slot != 31) player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }

        // --- LÓGICA: LOJA JOGADORES (COMMUNITY) ---
        else if (title.equals("§8Lojas: Jogadores")) {
            event.setCancelled(true);
            if (item.getType() == Material.ARROW) {
                abrirMenuLoja(player);
                return;
            }

            String uuidStr = item.getItemMeta().getPersistentDataContainer().get(lojaTargetKey, PersistentDataType.STRING);
            if (uuidStr != null) {
                Location loc = plugin.getSalvos().getLocation("lojas_publicas." + uuidStr + ".location");
                if (loc != null) {
                    player.closeInventory();
                    player.teleportAsync(loc).thenAccept(success -> {
                        if (success) {
                            player.sendMessage("§a🎉 Bem-vindo à loja de " + item.getItemMeta().getDisplayName());
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        }
                    });
                }
            }
        }

        // --- LÓGICA: SUB-MENU END ---
        else if (title.equals("§8Loja: The End")) {
            event.setCancelled(true);
            if (event.getSlot() == 26) {
                abrirMenuServidor(player);
                return;
            }
            processarCompraDinamica(player, event.getSlot());
        }
    }

    // ==========================================
    // MÉTODOS DE APOIO E COMPRA
    // ==========================================

    private void processarCompraLimite(Player player) {
        double preco = plugin.getConfig().getDouble("loja.terrenos.preco_limite", 10000.0);
        if (econ.has(player, preco)) {
            econ.withdrawPlayer(player, preco);
            sistemaTerrenos.adicionarLimite(player, 500);
            player.sendMessage("§a🎉 Limite aumentado em +500 blocos!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            player.sendMessage("§cSaldo insuficiente!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void processarCompraDinamica(Player player, int slot) {
        ConfigurationSection secaoItens = plugin.getConfig().getConfigurationSection("loja.end.itens");
        if (secaoItens == null) return;

        int i = 0;
        for (String key : secaoItens.getKeys(false)) {
            if (i++ == slot) {
                Material mat = Material.getMaterial(secaoItens.getString(key + ".material").toUpperCase());
                int qtd = secaoItens.getInt(key + ".quantidade");
                double preco = secaoItens.getDouble(key + ".preco");

                if (econ.has(player, preco)) {
                    econ.withdrawPlayer(player, preco);
                    player.getInventory().addItem(new ItemStack(mat, qtd)).values().forEach(rest ->
                            player.getWorld().dropItemNaturally(player.getLocation(), rest));
                    player.sendMessage("§aCompra realizada!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } else {
                    player.sendMessage("§cSaldo insuficiente!");
                }
                break;
            }
        }
    }

    private ItemStack criarItem(Material mat, String nome, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nome);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack criarIconeVenda(Material mat, String nome, int qtd, double preco) {
        ItemStack item = new ItemStack(mat, qtd);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nome.replace("&", "§"));
            meta.setLore(Arrays.asList("§fQuantidade: §7" + qtd + "x", "§fPreço: §c" + econ.format(preco), "", "§eClique para comprar!"));
            item.setItemMeta(meta);
        }
        return item;
    }
}