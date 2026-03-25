package srbatata.pica;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;

public class SistemaLoja implements Listener {

    private final Pica plugin;
    private final Economy econ;
    private final SistemaTerrenos sistemaTerrenos;

    public SistemaLoja(Pica plugin, Economy econ, SistemaTerrenos sistemaTerrenos) {
        this.plugin = plugin;
        this.econ = econ;
        this.sistemaTerrenos = sistemaTerrenos;
    }

    // ==========================================
    // 1. MENU PRINCIPAL DA LOJA (CATEGORIAS)
    // ==========================================
    public void abrirMenuLoja(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, "§8Menu Loja: Categorias");

        ItemStack overworld = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta metaOver = overworld.getItemMeta();
        metaOver.setDisplayName("§a§lLoja: Overworld");
        metaOver.setLore(Arrays.asList("§c🚧 Em manutenção 🚧", "§7Aguarde novidades em breve!"));
        overworld.setItemMeta(metaOver);

        ItemStack nether = new ItemStack(Material.NETHERRACK);
        ItemMeta metaNether = nether.getItemMeta();
        metaNether.setDisplayName("§c§lLoja: Nether");
        metaNether.setLore(Arrays.asList("§c🚧 Em manutenção 🚧", "§7Aguarde novidades em breve!"));
        nether.setItemMeta(metaNether);

        ItemStack end = new ItemStack(Material.END_STONE);
        ItemMeta metaEnd = end.getItemMeta();
        metaEnd.setDisplayName("§d§lLoja: The End");
        metaEnd.setLore(Arrays.asList("§7Compre blocos, itens e", "§7relíquias raras do Fim.", "", "§eClique para acessar!"));
        end.setItemMeta(metaEnd);

        double precoLimite = plugin.getConfig().getDouble("loja.terrenos.preco_limite", 10000.0);

        ItemStack limite = new ItemStack(Material.GOLDEN_SHOVEL);
        ItemMeta metaLimite = limite.getItemMeta();
        metaLimite.setDisplayName("§e§l+500 Blocos de Proteção");
        metaLimite.setLore(Arrays.asList("§7Aumente o tamanho máximo", "§7dos seus terrenos protegidos.", "", "§fPreço: §c" + econ.format(precoLimite), "", "§eClique para comprar!"));
        limite.setItemMeta(metaLimite);

        inv.setItem(11, overworld);
        inv.setItem(13, nether);
        inv.setItem(15, end);
        inv.setItem(31, limite);

        ItemStack voltar = new ItemStack(Material.ARROW);
        ItemMeta metaVoltar = voltar.getItemMeta();
        metaVoltar.setDisplayName("§c§lVoltar ao Menu Principal");
        voltar.setItemMeta(metaVoltar);
        inv.setItem(35, voltar);

        player.openInventory(inv);
    }

    // ==========================================
    // 2. SUB-MENU: LOJA DO FIM (DINÂMICO)
    // ==========================================
    public void abrirLojaEnd(Player player) {
        // Criando inventário de 27 slots
        Inventory inv = Bukkit.createInventory(null, 27, "§8Loja: The End");

        // Puxa a lista de itens da configuração
        ConfigurationSection secaoItens = plugin.getConfig().getConfigurationSection("loja.end.itens");

        if (secaoItens != null) {
            int slot = 0; // Começa do slot 0

            // Passa por todos os itens criados na config
            for (String key : secaoItens.getKeys(false)) {
                if (slot >= 26) break; // Protege o slot 26 para o botão de voltar!

                String matString = secaoItens.getString(key + ".material", "STONE");
                Material material = Material.getMaterial(matString.toUpperCase());

                if (material != null) {
                    String nome = secaoItens.getString(key + ".nome", "Item");
                    int quantidade = secaoItens.getInt(key + ".quantidade", 1);
                    double preco = secaoItens.getDouble(key + ".preco", 100.0);

                    inv.setItem(slot, criarIcone(material, nome, quantidade, preco));
                    slot++; // Vai para o próximo quadrado (1, 2, 3...)
                }
            }
        }

        // Adiciona o botão voltar no último slot fixo (26)
        ItemStack voltar = new ItemStack(Material.ARROW);
        ItemMeta metaVoltar = voltar.getItemMeta();
        metaVoltar.setDisplayName("§c§lVoltar às Categorias");
        voltar.setItemMeta(metaVoltar);
        inv.setItem(26, voltar);

        player.openInventory(inv);
    }

    private ItemStack criarIcone(Material mat, String nome, int quantidade, double preco) {
        ItemStack item = new ItemStack(mat, quantidade);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nome.replace("&", "§")); // Transforma & em cores do Minecraft
        meta.setLore(Arrays.asList(
                "§fQuantidade: §7" + quantidade + "x",
                "§fPreço: §c" + econ.format(preco),
                "",
                "§eClique para comprar!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // 3. EVENTOS DE CLIQUES
    // ==========================================
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack itemClicado = event.getCurrentItem();

        if (itemClicado == null || itemClicado.getType() == Material.AIR) return;

        // --- CLIQUES NO MENU DE CATEGORIAS ---
        if (event.getView().getTitle().equals("§8Menu Loja: Categorias")) {
            event.setCancelled(true);
            int slot = event.getSlot();

            if (slot == 11 || slot == 13) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage("§cEsta loja ainda está em manutenção!");
            }
            else if (slot == 15) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                abrirLojaEnd(player);
            }
            else if (slot == 31) {
                double precoLimite = plugin.getConfig().getDouble("loja.terrenos.preco_limite", 10000.0);
                if (econ.has(player, precoLimite)) {
                    econ.withdrawPlayer(player, precoLimite);
                    sistemaTerrenos.adicionarLimite(player, 500);
                    player.sendMessage("§a🎉 Você comprou §e+500 blocos §ade limite para seus terrenos!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    player.closeInventory();
                } else {
                    player.sendMessage("§cVocê não tem dinheiro suficiente! Custa " + econ.format(precoLimite));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }
            else if (itemClicado.getType() == Material.ARROW) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.performCommand("menu");
            }
            return;
        }

        // --- CLIQUES NO SUB-MENU DO FIM ---
        if (event.getView().getTitle().equals("§8Loja: The End")) {
            event.setCancelled(true);
            int slotClicado = event.getSlot();

            if (slotClicado == 26) { // Botão Voltar
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                abrirMenuLoja(player);
                return;
            }

            // Descobre em qual item o jogador clicou mapeando a config dinamicamente
            ConfigurationSection secaoItens = plugin.getConfig().getConfigurationSection("loja.end.itens");
            if (secaoItens != null) {
                int slotAtual = 0;

                for (String key : secaoItens.getKeys(false)) {
                    // Se o slot atual for o mesmo em que o jogador clicou
                    if (slotAtual == slotClicado) {
                        String matString = secaoItens.getString(key + ".material", "STONE");
                        Material material = Material.getMaterial(matString.toUpperCase());

                        if (material != null) {
                            int quantidade = secaoItens.getInt(key + ".quantidade", 1);
                            double preco = secaoItens.getDouble(key + ".preco", 100.0);

                            processarCompraItem(player, material, quantidade, preco);
                        }
                        break; // Para o laço de repetição depois de achar o item
                    }
                    slotAtual++;
                }
            }
        }
    }

    private void processarCompraItem(Player player, Material material, int quantidade, double preco) {
        if (econ.has(player, preco)) {
            econ.withdrawPlayer(player, preco);
            ItemStack itemComprado = new ItemStack(material, quantidade);
            HashMap<Integer, ItemStack> sobrou = player.getInventory().addItem(itemComprado);
            if (!sobrou.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), itemComprado);
            }
            player.sendMessage("§aVocê comprou §f" + quantidade + "x " + material.name() + " §apor §e" + econ.format(preco) + "§a!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } else {
            player.sendMessage("§cVocê não tem dinheiro suficiente! Custa " + econ.format(preco));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}