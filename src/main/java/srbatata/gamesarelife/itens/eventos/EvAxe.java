package srbatata.gamesarelife.itens.eventos;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.core.Principal;

import java.util.*;

public class EvAxe implements Listener {

    private final Principal plugin;

    // CHAVES NOVAS (Para os machados criados daqui em diante)
    private final NamespacedKey keyBlocos;
    private final NamespacedKey keyLenhador;

    // CHAVES ANTIGAS (Usadas para conversão silenciosa)
    private final NamespacedKey keyBlocosAntiga;
    private final NamespacedKey keyLenhadorAntiga;

    private final Set<Block> blocosSendoQuebrados = new HashSet<>();

    public EvAxe(Principal plugin) {
        this.plugin = plugin;

        // Inicializa as chaves com o nome atual do plugin (GamesAreLife)
        this.keyBlocos = new NamespacedKey(plugin, "blocos_quebrados_machado");
        this.keyLenhador = new NamespacedKey(plugin, "modo_lenhador_machado");

        // Inicializa as chaves do plugin antigo para recuperar dados perdidos
        this.keyBlocosAntiga = NamespacedKey.fromString("pica:blocos_quebrados_machado");
        this.keyLenhadorAntiga = NamespacedKey.fromString("pica:modo_lenhador_machado");
    }

    // ==========================================
    // SISTEMA DE MIGRAÇÃO (NOVIDADE)
    // ==========================================
    private void converterMachadoAntigo(ItemStack item, ItemMeta meta) {
        boolean modificou = false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Se encontrar o modo lenhador antigo, converte
        if (pdc.has(keyLenhadorAntiga, PersistentDataType.INTEGER)) {
            pdc.set(keyLenhador, PersistentDataType.INTEGER, pdc.get(keyLenhadorAntiga, PersistentDataType.INTEGER));
            pdc.remove(keyLenhadorAntiga);
            modificou = true;
        }
        // Se encontrar o progresso antigo, converte
        if (pdc.has(keyBlocosAntiga, PersistentDataType.INTEGER)) {
            pdc.set(keyBlocos, PersistentDataType.INTEGER, pdc.get(keyBlocosAntiga, PersistentDataType.INTEGER));
            pdc.remove(keyBlocosAntiga);
            modificou = true;
        }

        if (modificou) {
            item.setItemMeta(meta);
        }
    }

    // ==========================================
    // ALTERNAR MODO LENHADOR (BOTÃO DIREITO)
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

                // Conversão Silenciosa
                converterMachadoAntigo(item, meta);

                if (meta.getPersistentDataContainer().has(keyLenhador, PersistentDataType.INTEGER)) {
                    int estadoLenhador = meta.getPersistentDataContainer().getOrDefault(keyLenhador, PersistentDataType.INTEGER, 0);
                    int novoEstado = (estadoLenhador == 0) ? 1 : 0;

                    meta.getPersistentDataContainer().set(keyLenhador, PersistentDataType.INTEGER, novoEstado);

                    List<String> lore = meta.getLore();
                    if (lore != null && lore.size() >= 5) {
                        lore.set(2, novoEstado == 1 ? "§fModo Lenhador: §aAtivado" : "§fModo Lenhador: §cDesativado");
                        meta.setLore(lore);
                    }

                    item.setItemMeta(meta);

                    if (novoEstado == 1) {
                        player.sendMessage("§aModo Lenhador ATIVADO! (Cuidado com suas construções)");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_AXE_SCRAPE, 1.0f, 2.0f);
                    } else {
                        player.sendMessage("§cModo Lenhador DESATIVADO!");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_AXE_SCRAPE, 1.0f, 0.5f);
                    }
                }
            }
        }
    }

    // ==========================================
    // QUEBRAR BLOCO / ÁRVORE E EVOLUIR
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        if (blocosSendoQuebrados.contains(event.getBlock())) return;

        Player player = event.getPlayer();
        ItemStack itemHand = player.getInventory().getItemInMainHand();
        Block blocoInicial = event.getBlock();

        if (itemHand.getType() == Material.AIR || !itemHand.hasItemMeta()) return;

        ItemMeta meta = itemHand.getItemMeta();

        // Conversão Silenciosa antes de usar
        converterMachadoAntigo(itemHand, meta);

        if (meta != null && meta.getPersistentDataContainer().has(keyBlocos, PersistentDataType.INTEGER)) {

            if (!ehMadeira(blocoInicial.getType())) return;

            event.setCancelled(true);

            int estadoLenhador = meta.getPersistentDataContainer().getOrDefault(keyLenhador, PersistentDataType.INTEGER, 0);
            List<Block> blocosParaQuebrar = new ArrayList<>();

            if (estadoLenhador == 1) {
                blocosParaQuebrar = buscarArvore(blocoInicial);
            } else {
                blocosParaQuebrar.add(blocoInicial);
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

                List<ItemStack> esp = gerarDropsEspeciais();
                if (esp != null) todosOsDrops.addAll(esp);

                b.setType(Material.AIR);
                blocosCortados++;
            }

            HashMap<Integer, ItemStack> sobrou = player.getInventory().addItem(todosOsDrops.toArray(new ItemStack[0]));
            if (!sobrou.isEmpty()) {
                Location loc = player.getLocation();
                for (ItemStack item : sobrou.values()) loc.getWorld().dropItemNaturally(loc, item);
            }

            // Lógica de progressão com chave nova
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
            if (lore != null && lore.size() >= 5) {
                lore.set(4, gerarBarraProgresso(tier, progressoNoTier, metaAtual));
                meta.setLore(lore);
            }
            itemHand.setItemMeta(meta);

            evoluirMachado(itemHand, tier, player);

            for (int i = 0; i < blocosCortados; i++) tentarEncantar(itemHand, player);
        }
    }

    // ==========================================
    // ALGORITMO DE MAPEAR ÁRVORES (TREE CAPITATOR)
    // ==========================================
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

                        if (!visitados.contains(vizinho) && ehMadeira(vizinho.getType())) {
                            visitados.add(vizinho);
                            fila.add(vizinho);
                        }
                    }
                }
            }
        }
        return arvore;
    }

    // --- MÉTODOS AUXILIARES ---

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

        // Uso de Switch Expressions (Java 21) para deixar o código limpo
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
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
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

        if (chance <= 1.0) {
            drops.add(new ItemStack(Material.EMERALD));
        } else if (chance <= 2.0) {
            drops.add(new ItemStack(Material.GOLDEN_APPLE));
        } else if (chance <= 25.0) {
            drops.add(new ItemStack(Material.APPLE));
        }

        return drops.isEmpty() ? null : drops;
    }
}