package srbatata.pica;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import srbatata.pica.core.Pica;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SistemaMissoes implements Listener {

    private final Pica plugin;
    private final Economy econ;
    private final Map<UUID, Missao> missoesAtivas = new HashMap<>();

    public SistemaMissoes(Pica plugin, Economy econ) {
        this.plugin = plugin;
        this.econ = econ;

        // Carrega todas as missões salvas quando o servidor liga
        carregarMissoes();
    }

    private static class Missao {
        String tipo;
        int objetivo;
        int progresso;
        String descricao;
        double premio;

        Missao(String tipo, int objetivo, String descricao, double premio) {
            this.tipo = tipo;
            this.objetivo = objetivo;
            this.progresso = 0;
            this.descricao = descricao;
            this.premio = premio;
        }
    }

    // ==========================================
    // SISTEMA DE SALVAR E CARREGAR (salvos.yml)
    // ==========================================
    private void salvarMissao(UUID uuid) {
        String path = "missoes_ativas." + uuid.toString();

        if (missoesAtivas.containsKey(uuid)) {
            Missao m = missoesAtivas.get(uuid);
            plugin.getSalvos().set(path + ".tipo", m.tipo);
            plugin.getSalvos().set(path + ".objetivo", m.objetivo);
            plugin.getSalvos().set(path + ".progresso", m.progresso);
            plugin.getSalvos().set(path + ".descricao", m.descricao);
            plugin.getSalvos().set(path + ".premio", m.premio);
        } else {
            // Se ele não tem missão, apaga do arquivo
            plugin.getSalvos().set(path, null);
        }
        plugin.saveSalvos();
    }

    private void carregarMissoes() {
        if (plugin.getSalvos().contains("missoes_ativas")) {
            for (String uuidStr : plugin.getSalvos().getConfigurationSection("missoes_ativas").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                String path = "missoes_ativas." + uuidStr;

                String tipo = plugin.getSalvos().getString(path + ".tipo");
                int objetivo = plugin.getSalvos().getInt(path + ".objetivo");
                int progresso = plugin.getSalvos().getInt(path + ".progresso");
                String descricao = plugin.getSalvos().getString(path + ".descricao");
                double premio = plugin.getSalvos().getDouble(path + ".premio");

                Missao m = new Missao(tipo, objetivo, descricao, premio);
                m.progresso = progresso; // Restaura de onde ele parou
                missoesAtivas.put(uuid, m);
            }
        }
    }

    // Salva a missão do jogador caso ele saia do servidor (Para não lagar salvando a cada bloco quebrado)
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        salvarMissao(event.getPlayer().getUniqueId());
    }

    // ==========================================
    // MENU DE MISSÕES (Lendo da config.yml)
    // ==========================================
    public void abrirMenuMissoes(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, "§8Quadro de Missões");

        // Lendo os valores da config (Se não existir, usa o valor padrão após a vírgula)
        int objMin1 = plugin.getConfig().getInt("missoes.minerador_1.objetivo", 64);
        double premMin1 = plugin.getConfig().getDouble("missoes.minerador_1.premio", 500.0);

        int objCac1 = plugin.getConfig().getInt("missoes.cacador_1.objetivo", 20);
        double premCac1 = plugin.getConfig().getDouble("missoes.cacador_1.premio", 650.0);

        int objMin2 = plugin.getConfig().getInt("missoes.minerador_2.objetivo", 500);
        double premMin2 = plugin.getConfig().getDouble("missoes.minerador_2.premio", 5000.0);

        int objCac2 = plugin.getConfig().getInt("missoes.cacador_2.objetivo", 100);
        double premCac2 = plugin.getConfig().getDouble("missoes.cacador_2.premio", 20000.0);

        inv.setItem(11, criarIcone(Material.IRON_PICKAXE, "§aMinerador Iniciante", "§7Quebre " + objMin1 + " Pedras.", premMin1));
        inv.setItem(15, criarIcone(Material.ZOMBIE_HEAD, "§aCaçador Iniciante", "§7Derrote " + objCac1 + " Zumbis.", premCac1));
        inv.setItem(20, criarIcone(Material.DIAMOND_PICKAXE, "§cMestre da Mineração", "§7Quebre " + objMin2 + " Pedras.", premMin2));
        inv.setItem(24, criarIcone(Material.WITHER_SKELETON_SKULL, "§cExterminador", "§7Derrote " + objCac2 + " Zumbis.", premCac2));

        ItemStack statusMissao;
        if (missoesAtivas.containsKey(player.getUniqueId())) {
            Missao atual = missoesAtivas.get(player.getUniqueId());
            statusMissao = new ItemStack(Material.WRITTEN_BOOK);
            ItemMeta metaStatus = statusMissao.getItemMeta();
            metaStatus.setDisplayName("§e§lSua Missão Atual");
            metaStatus.setLore(Arrays.asList(
                    "§f" + atual.descricao, "",
                    "§7Progresso: §a" + atual.progresso + " §8/ §a" + atual.objetivo,
                    "§7Recompensa: §a" + econ.format(atual.premio)
            ));
            statusMissao.setItemMeta(metaStatus);
        } else {
            statusMissao = new ItemStack(Material.PAPER);
            ItemMeta metaStatus = statusMissao.getItemMeta();
            metaStatus.setDisplayName("§c§lNenhuma Missão Ativa");
            metaStatus.setLore(Arrays.asList("§7Você não tem nenhuma", "§7missão em andamento.", "", "§eEscolha uma acima!"));
            statusMissao.setItemMeta(metaStatus);
        }
        inv.setItem(31, statusMissao);

        ItemStack voltar = new ItemStack(Material.ARROW);
        ItemMeta metaVoltar = voltar.getItemMeta();
        metaVoltar.setDisplayName("§c§lVoltar ao Menu Principal");
        voltar.setItemMeta(metaVoltar);
        inv.setItem(35, voltar); // Posição atual do seu botão voltar

        player.openInventory(inv);
    }

    private ItemStack criarIcone(Material mat, String nome, String obj, double premio) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nome);
        meta.setLore(Arrays.asList(obj, "", "§eRecompensa: §a" + econ.format(premio), "§eClique para iniciar!"));
        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // LÓGICA DE CLIQUES
    // ==========================================
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack itemClicado = event.getCurrentItem();

        if (itemClicado == null || itemClicado.getType() == Material.AIR) return;

        if (event.getView().getTitle().equals("§8Quadro de Missões")) {
            event.setCancelled(true);

            if (itemClicado.getType() == Material.ARROW) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.performCommand("menu");
                return;
            }

            if (itemClicado.getType() == Material.PAPER || itemClicado.getType() == Material.WRITTEN_BOOK) return;

            if (missoesAtivas.containsKey(player.getUniqueId())) {
                Missao atual = missoesAtivas.get(player.getUniqueId());
                player.sendMessage("§cVocê já tem uma missão ativa: " + atual.descricao);
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            // Lê as configs novamente para passar para a missão
            int objMin1 = plugin.getConfig().getInt("missoes.minerador_1.objetivo", 64);
            double premMin1 = plugin.getConfig().getDouble("missoes.minerador_1.premio", 500.0);
            int objCac1 = plugin.getConfig().getInt("missoes.cacador_1.objetivo", 20);
            double premCac1 = plugin.getConfig().getDouble("missoes.cacador_1.premio", 650.0);
            int objMin2 = plugin.getConfig().getInt("missoes.minerador_2.objetivo", 500);
            double premMin2 = plugin.getConfig().getDouble("missoes.minerador_2.premio", 5000.0);
            int objCac2 = plugin.getConfig().getInt("missoes.cacador_2.objetivo", 100);
            double premCac2 = plugin.getConfig().getDouble("missoes.cacador_2.premio", 20000.0);

            int slot = event.getSlot();
            switch (slot) {
                case 11: aceitarMissao(player, new Missao("QUEBRAR_PEDRA", objMin1, "Quebre " + objMin1 + " blocos de Pedra.", premMin1)); break;
                case 15: aceitarMissao(player, new Missao("MATAR_ZUMBI", objCac1, "Derrote " + objCac1 + " Zumbis.", premCac1)); break;
                case 20: aceitarMissao(player, new Missao("QUEBRAR_PEDRA", objMin2, "Quebre " + objMin2 + " blocos de Pedra.", premMin2)); break;
                case 24: aceitarMissao(player, new Missao("MATAR_ZUMBI", objCac2, "Derrote " + objCac2 + " Zumbis.", premCac2)); break;
            }
        }
    }

    private void aceitarMissao(Player player, Missao missao) {
        missoesAtivas.put(player.getUniqueId(), missao);
        salvarMissao(player.getUniqueId()); // Salva ao aceitar
        player.sendMessage("§aNova missão aceita: §f" + missao.descricao);
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
    }

    // ==========================================
    // PROGRESSO E PAGAMENTO
    // ==========================================
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Missao missao = missoesAtivas.get(player.getUniqueId());
        if (missao != null && missao.tipo.equals("QUEBRAR_PEDRA")) {
            Material bloco = event.getBlock().getType();
            if (bloco == Material.STONE || bloco == Material.COBBLESTONE || bloco == Material.DEEPSLATE) {
                adicionarProgresso(player, missao);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            Missao missao = missoesAtivas.get(player.getUniqueId());
            if (missao != null && missao.tipo.equals("MATAR_ZUMBI")) {
                EntityType tipoMob = event.getEntityType();
                if (tipoMob == EntityType.ZOMBIE || tipoMob == EntityType.ZOMBIE_VILLAGER || tipoMob == EntityType.HUSK || tipoMob == EntityType.DROWNED) {
                    adicionarProgresso(player, missao);
                }
            }
        }
    }

    private void adicionarProgresso(Player player, Missao missao) {
        missao.progresso++;
        if (missao.progresso < missao.objetivo) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§a[Missão] Progresso: " + missao.progresso + "/" + missao.objetivo));
        }

        if (missao.progresso >= missao.objetivo) {
            missoesAtivas.remove(player.getUniqueId());
            salvarMissao(player.getUniqueId()); // Limpa do arquivo

            econ.depositPlayer(player, missao.premio);

            player.sendMessage("§a🎉 Você completou a missão: §f" + missao.descricao);
            player.sendMessage("§eE recebeu §a" + econ.format(missao.premio) + " §epelo trabalho!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }
}