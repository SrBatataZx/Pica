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

public class EvPa implements Listener {

    private final Principal plugin;

    private final NamespacedKey keyBlocos;
    private final NamespacedKey keyLixeiraPa;
    private final NamespacedKey keyFiltroPa;
    private final NamespacedKey keyBlocosAntiga;

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
                        abrirMenuFiltro(player, item);
                        return;
                    }

                    int estadoLixeira = meta.getPersistentDataContainer().getOrDefault(keyLixeiraPa, PersistentDataType.INTEGER, 0);
                    int novoEstado = (estadoLixeira == 0) ? 1 : 0;

                    meta.getPersistentDataContainer().set(keyLixeiraPa, PersistentDataType.INTEGER, novoEstado);

                    // CORREÇÃO: Busca Dinâmica na Lore
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

                    item.setItemMeta(meta);

                    if (novoEstado == 1) {
                        player.sendMessage("§aModo Lixeira da Pá ATIVADO!");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    } else {
                        player.sendMessage("§cModo Lixeira da Pá DESATIVADO!");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
                    }
                }
            }
        }
    }

    // ... (Os eventos onInventoryClick e abrirMenuFiltro permanecem inalterados)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FiltroHolder) {
            Player player = (Player) event.getWhoClicked();
            ItemStack pa = player.getInventory().getItemInMainHand();
            if (pa.getType() == Material.AIR || !pa.getType().name().endsWith("_SHOVEL") || !pa.hasItemMeta()) return;
            ItemMeta meta = pa.getItemMeta();
            if (!meta.getPersistentDataContainer().has(keyLixeiraPa, PersistentDataType.INTEGER)) return;
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
            String filtroAtual = meta.getPersistentDataContainer().getOrDefault(keyFiltroPa, PersistentDataType.STRING, "");
            List<String> listaFiltro = new ArrayList<>(Arrays.asList(filtroAtual.split(",")));
            listaFiltro.removeIf(String::isEmpty);
            Material tipoBloco = clickedItem.getType();
            boolean modificou = false;

            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                if (!listaFiltro.contains(tipoBloco.name())) {
                    listaFiltro.add(tipoBloco.name());
                    player.sendMessage("§a[+] §fAdicionado §e" + tipoBloco.name() + " §fao filtro da pá.");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
                    modificou = true;
                } else {
                    player.sendMessage("§cEsse item já está no filtro!");
                }
            } else if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack itemHand = player.getInventory().getItemInMainHand();

        if (itemHand.getType() == Material.AIR || !itemHand.hasItemMeta()) return;

        ItemMeta meta = itemHand.getItemMeta();
        converterPaAntiga(itemHand, meta);

        if (meta != null && meta.getPersistentDataContainer().has(keyBlocos, PersistentDataType.INTEGER)) {

            Material tipoBloco = event.getBlock().getType();
            if (!ehBlocoDeEscavacao(tipoBloco)) return;

            // CORREÇÃO: Gerar tesouros independente da auto-coleta!
            List<ItemStack> dropsParaEntregar = new ArrayList<>();
            List<ItemStack> dropsEspeciais = gerarTesouros();

            if (dropsEspeciais != null) {
                dropsParaEntregar.addAll(dropsEspeciais);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            }

            // --- LÓGICA DE COLETA, PEPITAS E LIXEIRA ---
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
                // Dropa os tesouros no chão se a coleta estiver off
                if (!dropsParaEntregar.isEmpty()) {
                    Location loc = event.getBlock().getLocation();
                    for (ItemStack item : dropsParaEntregar) {
                        loc.getWorld().dropItemNaturally(loc, item);
                    }
                }
            }

            // --- LÓGICA DE PROGRESSÃO ---
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

            // CORREÇÃO: Busca Dinâmica na Lore
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (int i = 0; i < lore.size(); i++) {
                    // Encontra a linha que tem a barrinha pelo colchete
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