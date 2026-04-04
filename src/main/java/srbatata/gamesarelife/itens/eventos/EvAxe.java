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

import java.util.*;

public class EvAxe implements Listener {

    private final Principal plugin;

    // CHAVES NOVAS
    private final NamespacedKey keyBlocos;
    private final NamespacedKey keyLenhador;
    private final NamespacedKey keyLixeiraAxe;
    private final NamespacedKey keyFiltroAxe;

    // CHAVES ANTIGAS
    private final NamespacedKey keyBlocosAntiga;
    private final NamespacedKey keyLenhadorAntiga;

    private final Set<Block> blocosSendoQuebrados = new HashSet<>();

    // NOVO NOME para metadado do Machado
    private final String META_COLOCADO = "colocado_pelo_jogador_machado";

    public EvAxe(Principal plugin) {
        this.plugin = plugin;

        this.keyBlocos = new NamespacedKey(plugin, "blocos_quebrados_machado");
        this.keyLenhador = new NamespacedKey(plugin, "modo_lenhador_machado");
        this.keyLixeiraAxe = new NamespacedKey(plugin, "modo_lixeira_machado");
        this.keyFiltroAxe = new NamespacedKey(plugin, "lixeira_filtro_machado");

        this.keyBlocosAntiga = NamespacedKey.fromString("pica:blocos_quebrados_machado");
        this.keyLenhadorAntiga = NamespacedKey.fromString("pica:modo_lenhador_machado");
    }

    private void converterMachadoAntigo(ItemStack item, ItemMeta meta) {
        boolean modificou = false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(keyLenhadorAntiga, PersistentDataType.INTEGER)) {
            pdc.set(keyLenhador, PersistentDataType.INTEGER, pdc.get(keyLenhadorAntiga, PersistentDataType.INTEGER));
            pdc.remove(keyLenhadorAntiga);
            modificou = true;
        }
        if (pdc.has(keyBlocosAntiga, PersistentDataType.INTEGER)) {
            pdc.set(keyBlocos, PersistentDataType.INTEGER, pdc.get(keyBlocosAntiga, PersistentDataType.INTEGER));
            pdc.remove(keyBlocosAntiga);
            modificou = true;
        }

        if (modificou) {
            item.setItemMeta(meta);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (ehMadeira(block.getType())) {
            block.setMetadata(META_COLOCADO, new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType() != Material.AIR && item.getType().name().endsWith("_AXE") && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                converterMachadoAntigo(item, meta);

                if (meta.getPersistentDataContainer().has(keyLenhador, PersistentDataType.INTEGER) || meta.getPersistentDataContainer().has(keyLixeiraAxe, PersistentDataType.INTEGER)) {
                    if (player.isSneaking()) {
                        abrirMenuConfiguracao(player, item);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        Player player = (Player) event.getWhoClicked();

        ItemStack machado = player.getInventory().getItemInMainHand();
        if (machado.getType() == Material.AIR || !machado.getType().name().endsWith("_AXE") || !machado.hasItemMeta()) return;

        ItemMeta meta = machado.getItemMeta();

        if (topInv.getHolder() instanceof ConfigHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInv)) return;

            if (event.getSlot() == 11) {
                int estadoLenhador = meta.getPersistentDataContainer().getOrDefault(keyLenhador, PersistentDataType.INTEGER, 0);
                int novoEstado = (estadoLenhador == 0) ? 1 : 0;
                meta.getPersistentDataContainer().set(keyLenhador, PersistentDataType.INTEGER, novoEstado);

                List<String> lore = meta.getLore();
                if (lore != null) {
                    for (int i = 0; i < lore.size(); i++) {
                        if (lore.get(i).contains("Modo Lenhador:")) {
                            lore.set(i, novoEstado == 1 ? "§fModo Lenhador: §aAtivado" : "§fModo Lenhador: §cDesativado");
                            break;
                        }
                    }
                    meta.setLore(lore);
                }
                machado.setItemMeta(meta);
                player.playSound(player.getLocation(), Sound.ITEM_AXE_SCRAPE, 1.0f, novoEstado == 1 ? 2.0f : 0.5f);
                abrirMenuConfiguracao(player, machado);
            }
            else if (event.getSlot() == 13) {
                int estadoLixeira = meta.getPersistentDataContainer().getOrDefault(keyLixeiraAxe, PersistentDataType.INTEGER, 0);
                int novoEstado = (estadoLixeira == 0) ? 1 : 0;
                meta.getPersistentDataContainer().set(keyLixeiraAxe, PersistentDataType.INTEGER, novoEstado);

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
                machado.setItemMeta(meta);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, novoEstado == 1 ? 2.0f : 0.5f);
                abrirMenuConfiguracao(player, machado);
            }
            else if (event.getSlot() == 15) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                abrirMenuFiltro(player, machado);
            }
            return;
        }

        if (topInv.getHolder() instanceof FiltroHolder) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            String filtroAtual = meta.getPersistentDataContainer().getOrDefault(keyFiltroAxe, PersistentDataType.STRING, "");
            List<String> listaFiltro = new ArrayList<>(Arrays.asList(filtroAtual.split(",")));
            listaFiltro.removeIf(String::isEmpty);

            Material tipoBloco = clickedItem.getType();
            boolean modificou = false;

            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                if (!listaFiltro.contains(tipoBloco.name()) && tipoBloco.isItem()) {
                    listaFiltro.add(tipoBloco.name());
                    player.sendMessage("§a[+] §fAdicionado §e" + tipoBloco.name() + " §fao filtro do machado.");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
                    modificou = true;
                } else if (listaFiltro.contains(tipoBloco.name())) {
                    player.sendMessage("§cEsse item já está no filtro!");
                }
            } else if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInv)) {
                if (listaFiltro.contains(tipoBloco.name())) {
                    listaFiltro.remove(tipoBloco.name());
                    player.sendMessage("§c[-] §fRemovido §e" + tipoBloco.name() + " §fdo filtro do machado.");
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    modificou = true;
                }
            }

            if (modificou) {
                meta.getPersistentDataContainer().set(keyFiltroAxe, PersistentDataType.STRING, String.join(",", listaFiltro));
                machado.setItemMeta(meta);
                Bukkit.getScheduler().runTask(plugin, () -> abrirMenuFiltro(player, machado));
            }
        }
    }

    private void abrirMenuConfiguracao(Player player, ItemStack machado) {
        Inventory gui = Bukkit.createInventory(new ConfigHolder(), 27, Component.text("§8Configurações do Machado"));
        ItemMeta meta = machado.getItemMeta();

        int estadoLenhador = meta.getPersistentDataContainer().getOrDefault(keyLenhador, PersistentDataType.INTEGER, 0);
        int estadoLixeira = meta.getPersistentDataContainer().getOrDefault(keyLixeiraAxe, PersistentDataType.INTEGER, 0);

        ItemStack toggleLenhador = new ItemStack(estadoLenhador == 1 ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta lenhadorMeta = toggleLenhador.getItemMeta();
        lenhadorMeta.setDisplayName(estadoLenhador == 1 ? "§aModo Lenhador: ATIVADO" : "§cModo Lenhador: DESATIVADO");
        lenhadorMeta.setLore(List.of("§7Clique para alternar a", "§7quebra de árvores completas."));
        toggleLenhador.setItemMeta(lenhadorMeta);
        gui.setItem(11, toggleLenhador);

        ItemStack toggleLixeira = new ItemStack(estadoLixeira == 1 ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta lixeiraMeta = toggleLixeira.getItemMeta();
        lixeiraMeta.setDisplayName(estadoLixeira == 1 ? "§aModo Lixeira: ATIVADO" : "§cModo Lixeira: DESATIVADO");
        lixeiraMeta.setLore(List.of("§7Clique para alternar o status."));
        toggleLixeira.setItemMeta(lixeiraMeta);
        gui.setItem(13, toggleLixeira);

        ItemStack btnFiltro = new ItemStack(Material.HOPPER);
        ItemMeta filtroMeta = btnFiltro.getItemMeta();
        filtroMeta.setDisplayName("§eFiltro da Lixeira");
        filtroMeta.setLore(List.of("§7Clique para gerenciar os itens", "§7que serão descartados."));
        btnFiltro.setItemMeta(filtroMeta);
        gui.setItem(15, btnFiltro);

        player.openInventory(gui);
    }

    private void abrirMenuFiltro(Player player, ItemStack machado) {
        Inventory gui = Bukkit.createInventory(new FiltroHolder(), 27, Component.text("§8Filtro da Lixeira (Machado)"));
        ItemMeta meta = machado.getItemMeta();
        String filtroStr = meta.getPersistentDataContainer().getOrDefault(keyFiltroAxe, PersistentDataType.STRING, "");

        if (!filtroStr.isEmpty()) {
            String[] materiais = filtroStr.split(",");
            for (String matName : materiais) {
                Material mat = Material.matchMaterial(matName);
                if (mat != null) {
                    ItemStack itemFiltro = new ItemStack(mat);
                    ItemMeta itemMeta = itemFiltro.getItemMeta();
                    itemMeta.setDisplayName("§cRemover " + mat.name());
                    itemMeta.setLore(List.of("§7Clique para remover", "§7item do filtro do machado."));
                    itemFiltro.setItemMeta(itemMeta);
                    gui.addItem(itemFiltro);
                }
            }
        }
        player.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (blocosSendoQuebrados.contains(event.getBlock())) return;

        Player player = event.getPlayer();
        ItemStack itemHand = player.getInventory().getItemInMainHand();
        Block blocoInicial = event.getBlock();

        if (itemHand.getType() == Material.AIR || !itemHand.hasItemMeta()) return;

        ItemMeta meta = itemHand.getItemMeta();
        converterMachadoAntigo(itemHand, meta);

        if (meta != null && meta.getPersistentDataContainer().has(keyBlocos, PersistentDataType.INTEGER)) {
            if (!ehMadeira(blocoInicial.getType())) return;

            event.setCancelled(true);

            int estadoLenhador = meta.getPersistentDataContainer().getOrDefault(keyLenhador, PersistentDataType.INTEGER, 0);

            // [NOVIDADE]: Verificações de segurança
            boolean noTerreno = SistemaTerrenos.getInstance() != null && SistemaTerrenos.getInstance().isBlocoProtegido(blocoInicial.getLocation());
            boolean blocoFoiColocado = blocoInicial.hasMetadata(META_COLOCADO);

            // Permissão para drops de maçã/esmeralda e level up
            boolean podeEvoluirEDropar = !noTerreno && !blocoFoiColocado;

            List<Block> blocosParaQuebrar;
            if (estadoLenhador == 1 && !blocoFoiColocado) {
                blocosParaQuebrar = buscarArvore(blocoInicial);
            } else {
                blocosParaQuebrar = new ArrayList<>(List.of(blocoInicial));
            }

            List<ItemStack> todosOsDrops = new ArrayList<>();
            int blocosCortados = 0;

            for (Block b : blocosParaQuebrar) {
                if (!b.equals(blocoInicial)) {
                    BlockBreakEvent fakeEvent = new BlockBreakEvent(b, player);
                    blocosSendoQuebrados.add(b);
                    Bukkit.getPluginManager().callEvent(fakeEvent);
                    blocosSendoQuebrados.remove(b);

                    if (fakeEvent.isCancelled()) continue;
                }

                todosOsDrops.addAll(b.getDrops(itemHand));

                // 1. SORTEIO DE DROPS EXTRAS (Somente na natureza real)
                if (podeEvoluirEDropar) {
                    List<ItemStack> esp = gerarDropsEspeciais();
                    if (esp != null) todosOsDrops.addAll(esp);
                }

                b.removeMetadata(META_COLOCADO, plugin); // Limpa da memória!
                b.setType(Material.AIR);
                blocosCortados++;
            }

            // 2. SISTEMA DE FILTRO/LIXEIRA E COLETA (Continua funcionando sempre)
            if (plugin.getConfig().getBoolean("sistema_coleta_ativa", true)) {
                int estadoLixeira = meta.getPersistentDataContainer().getOrDefault(keyLixeiraAxe, PersistentDataType.INTEGER, 0);
                String filtroStr = meta.getPersistentDataContainer().getOrDefault(keyFiltroAxe, PersistentDataType.STRING, "");

                if (estadoLixeira == 1 && !filtroStr.isEmpty()) {
                    List<String> listaFiltro = Arrays.asList(filtroStr.split(","));
                    todosOsDrops.removeIf(item -> listaFiltro.contains(item.getType().name()));
                }

                HashMap<Integer, ItemStack> sobrou = player.getInventory().addItem(todosOsDrops.toArray(new ItemStack[0]));
                if (!sobrou.isEmpty()) {
                    Location loc = player.getLocation();
                    for (ItemStack item : sobrou.values()) loc.getWorld().dropItemNaturally(loc, item);
                }
            } else {
                Location loc = player.getLocation();
                for (ItemStack item : todosOsDrops) loc.getWorld().dropItemNaturally(loc, item);
            }

            // 3. PROGRESSÃO E EVOLUÇÃO (Somente na natureza real)
            if (podeEvoluirEDropar && blocosCortados > 0) {
                int blocosTotais = meta.getPersistentDataContainer().getOrDefault(keyBlocos, PersistentDataType.INTEGER, 0);
                blocosTotais += blocosCortados;
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

                evoluirMachado(itemHand, tier, player);
                for (int i = 0; i < blocosCortados; i++) tentarEncantar(itemHand, player);
            }
        }
    }

    private List<Block> buscarArvore(Block blocoInicial) {
        List<Block> arvore = new ArrayList<>();
        Set<Block> visitados = new HashSet<>();
        Queue<Block> fila = new LinkedList<>();

        fila.add(blocoInicial);
        visitados.add(blocoInicial);

        int maxBlocos = 150;

        while (!fila.isEmpty() && arvore.size() < maxBlocos) {
            Block atual = fila.poll();
            arvore.add(atual);

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block vizinho = atual.getRelative(x, y, z);

                        if (!visitados.contains(vizinho) && ehMadeira(vizinho.getType()) && !vizinho.hasMetadata(META_COLOCADO)) {
                            visitados.add(vizinho);
                            fila.add(vizinho);
                        }
                    }
                }
            }
        }
        return arvore;
    }

    private boolean ehMadeira(Material mat) {
        String nome = mat.name();
        return nome.endsWith("_LOG") || nome.endsWith("_WOOD") || nome.endsWith("_STEM");
    }

    private String gerarBarraProgresso(int tier, int progressoAtual, int metaAtual) {
        if (tier >= 5) return "§8[§a||||||||||§8] §eMÁXIMO";
        int barrasPreenchidas = (progressoAtual * 10) / metaAtual;
        StringBuilder barra = new StringBuilder("§8[");
        for (int i = 0; i < 10; i++) barra.append(i < barrasPreenchidas ? "§a|" : "§7|");
        barra.append("§8] §e").append(progressoAtual).append("/").append(metaAtual);
        return barra.toString();
    }

    private void evoluirMachado(ItemStack machado, int tier, Player player) {
        Material atual = machado.getType();
        Material novoMaterial = switch (tier) {
            case 1 -> Material.STONE_AXE;
            case 2 -> Material.IRON_AXE;
            case 3 -> Material.GOLDEN_AXE;
            case 4 -> Material.DIAMOND_AXE;
            case 5 -> Material.NETHERITE_AXE;
            default -> atual;
        };

        if (atual != novoMaterial) {
            machado.setType(novoMaterial);
            player.sendMessage("§a🎉 Incrível! Seu machado evoluiu de material!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    private void tentarEncantar(ItemStack machado, Player player) {
        if (Math.random() * 100 <= 0.2) {
            int nivelAtual = machado.getEnchantmentLevel(Enchantment.EFFICIENCY);
            if (nivelAtual < 5) {
                machado.addUnsafeEnchantment(Enchantment.EFFICIENCY, nivelAtual + 1);
                player.sendMessage("§b✨ Seu machado ficou mais rápido! (Eficiência " + (nivelAtual + 1) + ")");
            }
        }
    }

    private List<ItemStack> gerarDropsEspeciais() {
        double chance = Math.random() * 100;
        List<ItemStack> drops = new ArrayList<>();

        if (chance <= 1.0) drops.add(new ItemStack(Material.EMERALD));
        else if (chance <= 2.0) drops.add(new ItemStack(Material.GOLDEN_APPLE));
        else if (chance <= 25.0) drops.add(new ItemStack(Material.APPLE));

        return drops.isEmpty() ? null : drops;
    }
}