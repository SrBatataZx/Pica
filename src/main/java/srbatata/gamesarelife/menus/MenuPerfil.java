package srbatata.gamesarelife.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.sistemas.SistemaMissoes;
import srbatata.gamesarelife.core.EcoGerente;
import srbatata.gamesarelife.core.Principal;

import java.util.List;

public class MenuPerfil implements Listener {

    private final Principal plugin;
    private final SistemaMissoes sistemaMissoes;
    private final ArmazemAprendiz armazemAprendiz;
    private final EcoGerente economia;
    private final NamespacedKey licencaKey;

    public MenuPerfil(Principal plugin, SistemaMissoes sistemaMissoes, ArmazemAprendiz armazemAprendiz, EcoGerente economia) {
        this.plugin = plugin;
        this.sistemaMissoes = sistemaMissoes;
        this.armazemAprendiz = armazemAprendiz;
        this.economia = economia;
        // Chave nativa para guardar a licença diretamente nos dados do jogador
        this.licencaKey = new NamespacedKey(plugin, "licenca_terreno_loja");
    }

    public void abrirMenuPerfil(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Seu Perfil");

        // 1. Botão de Missões
        ItemStack missoes = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta metaMissoes = missoes.getItemMeta();
        if (metaMissoes != null) {
            metaMissoes.setDisplayName("§e§lMenu de Missões");
            metaMissoes.setLore(List.of("§7Complete tarefas e", "§7ganhe recompensas.", "", "§eClique para aceder!"));
            missoes.setItemMeta(metaMissoes);
        }

        // 2. Botão da Licença (Verifica o PDC do jogador)
        boolean possuiLicenca = player.getPersistentDataContainer().has(licencaKey, PersistentDataType.BYTE);

        ItemStack licenca = new ItemStack(Material.PAPER);
        ItemMeta metaLicenca = licenca.getItemMeta();
        if (metaLicenca != null) {
            if (possuiLicenca) {
                metaLicenca.setDisplayName("§a§lLicença de Terreno Adquirida");
                metaLicenca.setLore(List.of("§7Estado: §aAtivo", "", "§a✔ Já pode criar uma loja!"));
                metaLicenca.addEnchant(Enchantment.UNBREAKING, 1, true); // Brilho
                metaLicenca.addItemFlags(ItemFlag.HIDE_ENCHANTS); // Esconde o nome do encantamento
            } else {
                metaLicenca.setDisplayName("§6§lLicença de Terreno");
                metaLicenca.setLore(List.of("§7Compre a sua licença para", "§7ter o direito de criar uma loja.", "", "§fValor: §2$§a20.000", "", "§eClique para comprar!"));
            }
            licenca.setItemMeta(metaLicenca);
        }

        // 3. Botão do Armazém
        ItemStack armazem = new ItemStack(Material.ENDER_CHEST);
        ItemMeta metaArmazem = armazem.getItemMeta();
        if (metaArmazem != null) {
            metaArmazem.setDisplayName("§d§lArmazém do Aprendiz");
            metaArmazem.setLore(List.of("§7Guarde as suas ferramentas", "§7iniciais aqui com segurança.", "", "§eClique para abrir!"));
            armazem.setItemMeta(metaArmazem);
        }

        // 4. Botão Voltar
        ItemStack voltar = new ItemStack(Material.ARROW);
        ItemMeta metaVoltar = voltar.getItemMeta();
        if (metaVoltar != null) {
            metaVoltar.setDisplayName("§c§lVoltar");
            voltar.setItemMeta(metaVoltar);
        }

        // Posicionando no inventário
        inv.setItem(11, missoes);
        inv.setItem(13, licenca);
        inv.setItem(15, armazem);
        inv.setItem(26, voltar);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Pattern Matching introduzido nas versões mais recentes do Java
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§8Seu Perfil")) return;

        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();

        if (current == null || current.getType() == Material.AIR) return;

        if (current.getType() == Material.ENCHANTED_BOOK) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            Bukkit.getScheduler().runTask(plugin, () -> sistemaMissoes.abrirMenuMissoes(player));

        } else if (current.getType() == Material.ENDER_CHEST) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            Bukkit.getScheduler().runTask(plugin, () -> armazemAprendiz.abrirArmazem(player));

        } else if (current.getType() == Material.PAPER) {
            // Lógica de Compra da Licença
            boolean possuiLicenca = player.getPersistentDataContainer().has(licencaKey, PersistentDataType.BYTE);

            if (possuiLicenca) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage("§cJá possui a Licença de Terreno!");
                return;
            }

            double precoLicenca = 20000.0;
            // O jogador (Player) é automaticamente aceite como OfflinePlayer no seu EcoGerente
            double saldoAtual = economia.getSaldo(player);

            if (saldoAtual >= precoLicenca) {
                // Subtrai o valor e atualiza no ficheiro
                economia.setSaldo(player, saldoAtual - precoLicenca);

                // Grava a licença no PDC do jogador (Nativo e otimizado)
                player.getPersistentDataContainer().set(licencaKey, PersistentDataType.BYTE, (byte) 1);

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.sendMessage("§aLicença de terreno adquirida com sucesso! Agora já pode criar a sua loja.");

                // Atualiza o ecrã instantaneamente
                abrirMenuPerfil(player);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage("§cNão tem moedas suficientes. A licença custa $20.000 moedas!");
            }

        } else if (current.getType() == Material.ARROW) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.performCommand("menu"); // Volta para o Menu Principal
        }
    }
}