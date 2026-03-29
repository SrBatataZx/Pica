package srbatata.gamesarelife;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import srbatata.gamesarelife.core.Principal;

import java.util.Arrays;

public class SistemaArmadurasCustomizadas implements Listener {

    private final Principal plugin;
    private final SistemaTerrenos sistemaTerrenos;

    public SistemaArmadurasCustomizadas(Principal plugin, SistemaTerrenos terrenos) {
        this.plugin = plugin;
        this.sistemaTerrenos = terrenos;
        registrarReceitas();
        iniciarLoopEfeitos();
    }

    private void aplicarBrilho(ItemMeta meta) {
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    // --- CRIAÇÃO DOS ITENS ---

//    public ItemStack getPeitoralExplorador() {
//        ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
//        ItemMeta meta = item.getItemMeta();
//        meta.setDisplayName("§e§lPeitoral do Explorador");
//        meta.setLore(Arrays.asList("§7Habilidade:", "§fShift + Clique Esquerdo para §aAlternar Voo", "§8(Apenas em seus terrenos!)"));
//        aplicarBrilho(meta);
//        item.setItemMeta(meta);
//        return item;
//    }

    public ItemStack getCalcaCorredor() {
        ItemStack item = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lCalça do Corredor");
        meta.setLore(Arrays.asList("§7Efeito Passivo:", "§f+50% de Velocidade de Movimento"));

        AttributeModifier modifier = new AttributeModifier(
                new NamespacedKey(plugin, "velocidade_corredor"),
                0.05,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.LEGS
        );
        meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, modifier);
        aplicarBrilho(meta);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getBotaCoelho() {
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lBota do Coelho");
        meta.setLore(Arrays.asList("§7Efeitos Passivos:", "§fSuper Pulo II e Imunidade a Queda"));
        aplicarBrilho(meta);
        item.setItemMeta(meta);
        return item;
    }

    // --- REGISTRO DE RECEITAS ---

    private void registrarReceitas() {
//        // Peitoral
//        ShapedRecipe rExplorador = new ShapedRecipe(new NamespacedKey(plugin, "peitoral_explorador"), getPeitoralExplorador());
//        rExplorador.shape("L L", "LEL", "LLL");
//        rExplorador.setIngredient('L', Material.LEATHER);
//        rExplorador.setIngredient('E', Material.ELYTRA);
//        Bukkit.addRecipe(rExplorador);

        // Calça
        ShapedRecipe rCorredor = new ShapedRecipe(new NamespacedKey(plugin, "calca_corredor"), getCalcaCorredor());
        rCorredor.shape("LLL", "LPL", "L L");
        rCorredor.setIngredient('L', Material.LEATHER);
        rCorredor.setIngredient('P', Material.POTION);
        Bukkit.addRecipe(rCorredor);

        // Botas
        ShapedRecipe rCoelho = new ShapedRecipe(new NamespacedKey(plugin, "bota_coelho"), getBotaCoelho());
        rCoelho.shape("S S", "P P");
        rCoelho.setIngredient('S', Material.SLIME_BALL);
        rCoelho.setIngredient('P', Material.RABBIT_FOOT);
        Bukkit.addRecipe(rCoelho);
    }

    // --- LOOP DE SEGURANÇA E PARTÍCULAS ---

    private void iniciarLoopEfeitos() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                // 1. Partículas da Calça (Velocidade)
                ItemStack leggings = p.getInventory().getLeggings();
                if (leggings != null && leggings.hasItemMeta() && leggings.getItemMeta().getDisplayName().contains("Corredor")) {
                    if (p.isSprinting()) {
                        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2, 0.05);
                    }
                }

                // 2. Lógica de Voo e Partículas do Peitoral
//                if (p.getAllowFlight() && p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
//                    boolean noTerreno = sistemaTerrenos.isDono(p, p.getLocation());
//                    ItemStack chest = p.getInventory().getChestplate();
//                    boolean comPeitoral = (chest != null && chest.hasItemMeta() && chest.getItemMeta().getDisplayName().contains("Explorador"));
//
//                    if (!noTerreno || !comPeitoral) {
//                        desativarVoo(p);
//                    } else if (p.isFlying()) {
//                        // Partículas de Voo
//                        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation(), 2, 0.3, 0.3, 0.3, 0.02);
//                    }
//                }
            }
        }, 10L, 5L); // Roda a cada 5 ticks (4 vezes por segundo) para partículas fluidas
    }

    // --- EVENTOS ---

    @EventHandler
    public void aoClicarPeitoral(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getChestplate();

        if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Explorador")) {
            if (p.isSneaking() && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
                if (!sistemaTerrenos.isDono(p, p.getLocation())) {
                    p.sendMessage("§cO voo do explorador só funciona no seu terreno!");
                    return;
                }
                if (p.getAllowFlight()) {
                    desativarVoo(p);
                } else {
                    p.setAllowFlight(true);
                    p.sendMessage("§aVoo ativado!");
                    p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 15, 0.5, 0.5, 0.5, 0.1);
                }
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void aoPular(PlayerMoveEvent e) {
        // Partículas de fumaça ao pular com a bota
        Player p = e.getPlayer();
        if (e.getFrom().getY() < e.getTo().getY() && !p.isFlying()) {
            ItemStack boots = p.getInventory().getBoots();
            if (boots != null && boots.hasItemMeta() && boots.getItemMeta().getDisplayName().contains("Coelho")) {
                p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p.getLocation(), 3, 0.1, 0, 0.1, 0.02);
            }
        }
    }

    @EventHandler
    public void aoDanoDeQueda(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            ItemStack boots = p.getInventory().getBoots();
            if (boots != null && boots.hasItemMeta() && boots.getItemMeta().getDisplayName().contains("Coelho")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void gerenciarEfeitos(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack boots = p.getInventory().getBoots();
            if (boots != null && boots.hasItemMeta() && boots.getItemMeta().getDisplayName().contains("Coelho")) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, false, false));
            } else {
                p.removePotionEffect(PotionEffectType.JUMP_BOOST);
            }
        }, 1L);
    }

    private void desativarVoo(Player p) {
        p.setAllowFlight(false);
        p.setFlying(false);
        p.sendMessage("§cVoo desativado!");
        p.getWorld().spawnParticle(Particle.LARGE_SMOKE, p.getLocation(), 10, 0.2, 0.2, 0.2, 0.05);
    }
}