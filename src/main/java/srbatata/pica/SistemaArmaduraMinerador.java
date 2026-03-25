package srbatata.pica;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SistemaArmaduraMinerador implements Listener {

    private final Pica plugin;
    private final NamespacedKey keyMinerador;

    public SistemaArmaduraMinerador(Pica plugin) {
        this.plugin = plugin;
        this.keyMinerador = new NamespacedKey(plugin, "peca_minerador");
        registrarReceitas();
        iniciarLoopDeBonus();
    }

    // ==========================================
    // 1. CRIAR AS 4 RECEITAS (Couro + Diamante)
    // ==========================================
    private void registrarReceitas() {
        criarReceita("capacete_minerador", Material.LEATHER_HELMET, "§e§lCapacete do Minerador");
        criarReceita("peitoral_minerador", Material.LEATHER_CHESTPLATE, "§6§lTraje do Minerador");
        criarReceita("calca_minerador", Material.LEATHER_LEGGINGS, "§6§lCalças do Minerador");
        criarReceita("bota_minerador", Material.LEATHER_BOOTS, "§6§lBotas do Minerador");
    }

    private void criarReceita(String chave, Material materialBase, String nome) {
        ItemStack item = new ItemStack(materialBase);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nome);

            // Criamos a Lore base para todas as peças
            List<String> lore = new ArrayList<>(Arrays.asList(
                    "§7Reforçado com diamante.",
                    "",
                    "§eBônus de Conjunto:",
                    "§7Cada peça equipada concede",
                    "§b+1 Nível de Escavação Rápida!"
            ));

            // Se for o capacete, adicionamos a informação da Visão Noturna!
            if (materialBase == Material.LEATHER_HELMET) {
                lore.add("");
                lore.add("§eBônus do Capacete:");
                lore.add("§bVisão Noturna");
            }

            meta.setLore(lore);

            // ==========================================
            // LÓGICA DO BRILHO (GLINT) FALSO
            // ==========================================
            // 1. Adicionamos Inquebrável 1 (DURABILITY) ignorando restrições de item (true)
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);

            // 2. Escondemos a linha que diz "Inquebrável I" da descrição do item
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            // ==========================================

            // Marca o item como peça do set de minerador
            meta.getPersistentDataContainer().set(keyMinerador, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }

        ShapelessRecipe receita = new ShapelessRecipe(new NamespacedKey(plugin, chave), item);
        receita.addIngredient(materialBase);
        receita.addIngredient(Material.DIAMOND);

        Bukkit.addRecipe(receita);
    }

    // ==========================================
    // 2. LÓGICA DO SET BONUS E LUZ (Checagem em tempo real)
    // ==========================================
    private void iniciarLoopDeBonus() {
        // Roda a cada 1 segundo (20 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            for (Player player : Bukkit.getOnlinePlayers()) {
                int pecasEquipadas = 0;

                // Checa cada slot de armadura do jogador
                ItemStack capacete = player.getInventory().getHelmet();
                ItemStack peitoral = player.getInventory().getChestplate();
                ItemStack calca = player.getInventory().getLeggings();
                ItemStack bota = player.getInventory().getBoots();

                // Separamos a variável do capacete para saber se damos a Visão Noturna
                boolean temCapaceteMinerador = ehPecaMinerador(capacete);

                if (temCapaceteMinerador) pecasEquipadas++;
                if (ehPecaMinerador(peitoral)) pecasEquipadas++;
                if (ehPecaMinerador(calca)) pecasEquipadas++;
                if (ehPecaMinerador(bota)) pecasEquipadas++;

                // --- LÓGICA DO HASTE (Escavação Rápida) ---
                if (pecasEquipadas > 0) {
                    // O Amplificador da poção começa no 0 (Nível 1). Então 1 peça = 0, 4 peças = 3.
                    int nivelHaste = pecasEquipadas - 1;
                    // Escondemos partículas (false, false, false) para não poluir a tela
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, nivelHaste, false, false, false));
                } else {
                    // Se não tem peças, removemos o efeito (apenas se for do nosso plugin, com menos de 3s)
                    PotionEffect efeitoAtual = player.getPotionEffect(PotionEffectType.HASTE);
                    if (efeitoAtual != null && efeitoAtual.getDuration() <= 60) {
                        player.removePotionEffect(PotionEffectType.HASTE);
                    }
                }

                // --- LÓGICA DA VISÃO NOTURNA ---
                if (temCapaceteMinerador) {
                    // 300 ticks (15 segundos) para a tela não piscar
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false, false));
                } else {
                    PotionEffect efeitoVisao = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
                    if (efeitoVisao != null && efeitoVisao.getDuration() <= 300) {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    }
                }
            }
        }, 0L, 20L);
    }

    private boolean ehPecaMinerador(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyMinerador, PersistentDataType.BYTE);
    }
}