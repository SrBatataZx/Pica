package srbatata.gamesarelife.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.core.Principal;
import srbatata.gamesarelife.itens.AprendizConstruct;

import java.util.Arrays;

public class SistemaKits implements Listener {

    private final Principal plugin;
    private final NamespacedKey keyPicareta;
    private final NamespacedKey keyMachado;
    private final NamespacedKey keyPa;
    private final NamespacedKey keyVara;

    private final NamespacedKey cooldownVaraKey;
    private final NamespacedKey cooldownPaoKey;

    public SistemaKits(Principal plugin) {
        this.plugin = plugin;
        this.keyPicareta = new NamespacedKey(plugin, "blocos_quebrados");
        this.keyMachado = new NamespacedKey(plugin, "blocos_quebrados_machado");
        this.keyPa = new NamespacedKey(plugin, "blocos_quebrados_pa");
        this.keyVara = new NamespacedKey(plugin, "vara_protecao");

        this.cooldownVaraKey = new NamespacedKey(plugin, "cooldown_vara");
        this.cooldownPaoKey = new NamespacedKey(plugin, "cooldown_pao");
    }

    public void abrirMenuKits(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Menu de Kits");

        ItemStack kitAprendiz = new ItemStack(Material.CHEST);
        ItemMeta metaApr = kitAprendiz.getItemMeta();
        metaApr.setDisplayName("§a§lKit Aprendiz");
        metaApr.setLore(Arrays.asList("§7Receba ferramentas iniciais.", "§7(Apenas as que você não possui)", "", "§eClique para resgatar!"));
        kitAprendiz.setItemMeta(metaApr);

        ItemStack kitProtetor = new ItemStack(Material.BLAZE_ROD);
        ItemMeta metaProt = kitProtetor.getItemMeta();
        metaProt.setDisplayName("§e§lKit Protetor");
        metaProt.setLore(Arrays.asList(
                "§7Receba sua Vara de Proteção",
                "§7para proteger suas casas.",
                "",
                "§fComo usar:",
                "§8- §7Botão Esquerdo: §fPosição 1",
                "§8- §7Botão Direito: §fPosição 2",
                "§8- §7Digite: §a/proteger",
                "§6Cooldown: §e2 Horas",
                "§eClique para resgatar!"
        ));
        kitProtetor.setItemMeta(metaProt);

        ItemStack kitPao = new ItemStack(Material.BREAD);
        ItemMeta metaPao = kitPao.getItemMeta();
        metaPao.setDisplayName("§6§lKit Pão");
        metaPao.setLore(Arrays.asList("§7Receba 32 pães.", "", "§6Cooldown: §e10 Minutos", "", "§eClique para resgatar!"));
        kitPao.setItemMeta(metaPao);

        inv.setItem(11, kitAprendiz);
        inv.setItem(13, kitPao);
        inv.setItem(15, kitProtetor);

        ItemStack voltar = new ItemStack(Material.ARROW);
        ItemMeta metaV = voltar.getItemMeta();
        metaV.setDisplayName("§cVoltar");
        voltar.setItemMeta(metaV);
        inv.setItem(26, voltar);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§8Menu de Kits")) return;

        event.setCancelled(true);
        ItemStack itemClicado = event.getCurrentItem();
        if (itemClicado == null || itemClicado.getType() == Material.AIR) return;

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        long agora = System.currentTimeMillis();

        if (itemClicado.getType() == Material.CHEST) {
            player.closeInventory();

            // Usar um array de booleanos para facilitar a referência [Picareta, Machado, Pá]
            boolean[] possuidos = verificarItensPossuidos(player);

            if (possuidos[0] && possuidos[1] && possuidos[2]) {
                player.sendMessage("§cVocê já possui o Kit Aprendiz completo (no inventário ou no armazém)!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Entrega as ferramentas usando a Factory do AprendizConstruct
            if (!possuidos[0]) player.getInventory().addItem(AprendizConstruct.getPicareta(plugin));
            if (!possuidos[1]) player.getInventory().addItem(AprendizConstruct.getMachado(plugin));
            if (!possuidos[2]) player.getInventory().addItem(AprendizConstruct.getPa(plugin));

            player.sendMessage("§a🎉 Você resgatou os itens que faltavam!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

        else if (itemClicado.getType() == Material.BLAZE_ROD) {
            player.closeInventory();
            long cooldown = 2L * 60 * 60 * 1000;

            if (pdc.has(cooldownVaraKey, PersistentDataType.LONG)) {
                long resta = (pdc.get(cooldownVaraKey, PersistentDataType.LONG) + cooldown) - agora;
                if (resta > 0) {
                    enviarMensagem(player, resta, "Vara de Proteção");
                    return;
                }
            }

            ItemStack vara = new ItemStack(Material.BLAZE_ROD);
            ItemMeta meta = vara.getItemMeta();
            meta.setDisplayName("§e§lVara da Proteção");
            meta.setLore(Arrays.asList(
                    "",
                    "§fComo usar:",
                    "§8- §7Botão Esquerdo: §fPosição 1",
                    "§8- §7Botão Direito: §fPosição 2",
                    "§8- §7Digite: §a/proteger",
                    "",
                    "§eClique para resgatar!"
            ));
            meta.getPersistentDataContainer().set(keyVara, PersistentDataType.BYTE, (byte) 1);
            vara.setItemMeta(meta);

            player.getInventory().addItem(vara);
            pdc.set(cooldownVaraKey, PersistentDataType.LONG, agora);
            player.sendMessage("§a🎉 Kit Protetor resgatado!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

        else if (itemClicado.getType() == Material.BREAD) {
            player.closeInventory();
            long cooldown = 10L * 60 * 1000;

            if (pdc.has(cooldownPaoKey, PersistentDataType.LONG)) {
                long resta = (pdc.get(cooldownPaoKey, PersistentDataType.LONG) + cooldown) - agora;
                if (resta > 0) {
                    enviarMensagem(player, resta, "Kit Pão");
                    return;
                }
            }

            player.getInventory().addItem(new ItemStack(Material.BREAD, 32));
            pdc.set(cooldownPaoKey, PersistentDataType.LONG, agora);
            player.sendMessage("§a🍞 Pães recebidos!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

        else if (itemClicado.getType() == Material.ARROW) {
            player.performCommand("menu");
        }
    }

    /**
     * Verifica inventário e o YAML do Armazém em busca das ferramentas.
     * Retorna um array indicando a posse na ordem: [Picareta, Machado, Pá]
     */
    private boolean[] verificarItensPossuidos(Player player) {
        boolean[] status = new boolean[]{false, false, false}; // 0 = Picareta, 1 = Machado, 2 = Pá

        // 1. Verifica no Inventário do Jogador
        for (ItemStack invItem : player.getInventory().getContents()) {
            checarItem(invItem, status);
        }

        // 2. Verifica no Armazém do Aprendiz (salvos.yml)
        String path = "armazem_aprendiz." + player.getUniqueId();
        if (plugin.getSalvos().contains(path)) {
            for (int i = 0; i < 9; i++) {
                String itemPath = path + "." + i;
                if (plugin.getSalvos().contains(itemPath)) {
                    ItemStack savedItem = plugin.getSalvos().getItemStack(itemPath);
                    checarItem(savedItem, status);
                }
            }
        }

        return status;
    }

    // Método auxiliar para evitar duplicação de código ao ler o PDC de um item
    private void checarItem(ItemStack item, boolean[] status) {
        if (item == null || !item.hasItemMeta()) return;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        if (pdc.has(keyPicareta, PersistentDataType.INTEGER)) status[0] = true;
        if (pdc.has(keyMachado, PersistentDataType.INTEGER)) status[1] = true;
        if (pdc.has(keyPa, PersistentDataType.INTEGER)) status[2] = true;
    }

    private void enviarMensagem(Player p, long ms, String nome) {
        long h = ms / 3600000;
        long m = (ms % 3600000) / 60000;
        long s = (ms % 60000) / 1000;
        String tempo = (h > 0 ? h + "h " : "") + m + "m " + s + "s";
        p.sendMessage("§cAguarde §e" + tempo + " §cpara pegar o " + nome + "!");
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }
}