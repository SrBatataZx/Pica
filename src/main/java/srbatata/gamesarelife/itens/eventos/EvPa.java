package srbatata.gamesarelife.itens.eventos; // Mude para seu pacote

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.util.ConfigHolder;
import srbatata.gamesarelife.util.FiltroHolder;
import srbatata.gamesarelife.core.Principal;
import srbatata.gamesarelife.sistemas.SistemaTerrenos; // IMPORTANTE

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class EvPa implements Listener {

    private final Principal plugin;

    private final NamespacedKey keyBlocos;
    private final NamespacedKey keyLixeiraPa;
    private final NamespacedKey keyFiltroPa;
    private final NamespacedKey keyBlocosAntiga;

    // NOVO: Metadado para evitar exploit de colocar bloco
    private final String META_COLOCADO = "colocado_pelo_jogador_pa";

    public EvPa(Principal plugin) {
        this.plugin = plugin;
        this.keyBlocos = new NamespacedKey(plugin, "blocos_quebrados_pa");
        this.keyLixeiraPa = new NamespacedKey(plugin, "modo_lixeira_pa");
        this.keyFiltroPa = new NamespacedKey(plugin, "lixeira_filtro_pa");

        this.keyBlocosAntiga = NamespacedKey.fromString("pica:blocos_quebrados_pa");
    }

    private void converterPaAntiga(ItemStack item, ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(keyBlocosAntiga, PersistentDataType.INTEGER)) {
            pdc.set(keyBlocos, PersistentDataType.INTEGER, pdc.get(keyBlocosAntiga, PersistentDataType.INTEGER));
            pdc.remove(keyBlocosAntiga);
            item.setItemMeta(meta);
        }
    }

    // ==========================================
    // PREVENÇÃO DE EXPLOIT: Etiqueta blocos colocados
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (ehBlocoDeEscavacao(block.getType())) {
            block.setMetadata(META_COLOCADO, new FixedMetadataValue(plugin, true));
        }
    }

    // ==========================================
    // EVENTO 1: ABRIR MENU DE CONFIGURAÇÃO (SHIFT+CLICK)
    // ==========================================
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType() != Material.AIR && item.getType().name().endsWith("_SHOVEL") && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                converterPaAntiga(item, meta);

                if (meta.getPersistentDataContainer().has(keyLixeiraPa, PersistentDataType.INTEGER)) {
                    if (player.isSneaking()) {
                        abrirMenuConfiguracao(player, item);
                    }
                }
            }
        }
    }

    // ==========================================
    // EVENTO 2: LÓGICA DE CLIQUES NOS MENUS
    // ==========================================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        Player player = (Player) event.getWhoClicked();

        ItemStack pa = player.getInventory().getItemInMainHand();
        if (pa.getType() == Material.AIR || !pa.getType().name().endsWith("_SHOVEL") || !pa.hasItemMeta()) return;

        ItemMeta meta = pa.getItemMeta();
        if (!meta.getPersistentDataContainer().has(keyLixeiraPa, PersistentDataType.INTEGER)) return;

        if (topInv.getHolder() instanceof ConfigHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInv)) return;

            if (event.getSlot() == 11) {
                int estadoLixeira = meta.getPersistentDataContainer().getOrDefault(keyLixeiraPa, PersistentDataType.INTEGER, 0);
                int novoEstado = (estadoLixeira == 0) ? 1 : 0;
                meta.getPersistentDataContainer().set(keyLixeiraPa, PersistentDataType.INTEGER, novoEstado);

                List<String> lore = meta.getLore();
                if (lore != null) {
                    for (int i = 0; i < lore.size(); i++) {
                        if (lore.get(i).contains("Modo Lixeira:")) {
                            lore.set(i, novoEstado == 1 ? "§fModo Lixeira: §aAtivado" : "§fModo Lixeira: §cDesativado");
                            break;
                        }
                    }
                    meta.setLore(lore);
                }

                pa.setItemMeta(meta);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, novoEstado == 1 ? 2.0f : 0.5f);
                abrirMenuConfiguracao(player, pa);
            }
            else if (event.getSlot() == 15) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                abrirMenuFiltro(player, pa);
            }
            return;
        }

        if (topInv.getHolder() instanceof FiltroHolder) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            String filtroAtual = meta.getPersistentDataContainer().getOrDefault(keyFiltroPa, PersistentDataType.STRING, "");
            List<String> listaFiltro = new ArrayList<>(Arrays.asList(filtroAtual.split(",")));
            listaFiltro.removeIf(String::isEmpty);

            Material tipoBloco = clickedItem.getType();
            boolean modificou = false;

            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                if (!listaFiltro.contains(tipoBloco.name()) && tipoBloco.isItem()) {
                    listaFiltro.add(tipoBloco.name());
                    player.sendMessage("§a[+] §fAdicionado §e" + tipoBloco.name() + " §fao filtro da pá.");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
                    modificou = true;
                } else if (listaFiltro.contains(tipoBloco.name())) {
                    player.sendMessage("§cEsse item já está no filtro!");
                }
            } else if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInv)) {
                if (listaFiltro.contains(tipoBloco.name())) {
                    listaFiltro.remove(tipoBloco.name());
                    player.sendMessage("§c[-] §fRemovido §e" + tipoBloco.name() + " §fdo filtro da pá.");
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    modificou = true;
                }
            }

            if (modificou) {
                meta.getPersistentDataContainer().set(keyFiltroPa, PersistentDataType.STRING, String.join(",", listaFiltro));
                pa.setItemMeta(meta);
                Bukkit.getScheduler().runTask(plugin, () -> abrirMenuFiltro(player, pa));
            }
        }
    }

    private void abrirMenuConfiguracao(Player player, ItemStack pa) {
        Inventory gui = Bukkit.createInventory(new ConfigHolder(), 27, Component.text("§8Configurações da Pá"));
        ItemMeta meta = pa.getItemMeta();

        int estadoLixeira = meta.getPersistentDataContainer().getOrDefault(keyLixeiraPa, PersistentDataType.INTEGER, 0);

        ItemStack toggleLixeira = new ItemStack(estadoLixeira == 1 ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta toggleMeta = toggleLixeira.getItemMeta();
        toggleMeta.setDisplayName(estadoLixeira == 1 ? "§aModo Lixeira: ATIVADO" : "§cModo Lixeira: DESATIVADO");
        toggleMeta.setLore(List.of("§7Clique para alternar o status."));
        toggleLixeira.setItemMeta(toggleMeta);
        gui.setItem(11, toggleLixeira);

        ItemStack btnFiltro = new ItemStack(Material.HOPPER);
        ItemMeta filtroMeta = btnFiltro.getItemMeta();
        filtroMeta.setDisplayName("§eFiltro da Lixeira");
        filtroMeta.setLore(List.of("§7Clique para gerenciar os itens", "§7que serão descartados."));
        btnFiltro.setItemMeta(filtroMeta);
        gui.setItem(15, btnFiltro);

        player.openInventory(gui);
    }

    private void abrirMenuFiltro(Player player, ItemStack pa) {
        Inventory gui = Bukkit.createInventory(new FiltroHolder(), 27, Component.text("§8Filtro da Lixeira (Pá)"));
        ItemMeta meta = pa.getItemMeta();
        String filtroStr = meta.getPersistentDataContainer().getOrDefault(keyFiltroPa, PersistentDataType.STRING, "");

        if (!filtroStr.isEmpty()) {
            String[] materiais = filtroStr.split(",");
            for (String matName : materiais) {
                Material mat = Material.matchMaterial(matName);
                if (mat != null) {
                    ItemStack itemFiltro = new ItemStack(mat);
                    ItemMeta itemMeta = itemFiltro.getItemMeta();
                    itemMeta.setDisplayName("§cRemover " + mat.name());
                    itemMeta.setLore(List.of("§7Clique para remover", "§7item do filtro da pá."));
                    itemFiltro.setItemMeta(itemMeta);
                    gui.addItem(itemFiltro);
                }
            }
        }
        player.openInventory(gui);
    }

    // ==========================================
    // EVENTO 3: QUEBRAR BLOCO (LÓGICA DA LIXEIRA/PÁ)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack itemHand = player.getInventory().getItemInMainHand();
        Block block = event.getBlock();

        if (itemHand.getType() == Material.AIR || !itemHand.hasItemMeta()) return;

        ItemMeta meta = itemHand.getItemMeta();
        converterPaAntiga(itemHand, meta);

        if (meta != null && meta.getPersistentDataContainer().has(keyBlocos, PersistentDataType.INTEGER)) {

            Material tipoBloco = block.getType();
            if (!ehBlocoDeEscavacao(tipoBloco)) return;

            // [NOVIDADE]: Verificações de segurança
            boolean noTerreno = SistemaTerrenos.getInstance() != null && SistemaTerrenos.getInstance().isBlocoProtegido(block.getLocation());
            boolean blocoColocadoManual = block.hasMetadata(META_COLOCADO);

            // Variável que autoriza drops e level up
            boolean podeEvoluirEDropar = !noTerreno && !blocoColocadoManual;

            List<ItemStack> dropsParaEntregar = new ArrayList<>();

            // 1. SORTEIO DE TESOUROS
            if (podeEvoluirEDropar) {
                List<ItemStack> dropsEspeciais = gerarTesouros();
                if (dropsEspeciais != null) {
                    dropsParaEntregar.addAll(dropsEspeciais);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                }
            }

            // 2. SISTEMA DE COLETA E LIXEIRA (Continua funcionando normal)
            if (plugin.getConfig().getBoolean("sistema_coleta_ativa", true)) {
                event.setDropItems(false);
                dropsParaEntregar.addAll(event.getBlock().getDrops(itemHand));

                int estadoLixeira = meta.getPersistentDataContainer().getOrDefault(keyLixeiraPa, PersistentDataType.INTEGER, 0);
                String filtroStr = meta.getPersistentDataContainer().getOrDefault(keyFiltroPa, PersistentDataType.STRING, "");

                if (estadoLixeira == 1 && !filtroStr.isEmpty()) {
                    List<String> listaFiltro = Arrays.asList(filtroStr.split(","));
                    dropsParaEntregar.removeIf(item -> listaFiltro.contains(item.getType().name()));
                }

                HashMap<Integer, ItemStack> sobrou = player.getInventory().addItem(dropsParaEntregar.toArray(new ItemStack[0]));
                if (!sobrou.isEmpty()) {
                    Location loc = event.getBlock().getLocation();
                    for (ItemStack item : sobrou.values()) {
                        loc.getWorld().dropItemNaturally(loc, item);
                    }
                }
            } else {
                if (!dropsParaEntregar.isEmpty()) {
                    Location loc = event.getBlock().getLocation();
                    for (ItemStack item : dropsParaEntregar) {
                        loc.getWorld().dropItemNaturally(loc, item);
                    }
                }
            }

            // Limpa a memória para segurança
            block.removeMetadata(META_COLOCADO, plugin);

            // 3. PROGRESSÃO E EVOLUÇÃO
            if (podeEvoluirEDropar) {
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
                if (lore != null) {
                    for (int i = 0; i < lore.size(); i++) {
                        if (lore.get(i).contains("[") && lore.get(i).contains("]")) {
                            lore.set(i, gerarBarraProgresso(tier, progressoNoTier, metaAtual));
                            break;
                        }
                    }
                    meta.setLore(lore);
                }

                itemHand.setItemMeta(meta);
                tentarEncantar(itemHand, player);
                evoluirPa(itemHand, tier, player);
            }
        }
    }

    private boolean ehBlocoDeEscavacao(Material mat) {
        return mat == Material.DIRT || mat == Material.GRASS_BLOCK || mat == Material.SAND ||
                mat == Material.GRAVEL || mat == Material.COARSE_DIRT || mat == Material.PODZOL ||
                mat == Material.MYCELIUM || mat == Material.CLAY || mat == Material.SOUL_SAND ||
                mat == Material.SOUL_SOIL || mat == Material.RED_SAND || mat == Material.DIRT_PATH;
    }

    private String gerarBarraProgresso(int tier, int progressoAtual, int metaAtual) {
        if (tier >= 5) return "§8[§a||||||||||§8] §eMÁXIMO";
        int barrasPreenchidas = (progressoAtual * 10) / metaAtual;
        StringBuilder barra = new StringBuilder("§8[");
        for (int i = 0; i < 10; i++) barra.append(i < barrasPreenchidas ? "§a|" : "§7|");
        barra.append("§8] §e").append(progressoAtual).append("/").append(metaAtual);
        return barra.toString();
    }

    private void evoluirPa(ItemStack pa, int tier, Player player) {
        Material novoMaterial = switch (tier) {
            case 1 -> Material.STONE_SHOVEL;
            case 2 -> Material.IRON_SHOVEL;
            case 3 -> Material.GOLDEN_SHOVEL;
            case 4 -> Material.DIAMOND_SHOVEL;
            case 5 -> Material.NETHERITE_SHOVEL;
            default -> pa.getType();
        };

        if (pa.getType() != novoMaterial) {
            pa.setType(novoMaterial);
            player.sendMessage("§a🎉 Fantástico! Sua Pá evoluiu de material!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    private void tentarEncantar(ItemStack pa, Player player) {
        if (Math.random() * 100 <= 0.5) {
            int nivelAtual = pa.getEnchantmentLevel(Enchantment.EFFICIENCY);
            if (nivelAtual < 5) {
                pa.addUnsafeEnchantment(Enchantment.EFFICIENCY, nivelAtual + 1);
                player.sendMessage("§b✨ A sua Pá ficou mais rápida! (Eficiência " + (nivelAtual + 1) + ")");
            }
        }
    }

    private List<ItemStack> gerarTesouros() {
        double chance = Math.random() * 100;
        List<ItemStack> drops = new ArrayList<>();

        if (chance <= 3.0) drops.add(new ItemStack(Material.GOLD_NUGGET, 1));
        else if (chance <= 4.0) drops.add(new ItemStack(Material.IRON_NUGGET, 1));
        else if (chance <= 4.2) drops.add(new ItemStack(Material.BONE, 1));

        return drops.isEmpty() ? null : drops;
    }
}