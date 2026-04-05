package srbatata.gamesarelife.sistemas;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import srbatata.gamesarelife.core.Principal;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SistemaPesca implements Listener {

    private static final ItemStack AGUA_TELA;
    private static final Material[] ICONES_PEIXE = {
            Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH
    };

    static {
        AGUA_TELA = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = AGUA_TELA.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            AGUA_TELA.setItemMeta(meta);
        }
    }

    private static class PescaHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private final Principal plugin;
    private final Economy economia;
    private final Map<UUID, Integer> peixeSlot = new HashMap<>();
    private final String menuNome = "§3§lPEGUE O PEIXE!!!";

    private final NamespacedKey pesoKey;
    private final NamespacedKey valorKey;
    private final NamespacedKey varaKey;

    public SistemaPesca(Principal plugin, Economy economia) {
        this.plugin = plugin;
        this.economia = economia;
        this.pesoKey = new NamespacedKey(plugin, "peixe_peso");
        this.valorKey = new NamespacedKey(plugin, "peixe_valor");
        this.varaKey = new NamespacedKey(plugin, "item_vara_especial");
        registrarReceita();
    }

    private void registrarReceita() {
        ItemStack vara = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = vara.getItemMeta();
        meta.setDisplayName("§b§lVara de Pesca Profissional");
        meta.setLore(List.of("§7Vara de alta precisão.", "", "§e[SHIFT + CLIQUE ESQUERDO]", "§7Vender peixes do inventário."));
        meta.getPersistentDataContainer().set(varaKey, PersistentDataType.BYTE, (byte) 1);
        vara.setItemMeta(meta);

        NamespacedKey key = new NamespacedKey(plugin, "vara_pica");
        ShapedRecipe receita = new ShapedRecipe(key, vara);
        receita.shape("  /", " /S", "/ G");
        receita.setIngredient('/', Material.STICK);
        receita.setIngredient('S', Material.STRING);
        receita.setIngredient('G', Material.TRIPWIRE_HOOK);

        if (Bukkit.getRecipe(key) == null) Bukkit.addRecipe(receita);
    }

    @EventHandler
    public void aoPescar(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player p = event.getPlayer();
            event.getHook().remove();
            event.setCancelled(true);

            // Se estiver com "Sorte no Mar", a chance de vir lixo diminui 5% por nível
            int sorteNoMar = p.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);
            int chanceLixo = Math.max(5, 30 - (sorteNoMar * 5));

            if (ThreadLocalRandom.current().nextInt(100) < chanceLixo) {
                darLixo(p);
            } else {
                abrirMenuPesca(p);
            }
        }
    }

    private void abrirMenuPesca(Player p) {
        ItemStack vara = p.getInventory().getItemInMainHand();
        int lvlLure = vara.getEnchantmentLevel(Enchantment.LURE);

        // Melhoria LURE: Aumenta o tempo do relógio (Base 10s + 2s por nível)
        int tempoTotal = 10 + (lvlLure * 2);
        // Melhoria LURE: Aumenta o tempo que o peixe fica no slot (Fica mais lento/fácil)
        long delayTicks = 10L + (lvlLure * 2L);

        Inventory inv = Bukkit.createInventory(new PescaHolder(), 27, menuNome);
        for (int i = 0; i < 27; i++) inv.setItem(i, AGUA_TELA);
        p.openInventory(inv);

        new BukkitRunnable() {
            int segundosRestantes = tempoTotal;
            int ticksParaSegundo = 0;
            int ultimoSlotPeixe = -1;
            final long ticksPorCiclo = delayTicks;

            @Override
            public void run() {
                if (!(p.getOpenInventory().getTopInventory().getHolder() instanceof PescaHolder)) {
                    peixeSlot.remove(p.getUniqueId());
                    this.cancel();
                    return;
                }

                // Lógica para decrementar segundos baseada no delay variável
                ticksParaSegundo += (int) ticksPorCiclo;
                if (ticksParaSegundo >= 20) {
                    segundosRestantes--;
                    ticksParaSegundo = 0;
                }

                if (segundosRestantes <= 0) {
                    p.closeInventory();
                    p.sendMessage("§c§l[!] §7O peixe escapou!");
                    this.cancel();
                    return;
                }

                if (ultimoSlotPeixe != -1) inv.setItem(ultimoSlotPeixe, AGUA_TELA);

                ItemStack info = new ItemStack(Material.CLOCK);
                ItemMeta mI = info.getItemMeta();
                mI.setDisplayName("§eTempo restante: §f" + segundosRestantes + "s");
                info.setItemMeta(mI);
                inv.setItem(4, info);

                int slot = ThreadLocalRandom.current().nextInt(27);
                if (slot == 4) slot = 5;

                inv.setItem(slot, gerarIconeFisgar());
                peixeSlot.put(p.getUniqueId(), slot);
                ultimoSlotPeixe = slot;

                p.playSound(p.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.3f, 2f);
            }
        }.runTaskTimer(plugin, 0L, delayTicks);
    }

    private ItemStack gerarIconeFisgar() {
        ItemStack icone = new ItemStack(ICONES_PEIXE[ThreadLocalRandom.current().nextInt(ICONES_PEIXE.length)]);
        ItemMeta meta = icone.getItemMeta();
        meta.setDisplayName("§6§lFISGUE-ME!");
        icone.setItemMeta(meta);
        return icone;
    }

    @EventHandler
    public void aoClicarMenu(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PescaHolder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        Integer slotCorreto = peixeSlot.get(p.getUniqueId());
        if (slotCorreto != null && event.getRawSlot() == slotCorreto) {
            peixeSlot.remove(p.getUniqueId());
            p.closeInventory();

            ItemStack peixe = gerarPeixeUnico(p);
            entregarItem(p, peixe);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
        }
    }

    private void entregarItem(Player p, ItemStack item) {
        var resto = p.getInventory().addItem(item);
        if (!resto.isEmpty()) {
            resto.values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
            p.sendMessage("§6[!] §7Inventário cheio! O item caiu no chão.");
        }
    }

    private ItemStack gerarPeixeUnico(Player p) {
        // Melhoria LUCK OF THE SEA: Aumenta as chances de raridade
        int lvlLuck = p.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);

        double chanceTesouro = 2.0 + (lvlLuck * 1.0); // +1% por nível
        double chanceLendario = 7.0 + (lvlLuck * 3.0); // +3% por nível

        double sorte = ThreadLocalRandom.current().nextDouble(100.0);

        Material materialSorteado;
        String nomeExibicao;
        double multiplicadorValor;
        double pesoBase;

        if (sorte < chanceTesouro) {
            Material[] itensTesouro = {Material.ENCHANTED_BOOK, Material.NAME_TAG, Material.GOLDEN_APPLE, Material.NAUTILUS_SHELL};
            materialSorteado = itensTesouro[ThreadLocalRandom.current().nextInt(itensTesouro.length)];
            nomeExibicao = "§6§l§nTESOURO PERDIDO";
            multiplicadorValor = 300.0;
            pesoBase = 10.0 + (50.0 * ThreadLocalRandom.current().nextDouble());
        } else if (sorte < (chanceTesouro + chanceLendario)) {
            materialSorteado = ICONES_PEIXE[ThreadLocalRandom.current().nextInt(ICONES_PEIXE.length)];
            nomeExibicao = "§d§l" + materialSorteado.name().replace("_", " ") + " LENDÁRIO";
            multiplicadorValor = 240.0;
            pesoBase = 25.0 + (15.0 * ThreadLocalRandom.current().nextDouble());
        } else {
            materialSorteado = ICONES_PEIXE[ThreadLocalRandom.current().nextInt(ICONES_PEIXE.length)];
            nomeExibicao = "§6Peixe Comum";
            multiplicadorValor = 40.0;
            pesoBase = 0.5 + (15.0 * ThreadLocalRandom.current().nextDouble());
        }

        ItemStack itemResult = new ItemStack(materialSorteado);
        ItemMeta meta = itemResult.getItemMeta();
        double valorFinal = pesoBase * multiplicadorValor;

        meta.getPersistentDataContainer().set(pesoKey, PersistentDataType.DOUBLE, pesoBase);
        meta.getPersistentDataContainer().set(valorKey, PersistentDataType.DOUBLE, valorFinal);

        if (nomeExibicao.contains("LENDÁRIO") || nomeExibicao.contains("TESOURO")) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            anunciarPescaEspecial(p, materialSorteado, pesoBase, valorFinal, nomeExibicao.contains("TESOURO"));
        } else {
            p.sendMessage("§a§l✔ §fCapturado com sucesso!");
        }

        meta.setDisplayName(nomeExibicao);
        meta.setLore(List.of(
                "§7Peso: §f" + String.format("%.2f", pesoBase) + "kg",
                "§7Valor: §a$" + String.format("%.2f", valorFinal),
                "",
                "§eUse SHIFT + CLICK ESQUERDO com a vara",
                "§epara vender este item!"
        ));

        itemResult.setItemMeta(meta);
        return itemResult;
    }

    private void anunciarPescaEspecial(Player p, Material mat, double peso, double valor, boolean tesouro) {
        String prefixo = tesouro ? "§6§l[TESOURO]" : "§d§l[LENDÁRIO]";
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage(prefixo + " §e" + p.getName() + " §fpescou um(a) §l" + mat.name().replace("_", " ") + "!");
        Bukkit.broadcastMessage("§fPeso: §b" + String.format("%.2f", peso) + "kg §f| Valor: §a$" + String.format("%.2f", valor));
        Bukkit.broadcastMessage(" ");
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    @EventHandler
    public void aoVenderInteracao(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (p.isSneaking() && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(varaKey, PersistentDataType.BYTE)) {
                venderTudo(p);
            }
        }
    }

    private void venderTudo(Player p) {
        double total = 0;
        int count = 0;
        for (ItemStack item : p.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            Double valorPeixe = meta.getPersistentDataContainer().get(valorKey, PersistentDataType.DOUBLE);
            if (valorPeixe != null) {
                total += valorPeixe * item.getAmount();
                count += item.getAmount();
                item.setAmount(0);
            }
        }
        if (count > 0) {
            economia.depositPlayer(p, total);
            p.sendMessage("§a§l[PESCA] §fVendeu §e" + count + " peixe(s) §fpor §a$" + String.format("%.2f", total));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            p.sendMessage("§cSem peixes no inventário.");
        }
    }

    private void darLixo(Player p) {
        ItemStack lixo = new ItemStack(Material.KELP);
        entregarItem(p, lixo);
        p.sendMessage("§7[!] Você pegou lixo, mais sorte na próxima...");
        p.playSound(p.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1f, 1f);
    }
    @EventHandler
    public void aoSairPesca(org.bukkit.event.player.PlayerQuitEvent e) {
        peixeSlot.remove(e.getPlayer().getUniqueId());
    }
}