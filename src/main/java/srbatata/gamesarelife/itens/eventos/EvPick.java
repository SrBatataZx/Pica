package srbatata.gamesarelife.itens.eventos;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.FiltroHolder;
import srbatata.gamesarelife.core.Principal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class EvPick implements Listener {

    private final Principal plugin;

    // CHAVES NOVAS (Usadas para novas picaretas com o nome atual do plugin)
    private final NamespacedKey keyBlocos;
    private final NamespacedKey keyLixeira;
    private final NamespacedKey keyFiltro;

    // CHAVES ANTIGAS (Usadas para identificar picaretas velhas e convertê-las)
    private final NamespacedKey keyBlocosAntiga;
    private final NamespacedKey keyLixeiraAntiga;
    private final NamespacedKey keyFiltroAntiga;

    public EvPick(Principal plugin) {
        this.plugin = plugin;

        // Chaves Padrão (GamesAreLife)
        this.keyBlocos = new NamespacedKey(plugin, "blocos_quebrados");
        this.keyLixeira = new NamespacedKey(plugin, "modo_lixeira");
        this.keyFiltro = new NamespacedKey(plugin, "lixeira_filtro");

        // Chaves Legadas (Pica)
        this.keyBlocosAntiga = NamespacedKey.fromString("pica:blocos_quebrados");
        this.keyLixeiraAntiga = NamespacedKey.fromString("pica:modo_lixeira");
        this.keyFiltroAntiga = NamespacedKey.fromString("pica:lixeira_filtro");
    }

    // ==========================================
    // SISTEMA DE MIGRAÇÃO (NOVIDADE)
    // ==========================================
    /**
     * Verifica se a picareta tem dados do plugin antigo.
     * Se tiver, copia os dados para as chaves novas e apaga as antigas permanentemente.
     */
    private void converterPicaretaAntiga(ItemStack item, ItemMeta meta) {
        boolean modificou = false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(keyLixeiraAntiga, PersistentDataType.INTEGER)) {
            pdc.set(keyLixeira, PersistentDataType.INTEGER, pdc.get(keyLixeiraAntiga, PersistentDataType.INTEGER));
            pdc.remove(keyLixeiraAntiga);
            modificou = true;
        }
        if (pdc.has(keyBlocosAntiga, PersistentDataType.INTEGER)) {
            pdc.set(keyBlocos, PersistentDataType.INTEGER, pdc.get(keyBlocosAntiga, PersistentDataType.INTEGER));
            pdc.remove(keyBlocosAntiga);
            modificou = true;
        }
        if (pdc.has(keyFiltroAntiga, PersistentDataType.STRING)) {
            pdc.set(keyFiltro, PersistentDataType.STRING, pdc.get(keyFiltroAntiga, PersistentDataType.STRING));
            pdc.remove(keyFiltroAntiga);
            modificou = true;
        }

        if (modificou) {
            item.setItemMeta(meta);
        }
    }

    // ==========================================
    // EVENTO 1: ABRIR MENU (SHIFT+CLICK) OU ALTERNAR LIXEIRA (CLICK)
    // ==========================================
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType() != Material.AIR && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();

                // Antes de qualquer coisa, tentamos atualizar a picareta se for velha
                converterPicaretaAntiga(item, meta);

                if (meta.getPersistentDataContainer().has(keyLixeira, PersistentDataType.INTEGER)) {

                    if (player.isSneaking()) {
                        abrirMenuFiltro(player, item);
                        return;
                    }

                    int estadoLixeira = meta.getPersistentDataContainer().getOrDefault(keyLixeira, PersistentDataType.INTEGER, 0);
                    int novoEstado = (estadoLixeira == 0) ? 1 : 0;

                    meta.getPersistentDataContainer().set(keyLixeira, PersistentDataType.INTEGER, novoEstado);

                    List<String> lore = meta.getLore();
                    if (lore != null && lore.size() >= 5) {
                        lore.set(2, novoEstado == 1 ? "§fModo Lixeira: §aAtivado" : "§fModo Lixeira: §cDesativado");
                        meta.setLore(lore);
                    }

                    item.setItemMeta(meta);

                    if (novoEstado == 1) {
                        player.sendMessage("§aModo Lixeira ATIVADO!");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    } else {
                        player.sendMessage("§cModo Lixeira DESATIVADO!");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
                    }
                }
            }
        }
    }

    // ==========================================
    // EVENTO 2: LÓGICA DE CLIQUES NO MENU DE FILTRO
    // ==========================================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FiltroHolder) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            ItemStack picareta = player.getInventory().getItemInMainHand();
            if (picareta.getType() == Material.AIR || !picareta.hasItemMeta()) return;

            ItemMeta meta = picareta.getItemMeta();
            converterPicaretaAntiga(picareta, meta); // Atualiza se necessário

            if (!meta.getPersistentDataContainer().has(keyLixeira, PersistentDataType.INTEGER)) return;

            String filtroAtual = meta.getPersistentDataContainer().getOrDefault(keyFiltro, PersistentDataType.STRING, "");
            List<String> listaFiltro = new ArrayList<>(Arrays.asList(filtroAtual.split(",")));
            listaFiltro.removeIf(String::isEmpty);

            Material tipoBloco = clickedItem.getType();
            boolean modificou = false;

            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                if (!listaFiltro.contains(tipoBloco.name())) {
                    listaFiltro.add(tipoBloco.name());
                    player.sendMessage("§a[+] §fAdicionado §e" + tipoBloco.name() + " §fao filtro da lixeira.");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
                    modificou = true;
                } else {
                    player.sendMessage("§cEsse item já está no filtro!");
                }
            }
            else if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (listaFiltro.contains(tipoBloco.name())) {
                    listaFiltro.remove(tipoBloco.name());
                    player.sendMessage("§c[-] §fRemovido §e" + tipoBloco.name() + " §fdo filtro da lixeira.");
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    modificou = true;
                }
            }

            if (modificou) {
                meta.getPersistentDataContainer().set(keyFiltro, PersistentDataType.STRING, String.join(",", listaFiltro));
                picareta.setItemMeta(meta);
                Bukkit.getScheduler().runTask(plugin, () -> abrirMenuFiltro(player, picareta));
            }
        }
    }

    // ==========================================
    // MÉTODOS DE RENDERIZAÇÃO DO GUI
    // ==========================================
    private void abrirMenuFiltro(Player player, ItemStack picareta) {
        Inventory gui = Bukkit.createInventory(new FiltroHolder(), 27, Component.text("§8Filtro da Lixeira"));

        ItemMeta meta = picareta.getItemMeta();
        converterPicaretaAntiga(picareta, meta); // Garantia extra

        String filtroStr = meta.getPersistentDataContainer().getOrDefault(keyFiltro, PersistentDataType.STRING, "");

        if (!filtroStr.isEmpty()) {
            String[] materiais = filtroStr.split(",");
            for (String matName : materiais) {
                Material mat = Material.matchMaterial(matName);
                if (mat != null) {
                    ItemStack itemFiltro = new ItemStack(mat);
                    ItemMeta itemMeta = itemFiltro.getItemMeta();
                    itemMeta.setDisplayName("§cRemover " + mat.name());
                    itemMeta.setLore(List.of("§7Clique para remover", "§7item do filtro de lixeira."));
                    itemFiltro.setItemMeta(itemMeta);
                    gui.addItem(itemFiltro);
                }
            }
        }

        player.openInventory(gui);
    }

    // ==========================================
    // EVENTO 3: QUEBRAR BLOCO (LÓGICA DA LIXEIRA)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack itemHand = player.getInventory().getItemInMainHand();

        if (itemHand.getType() == Material.AIR || !itemHand.hasItemMeta()) return;

        ItemMeta meta = itemHand.getItemMeta();
        converterPicaretaAntiga(itemHand, meta);

        if (meta == null) return;

        converterPicaretaAntiga(itemHand, meta);

        // Verifica se é uma Picareta do Sistema (tem a tag de blocos)
        if (meta.getPersistentDataContainer().has(keyBlocos, PersistentDataType.INTEGER)) {

            // 1. SORTEIO DE DROPS ESPECIAIS (Independente de ser pedra ou não)
            List<ItemStack> dropsParaEntregar = new ArrayList<>();
            List<ItemStack> sorteados = gerarDropsEspeciais();
            if (sorteados != null) dropsParaEntregar.addAll(sorteados);

            // 2. LÓGICA DE COLETA AUTOMÁTICA E LIXEIRA
            if (plugin.getConfig().getBoolean("sistema_coleta_ativa", true)) {
                // Cancela os drops no chão para coletar no inventário
                event.setDropItems(false);

                // Adiciona os drops normais do bloco quebrado
                dropsParaEntregar.addAll(event.getBlock().getDrops(itemHand));

                int estadoLixeira = meta.getPersistentDataContainer().getOrDefault(keyLixeira, PersistentDataType.INTEGER, 0);
                String filtroStr = meta.getPersistentDataContainer().getOrDefault(keyFiltro, PersistentDataType.STRING, "");

                // Aplica o Filtro da Lixeira
                if (estadoLixeira == 1 && !filtroStr.isEmpty()) {
                    List<String> listaFiltro = Arrays.asList(filtroStr.split(","));
                    dropsParaEntregar.removeIf(item -> listaFiltro.contains(item.getType().name()));
                }

                // Entrega os itens ao jogador
                HashMap<Integer, ItemStack> sobrou = player.getInventory().addItem(dropsParaEntregar.toArray(new ItemStack[0]));

                // Se o inventário encher, dropa o que sobrou no chão
                if (!sobrou.isEmpty()) {
                    Location loc = event.getBlock().getLocation();
                    for (ItemStack item : sobrou.values()) {
                        loc.getWorld().dropItemNaturally(loc, item);
                    }
                }
            } else {
                // Se a coleta ativa estiver DESLIGADA, dropa os itens sorteados no chão
                if (!dropsParaEntregar.isEmpty()) {
                    Location loc = event.getBlock().getLocation();
                    for (ItemStack item : dropsParaEntregar) {
                        loc.getWorld().dropItemNaturally(loc, item);
                    }
                }
            }


            Material tipoBloco = event.getBlock().getType();
            if (!ehPedra(tipoBloco)) return;

            int blocosTotais = meta.getPersistentDataContainer().getOrDefault(keyBlocos, PersistentDataType.INTEGER, 0);
            blocosTotais++;
            meta.getPersistentDataContainer().set(keyBlocos, PersistentDataType.INTEGER, blocosTotais);

            int metaAtual = plugin.getConfig().getInt("blocos_iniciais", 50);
            int tier = 0;
            int progressoNoTier = blocosTotais;

            while (progressoNoTier >= metaAtual && tier < 5) {
                progressoNoTier -= metaAtual;
                tier++;
                metaAtual *= 2;
            }

            List<String> lore = meta.getLore();
            if (lore != null && lore.size() >= 5) {
                lore.set(4, gerarBarraProgresso(tier, progressoNoTier, metaAtual));
                meta.setLore(lore);
            }

            itemHand.setItemMeta(meta);
            tentarEncantar(itemHand, player);
            evoluirPicareta(itemHand, tier, player);
        }
    }


    // --- MÉTODOS AUXILIARES INALTERADOS ---
    private boolean ehPedra(Material mat) {
        return mat == Material.STONE || mat == Material.COBBLESTONE || mat == Material.DEEPSLATE ||
                mat == Material.COBBLED_DEEPSLATE || mat == Material.ANDESITE ||
                mat == Material.DIORITE || mat == Material.GRANITE || mat == Material.TUFF;
    }

    private String gerarBarraProgresso(int tier, int progressoAtual, int metaAtual) {
        if (tier >= 5) return "§8[§a||||||||||§8] §eMÁXIMO";
        int barrasPreenchidas = (progressoAtual * 10) / metaAtual;
        StringBuilder barra = new StringBuilder("§8[");
        for (int i = 0; i < 10; i++) barra.append(i < barrasPreenchidas ? "§a|" : "§7|");
        barra.append("§8] §e").append(progressoAtual).append("/").append(metaAtual);
        return barra.toString();
    }

    private void evoluirPicareta(ItemStack picareta, int tier, Player player) {
        Material novoMaterial = switch (tier) {
            case 1 -> Material.STONE_PICKAXE;
            case 2 -> Material.IRON_PICKAXE;
            case 3 -> Material.GOLDEN_PICKAXE;
            case 4 -> Material.DIAMOND_PICKAXE;
            case 5 -> Material.NETHERITE_PICKAXE;
            default -> picareta.getType();
        };

        if (picareta.getType() != novoMaterial) {
            picareta.setType(novoMaterial);
            player.sendMessage("§a🎉 Fantástico! Sua picareta evoluiu de material!");
        }
    }

    private void tentarEncantar(ItemStack picareta, Player player) {
        if (Math.random() * 100 <= 0.5) {
            int nivelAtual = picareta.getEnchantmentLevel(Enchantment.EFFICIENCY);
            if (nivelAtual < 5) {
                picareta.addUnsafeEnchantment(Enchantment.EFFICIENCY, nivelAtual + 1);
                player.sendMessage("§b✨ A sua picareta ficou mais rápida! (Eficiência " + (nivelAtual + 1) + ")");
            }
        }
    }

    private List<ItemStack> gerarDropsEspeciais() {
        List<ItemStack> drops = new ArrayList<>();
        if (Math.random() * 100 <= 1.0) drops.add(new ItemStack(Material.RAW_COPPER));
        if (Math.random() * 100 <= 1.0) drops.add(new ItemStack(Material.RAW_GOLD));
        if (Math.random() * 100 <= 1.5) drops.add(new ItemStack(Material.RAW_IRON));
        if (Math.random() * 100 <= 0.5) drops.add(new ItemStack(Material.DIAMOND));
        if (Math.random() * 100 <= 2.0) drops.add(new ItemStack(Material.COAL));
        if (Math.random() * 100 <= 2.0) drops.add(new ItemStack(Material.LAPIS_LAZULI));
        if (Math.random() * 100 <= 0.5) drops.add(new ItemStack(Material.REDSTONE));

        return drops.isEmpty() ? null : drops;
    }
}