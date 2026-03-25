package srbatata.pica;

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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import srbatata.pica.core.PicaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SistemaPesca implements Listener {

    private final PicaPlugin plugin;
    private final Economy economia;
    private final Map<UUID, Integer> peixeSlot = new HashMap<>();
    private final String menuNome = "§3§lPEGUE O PEIXE!!!";

    private final NamespacedKey pesoKey;
    private final NamespacedKey valorKey;
    private final NamespacedKey varaKey;

    public SistemaPesca(PicaPlugin plugin, Economy economia) {
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
        meta.setLore(List.of(
                "§7Vara de alta precisão.",
                "",
                "§e[SHIFT + CLIQUE ESQUERDO]",
                "§7Vender peixes do inventário."
        ));
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

            // 30% de chance de vir lixo
            if (ThreadLocalRandom.current().nextInt(100) < 30) {
                darLixo(p);
            } else {
                abrirMenuPesca(p);
            }
        }
    }

    private void abrirMenuPesca(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, menuNome);
        p.openInventory(inv);

        new BukkitRunnable() {
            int ticksExecutados = 0;
            int segundosRestantes = 10;

            @Override
            public void run() {
                if (!p.getOpenInventory().getTitle().equals(menuNome)) {
                    peixeSlot.remove(p.getUniqueId());
                    this.cancel();
                    return;
                }

                // Como roda a cada 10 ticks (0.5s), a cada 2 runs passou 1 segundo
                if (ticksExecutados > 0 && ticksExecutados % 2 == 0) {
                    segundosRestantes--;
                }
                ticksExecutados++;

                if (segundosRestantes <= 0) {
                    p.closeInventory();
                    p.sendMessage("§c§l[!] §7O peixe escapou! Você demorou demais.");
                    p.playSound(p.getLocation(), Sound.ENTITY_FISH_SWIM, 1f, 0.5f);
                    this.cancel();
                    return;
                }

                inv.clear();

                // Info do Tempo
                ItemStack info = new ItemStack(Material.CLOCK);
                ItemMeta mI = info.getItemMeta();
                mI.setDisplayName("§eTempo restante: §f" + segundosRestantes + "s");
                info.setItemMeta(mI);
                inv.setItem(4, info);

                // Fundo de Água
                ItemStack agua = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
                ItemMeta mA = agua.getItemMeta(); mA.setDisplayName(" "); agua.setItemMeta(mA);
                for (int i = 0; i < 27; i++) if (i != 4) inv.setItem(i, agua);

                // Slot do Peixe
                int slot = ThreadLocalRandom.current().nextInt(27);
                if (slot == 4) slot = 5;

                Material[] icones = {Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH};
                ItemStack peixeIcone = new ItemStack(icones[ThreadLocalRandom.current().nextInt(icones.length)]);
                ItemMeta mP = peixeIcone.getItemMeta();
                mP.setDisplayName("§6§lFISGUE-ME!");
                peixeIcone.setItemMeta(mP);

                inv.setItem(slot, peixeIcone);
                peixeSlot.put(p.getUniqueId(), slot);

                p.playSound(p.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.3f, 2f);
            }
        }.runTaskTimer(plugin, 0L, 10L); // Peixe muda a cada 0.5s (Melhor para Bedrock)
    }

    @EventHandler
    public void aoClicarMenu(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(menuNome)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        Integer slotCorreto = peixeSlot.get(p.getUniqueId());
        if (slotCorreto != null && event.getRawSlot() == slotCorreto) {
            peixeSlot.remove(p.getUniqueId());
            p.closeInventory();

            // Entrega o peixe e processa se é lendário
            p.getInventory().addItem(gerarPeixeUnico(p));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
        }
    }

    private ItemStack gerarPeixeUnico(Player p) {
        int sorte = ThreadLocalRandom.current().nextInt(100);

        boolean tesouro = sorte < 2;     // 2% de chance de Tesouro
        boolean lendario = sorte < 7;    // 5% de chance de Lendário (se não for tesouro)

        Material materialSorteado;
        String nomeExibicao;
        double multiplicadorValor;
        double pesoBase;

        if (tesouro) {
            // --- LOGICA DE TESOUROS ---
            Material[] itensTesouro = {Material.ENCHANTED_BOOK, Material.NAME_TAG, Material.GOLDEN_APPLE, Material.NAUTILUS_SHELL};
            materialSorteado = itensTesouro[ThreadLocalRandom.current().nextInt(itensTesouro.length)];
            nomeExibicao = "§6§l§nTESOURO PERDIDO";
            multiplicadorValor = 250.0; // R$ 250,00 por kg
            pesoBase = 10.0 + (50.0 * ThreadLocalRandom.current().nextDouble()); // Itens pesados e valiosos
        } else if (lendario) {
            // --- LOGICA DE LENDÁRIOS ---
            Material[] peixes = {Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH};
            materialSorteado = peixes[ThreadLocalRandom.current().nextInt(peixes.length)];
            nomeExibicao = "§d§l" + materialSorteado.name().replace("_", " ") + " LENDÁRIO";
            multiplicadorValor = 80.0;
            pesoBase = 25.0 + (15.0 * ThreadLocalRandom.current().nextDouble());
        } else {
            // --- LOGICA DE COMUNS ---
            Material[] peixes = {Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH};
            materialSorteado = peixes[ThreadLocalRandom.current().nextInt(peixes.length)];
            nomeExibicao = "§6Peixe Comum";
            multiplicadorValor = 12.5;
            pesoBase = 0.5 + (15.0 * ThreadLocalRandom.current().nextDouble());
        }

        ItemStack itemResult = new ItemStack(materialSorteado);
        ItemMeta meta = itemResult.getItemMeta();

        double valorFinal = pesoBase * multiplicadorValor;

        // Persistência NBT
        meta.getPersistentDataContainer().set(pesoKey, PersistentDataType.DOUBLE, pesoBase);
        meta.getPersistentDataContainer().set(valorKey, PersistentDataType.DOUBLE, valorFinal);

        if (tesouro || lendario) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Broadcast Especial
            String prefixo = tesouro ? "§6§l[TESOURO]" : "§d§l[LENDÁRIO]";
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage(prefixo + " §e" + p.getName() + " §fpescou um(a) §l" + materialSorteado.name().replace("_", " ") + "!");
            Bukkit.broadcastMessage("§fPeso: §b" + String.format("%.2f", pesoBase) + "kg §f| Valor: §a$" + String.format("%.2f", valorFinal));
            Bukkit.broadcastMessage(" ");

            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
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

//    private ItemStack gerarPeixeUnico(Player p) {
//        // 3% de chance de ser lendário
//        boolean lendario = ThreadLocalRandom.current().nextInt(100) < 3;
//
//        // Lista de todos os peixes possíveis do Minecraft
//        Material[] tiposDePeixe = {
//                Material.COD,
//                Material.SALMON,
//                Material.TROPICAL_FISH,
//                Material.PUFFERFISH
//        };
//
//        // Sorteia um dos materiais acima
//        Material materialSorteado = tiposDePeixe[ThreadLocalRandom.current().nextInt(tiposDePeixe.length)];
//
//        ItemStack peixe = new ItemStack(materialSorteado);
//        ItemMeta meta = peixe.getItemMeta();
//
//        // Lógica de Peso e Valor
//        double peso = (lendario ? 30.0 : 0.5) + (20.0 * ThreadLocalRandom.current().nextDouble());
//        double valor = peso * (lendario ? 80.0 : 12.5);
//
//        meta.getPersistentDataContainer().set(pesoKey, PersistentDataType.DOUBLE, peso);
//        meta.getPersistentDataContainer().set(valorKey, PersistentDataType.DOUBLE, valor);
//
//        if (lendario) {
//            // Nome dinâmico baseado no tipo do peixe para dar mais imersão
//            String nomePeixe = materialSorteado.name().replace("_", " ");
//            meta.setDisplayName("§d§l§k!§5§l " + nomePeixe + " LENDÁRIO §d§l§k!");
//
//            meta.addEnchant(Enchantment.LURE, 1, true);
//            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
//
//            // MENSAGEM GLOBAL
//            Bukkit.broadcastMessage(" ");
//            Bukkit.broadcastMessage("§d§l[PESCA] §fO jogador §e" + p.getName() + " §fpescou um");
//            Bukkit.broadcastMessage("§d§l" + nomePeixe + " LENDÁRIO §fde §b" + String.format("%.2f", peso) + "kg§f!");
//            Bukkit.broadcastMessage(" ");
//
//            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
//        } else {
//            meta.setDisplayName("§6Peixe Comum");
//            p.sendMessage("§a§l✔ §fCapturado com sucesso!");
//        }
//
//        meta.setLore(List.of(
//                "§7Peso: §f" + String.format("%.2f", peso) + "kg",
//                "§7Valor: §a$" + String.format("%.2f", valor)
//        ));
//
//        peixe.setItemMeta(meta);
//        return peixe;
//    }

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
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            // Pegando valor de forma segura para evitar NullPointerException
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
        p.getInventory().addItem(new ItemStack(Material.BONE));
        p.sendMessage("§7[!] Voce pegou lixo, mais sorte na proxima...");
        p.playSound(p.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1f, 1f);
    }
}