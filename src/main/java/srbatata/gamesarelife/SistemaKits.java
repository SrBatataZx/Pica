package srbatata.gamesarelife;

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
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.core.Principal;

import java.util.Arrays;

public class SistemaKits implements Listener {

    private final Principal plugin;
    private final NamespacedKey keyPicareta;
    private final NamespacedKey keyMachado;
    private final NamespacedKey keyVara;

    public SistemaKits(Principal plugin) {
        this.plugin = plugin;
        // As mesmas chaves que criamos nos comandos das ferramentas
        this.keyPicareta = new NamespacedKey(plugin, "blocos_quebrados");
        this.keyMachado = new NamespacedKey(plugin, "blocos_quebrados_machado");
        this.keyVara = new NamespacedKey(plugin, "vara_protecao");
    }

    public void abrirMenuKits(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Menu de Kits");

        // Ícone do Kit Aprendiz (Já existia)
        ItemStack kitAprendiz = new ItemStack(Material.CHEST);
        ItemMeta metaApr = kitAprendiz.getItemMeta();
        metaApr.setDisplayName("§a§lKit Aprendiz");
        metaApr.setLore(Arrays.asList("§7Receba ferramentas iniciais.", "", "§eClique para resgatar!"));
        kitAprendiz.setItemMeta(metaApr);

        // NOVO: Ícone do Kit Protetor
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
                "",
                "§eClique para resgatar!"
        ));
        kitProtetor.setItemMeta(metaProt);

        inv.setItem(11, kitAprendiz);
        inv.setItem(15, kitProtetor); // Posiciona o Kit Protetor do lado direito

        ItemStack voltar = new ItemStack(Material.ARROW);
        ItemMeta metaVoltar = voltar.getItemMeta();
        metaVoltar.setDisplayName("§c§lVoltar ao Menu Principal");
        voltar.setItemMeta(metaVoltar);
        inv.setItem(26, voltar); // Canto inferior esquerdo

        // NOVO: Ícone do Kit Pão
        ItemStack kitPao = new ItemStack(Material.BREAD);
        ItemMeta metaPao = kitPao.getItemMeta();
        metaPao.setDisplayName("§6§lKit Pão");
        metaPao.setLore(Arrays.asList(
                "§7Receba 32 pães para se alimentar.",
                "",
                "§eClique para resgatar!"
        ));
        kitPao.setItemMeta(metaPao);

        // Coloque em algum slot livre (ex: 13 no meio)
        inv.setItem(13, kitPao);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack itemClicado = event.getCurrentItem();

        if (itemClicado == null || itemClicado.getType() == Material.AIR) return;

        // Lógica do Menu de Kits
        if (event.getView().getTitle().equals("§8Menu de Kits")) {
            event.setCancelled(true); // Impede de roubar o item

            if (itemClicado.getType() == Material.ARROW) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.performCommand("menu");
                return;
            }

            // Se clicou no Baú do Kit Aprendiz
            if (itemClicado.getType() == Material.CHEST) {
                player.closeInventory();

                boolean temPicareta = false;
                boolean temMachado = false;

                // Vasculha o inventário inteiro do jogador
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.hasItemMeta()) {
                        ItemMeta meta = item.getItemMeta();

                        // Verifica se achou a Picareta
                        if (meta.getPersistentDataContainer().has(keyPicareta, PersistentDataType.INTEGER)) {
                            temPicareta = true;
                        }
                        // Verifica se achou o Machado
                        if (meta.getPersistentDataContainer().has(keyMachado, PersistentDataType.INTEGER)) {
                            temMachado = true;
                        }
                    }
                }

                // Verifica o que o jogador já tem
                if (temPicareta && temMachado) {
                    // Tem as duas coisas
                    player.sendMessage("§cVocê já tem o Kit Aprendiz completo no seu inventário!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } else {
                    // Falta alguma coisa (ou falta tudo)
                    if (!temPicareta) {
                        player.performCommand("picareta");
                    }
                    if (!temMachado) {
                        player.performCommand("machado");
                    }

                    player.sendMessage("§a🎉 Você resgatou as ferramentas que faltavam do §lKit Aprendiz§a!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            }else if (itemClicado.getType() == Material.BLAZE_ROD) {
                player.closeInventory();

                // Cria e entrega a Vara da Proteção
                ItemStack vara = new ItemStack(Material.BLAZE_ROD);
                ItemMeta meta = vara.getItemMeta();
                meta.setDisplayName("§e§lVara da Proteção");
                meta.setLore(Arrays.asList("§7Item mágico para proteger áreas."));

                // Adiciona a TAG para o sistema reconhecer a vara verdadeira
                meta.getPersistentDataContainer().set(keyVara, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                vara.setItemMeta(meta);

                player.getInventory().addItem(vara);
                player.sendMessage("§a🎉 Você recebeu o §lKit Protetor§a!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }else if (itemClicado.getType() == Material.BREAD) {
                player.closeInventory();

                String path = "kits.pao." + player.getUniqueId();
                long agora = System.currentTimeMillis();
                long cooldown = 10 * 60 * 1000; // 10 minutos

                // Se já existe tempo salvo
                if (plugin.getSalvos().contains(path)) {
                    long ultimoUso = plugin.getSalvos().getLong(path);

                    long tempoRestante = (ultimoUso + cooldown) - agora;

                    if (tempoRestante > 0) {
                        long segundos = tempoRestante / 1000;
                        long minutos = segundos / 60;
                        segundos = segundos % 60;

                        player.sendMessage("§cVocê precisa esperar §e" + minutos + "m " + segundos + "s §cpara pegar o Kit Pão novamente!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }
                }

                // Libera o kit
                ItemStack pao = new ItemStack(Material.BREAD, 32);
                player.getInventory().addItem(pao);

                // Salva o tempo atual
                plugin.getSalvos().set(path, agora);
                plugin.saveSalvos();

                player.sendMessage("§a🍞 Você recebeu o §lKit Pão§a!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
    }
}