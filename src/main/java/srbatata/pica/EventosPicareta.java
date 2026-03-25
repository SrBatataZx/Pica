package srbatata.pica;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class EventosPicareta implements Listener {

    private final Pica plugin;
    private final NamespacedKey keyBlocos;
    private final NamespacedKey keyLixeira;

    public EventosPicareta(Pica plugin) {
        this.plugin = plugin;
        this.keyBlocos = new NamespacedKey(plugin, "blocos_quebrados");
        this.keyLixeira = new NamespacedKey(plugin, "modo_lixeira");
    }

    // ==========================================
    // EVENTO 1: ALTERNAR MODO LIXEIRA (CLIQUE DIREITO)
    // ==========================================
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Ignora o evento se for da mão secundária (off-hand)
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Action action = event.getAction();

        // Verifica se clicou com o botão direito
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType() != Material.AIR && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();

                // Verifica se é a picareta correta
                if (meta.getPersistentDataContainer().has(keyLixeira, PersistentDataType.INTEGER)) {
                    int estadoLixeira = meta.getPersistentDataContainer().get(keyLixeira, PersistentDataType.INTEGER);

                    // Inverte o estado (0 para 1, ou 1 para 0)
                    int novoEstado = (estadoLixeira == 0) ? 1 : 0;
                    meta.getPersistentDataContainer().set(keyLixeira, PersistentDataType.INTEGER, novoEstado);

                    // Atualiza a Lore
                    List<String> lore = meta.getLore();
                    if (lore != null && lore.size() >= 5) {
                        lore.set(2, novoEstado == 1 ? "§fModo Lixeira: §aAtivado" : "§fModo Lixeira: §cDesativado");
                        meta.setLore(lore);
                    }

                    item.setItemMeta(meta);

                    // Envia a mensagem e toca o som correspondente
                    if (novoEstado == 1) {
                        player.sendMessage("§aModo Lixeira ATIVADO!");
                        // Toca um som de "Pling" agudo (volume 1.0, pitch 2.0)
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    } else {
                        player.sendMessage("§cModo Lixeira DESATIVADO!");
                        // Toca o mesmo som, mas grave (volume 1.0, pitch 0.5)
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
                    }
                }
            }
        }
    }

    // ==========================================
    // EVENTO 2: QUEBRAR BLOCO (EVOLUÇÃO E COLETA)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ItemStack itemHand = player.getInventory().getItemInMainHand();

        if (itemHand.getType() == Material.AIR || !itemHand.hasItemMeta()) return;

        ItemMeta meta = itemHand.getItemMeta();

        if (meta != null && meta.getPersistentDataContainer().has(keyBlocos, PersistentDataType.INTEGER)) {

            // Lógica de drops personalizados e Telecinese
            if (plugin.getConfig().getBoolean("sistema_coleta_ativa", true)) {

                // Cancela o drop natural dos itens no chão
                event.setDropItems(false);

                // Pega os itens que o bloco iria dropar naturalmente (considera Fortuna)
                List<ItemStack> drops = new ArrayList<>(event.getBlock().getDrops(itemHand));

                // Adiciona nossos drops especiais (Diamante, Ouro, etc)
                List<ItemStack> dropsEspeciais = gerarDropsEspeciais();
                if (dropsEspeciais != null) {
                    drops.addAll(dropsEspeciais);
                }

                // Tenta adicionar tudo direto no inventário
                HashMap<Integer, ItemStack> sobrou = player.getInventory().addItem(drops.toArray(new ItemStack[0]));

                // Se o inventário estiver cheio (sobrou itens)
                if (!sobrou.isEmpty()) {
                    int estadoLixeira = meta.getPersistentDataContainer().getOrDefault(keyLixeira, PersistentDataType.INTEGER, 0);

                    if (estadoLixeira == 0) {
                        // Lixeira DESATIVADA: Dropa no chão o que não coube
                        Location loc = event.getBlock().getLocation();
                        for (ItemStack item : sobrou.values()) {
                            loc.getWorld().dropItemNaturally(loc, item);
                        }
                    }
                    // Se a lixeira for 1 (ATIVADA), não fazemos nada. Os itens são simplesmente perdidos/deletados.
                }
            } else {
                // Se o OP desligou o sistema, mantemos apenas o drop especial caindo no chão
                Location loc = event.getBlock().getLocation();
                List<ItemStack> especiais = gerarDropsEspeciais();
                if (especiais != null) {
                    for (ItemStack e : especiais) loc.getWorld().dropItemNaturally(loc, e);
                }
            }

            // ==========================================
            // LÓGICA DE PROGRESSO (MANTIDA)
            // ==========================================
            Material tipoBloco = event.getBlock().getType();
            if (!ehPedra(tipoBloco)) return;

            int blocosTotais = meta.getPersistentDataContainer().get(keyBlocos, PersistentDataType.INTEGER);
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
                lore.set(4, gerarBarraProgresso(tier, progressoNoTier, metaAtual)); // Agora índice 4
                meta.setLore(lore);
            }

            itemHand.setItemMeta(meta);

            tentarEncantar(itemHand, player);
            evoluirPicareta(itemHand, tier, player);
        }
    }
    // --- MÉTODOS AUXILIARES ---

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
        Material atual = picareta.getType();
        Material novoMaterial = atual;
        switch (tier) {
            case 1: novoMaterial = Material.STONE_PICKAXE; break;
            case 2: novoMaterial = Material.IRON_PICKAXE; break;
            case 3: novoMaterial = Material.GOLDEN_PICKAXE; break;
            case 4: novoMaterial = Material.DIAMOND_PICKAXE; break;
            case 5: novoMaterial = Material.NETHERITE_PICKAXE; break;
        }
        if (atual != novoMaterial) {
            picareta.setType(novoMaterial);
            player.sendMessage("§a🎉 Incrível! Sua picareta evoluiu de material!");
        }
    }

    private void tentarEncantar(ItemStack picareta, Player player) {
        if (Math.random() * 100 <= 0.5) {
            int nivelAtual = picareta.getEnchantmentLevel(Enchantment.EFFICIENCY);
            if (nivelAtual < 5) {
                picareta.addUnsafeEnchantment(Enchantment.EFFICIENCY, nivelAtual + 1);
                player.sendMessage("§b✨ Sua picareta ficou mais rápida! (Eficiência " + (nivelAtual + 1) + ")");
            }
        }
    }

    private List<ItemStack> gerarDropsEspeciais() {
        List<ItemStack> drops = new ArrayList<>();

        // Sorteia de forma independente para cada minério. Note que não tem "else"
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