package srbatata.pica;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class SistemaTerrenos implements Listener, CommandExecutor {

    private final Pica plugin;
    public final NamespacedKey keyVara;

    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private final List<Terreno> terrenosProtegidos = new ArrayList<>();

    // Para controlar a mensagem de "Quem é o dono" e não floodar o chat
    private final Map<UUID, Terreno> jogadorNoTerreno = new HashMap<>();

    private final int LIMITE_BASE = 400;

    public SistemaTerrenos(Pica plugin) {
        this.plugin = plugin;
        this.keyVara = new NamespacedKey(plugin, "vara_protecao");

        carregarTerrenos();
        iniciarVisualizadorDeBordas();
    }

    // ==========================================
    // 1. CORREÇÃO: ANTI-EXPLOSÃO EM TERRENOS
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void aoExplodir(EntityExplodeEvent event) {
        // Remove da lista de blocos a serem destruídos aqueles que estão em terrenos
        event.blockList().removeIf(block -> isBlocoProtegido(block.getLocation()));
    }

    private boolean isBlocoProtegido(Location loc) {
        for (Terreno t : terrenosProtegidos) {
            if (t.contem(loc)) return true;
        }
        return false;
    }

    // ==========================================
    // 2. CORREÇÃO: MOSTRAR BLOCOS (Bordas)
    // ==========================================
    private void iniciarVisualizadorDeBordas() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() == Material.BLAZE_ROD && item.hasItemMeta() &&
                        item.getItemMeta().getPersistentDataContainer().has(keyVara, PersistentDataType.BYTE)) {

                    mostrarBordasFalsas(player);

                    int usados = getBlocosUsados(player.getUniqueId());
                    int total = getLimiteTotal(player.getUniqueId());
                    String mensagem = "§6§lTERRENOS: §e" + usados + " §8/ §e" + total + " §eblocos";

                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(mensagem));
                }
            }
        }, 0L, 40L); // 2 segundos de delay para não pesar
    }

    private void mostrarBordasFalsas(Player player) {
        UUID uuid = player.getUniqueId();
        for (Terreno t : terrenosProtegidos) {
            if (t.dono.equals(uuid) && t.mundo.equals(player.getWorld().getName())) {
                // Desenha as linhas das bordas no chão (Y do jogador)
                int y = player.getLocation().getBlockY() - 1;

                for (int x = t.minX; x <= t.maxX; x++) {
                    enviarBlocoFalso(player, x, y, t.minZ);
                    enviarBlocoFalso(player, x, y, t.maxZ);
                }
                for (int z = t.minZ; z <= t.maxZ; z++) {
                    enviarBlocoFalso(player, t.minX, y, z);
                    enviarBlocoFalso(player, t.maxX, y, z);
                }
            }
        }
    }

    private void enviarBlocoFalso(Player p, int x, int y, int z) {
        Location loc = new Location(p.getWorld(), x, y, z);
        // Envia Vidro Amarelo apenas para o cliente do jogador (não existe no servidor)
        p.sendBlockChange(loc, Bukkit.createBlockData(Material.YELLOW_STAINED_GLASS));

        // Agendar para resetar o bloco após 3 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            p.sendBlockChange(loc, loc.getBlock().getBlockData());
        }, 60L);
    }

    // ==========================================
    // 3. NOVO: MOSTRAR DONO AO ENTRAR
    // ==========================================
    @EventHandler
    public void aoMover(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        Terreno terrenoOndeEsta = null;

        for (Terreno t : terrenosProtegidos) {
            if (t.contem(event.getTo())) {
                terrenoOndeEsta = t;
                break;
            }
        }

        Terreno terrenoAnterior = jogadorNoTerreno.get(player.getUniqueId());

        // Entrou em um terreno novo
        if (terrenoOndeEsta != null && terrenoOndeEsta != terrenoAnterior) {
            OfflinePlayer dono = Bukkit.getOfflinePlayer(terrenoOndeEsta.dono);
            player.sendMessage("§6§l[Terrenos] §fVocê entrou no terreno de: §e" + dono.getName());
            player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.5f, 1.5f);
            jogadorNoTerreno.put(player.getUniqueId(), terrenoOndeEsta);
        }
        // Saiu do terreno
        else if (terrenoOndeEsta == null && terrenoAnterior != null) {
            player.sendMessage("§6§l[Terrenos] §cVocê saiu de uma área protegida.");
            jogadorNoTerreno.remove(player.getUniqueId());
        }
    }

    // --- REUTILIZANDO O RESTANTE DO SEU CÓDIGO (MATEMÁTICA E COMANDOS) ---

    private void salvarTerrenos() {
        plugin.getSalvos().set("terrenos_salvos", null);
        for (Terreno t : terrenosProtegidos) {
            String path = "terrenos_salvos." + t.dono.toString();
            List<String> lista = plugin.getSalvos().getStringList(path);
            List<String> amigosStrList = new ArrayList<>();
            for (UUID amigoId : t.amigos) amigosStrList.add(amigoId.toString());
            String amigosStr = String.join(",", amigosStrList);
            String dados = t.mundo + ";" + t.minX + ";" + t.maxX + ";" + t.minZ + ";" + t.maxZ + ";" + amigosStr;
            lista.add(dados);
            plugin.getSalvos().set(path, lista);
        }
        plugin.saveSalvos();
    }

    private void carregarTerrenos() {
        terrenosProtegidos.clear();
        if (plugin.getSalvos().contains("terrenos_salvos")) {
            for (String uuidStr : plugin.getSalvos().getConfigurationSection("terrenos_salvos").getKeys(false)) {
                UUID dono = UUID.fromString(uuidStr);
                List<String> lista = plugin.getSalvos().getStringList("terrenos_salvos." + uuidStr);
                for (String dados : lista) {
                    String[] partes = dados.split(";");
                    if (partes.length >= 5) {
                        List<UUID> amigos = new ArrayList<>();
                        if (partes.length == 6 && !partes[5].isEmpty()) {
                            for (String id : partes[5].split(",")) amigos.add(UUID.fromString(id));
                        }
                        terrenosProtegidos.add(new Terreno(dono, partes[0], Integer.parseInt(partes[1]), Integer.parseInt(partes[2]), Integer.parseInt(partes[3]), Integer.parseInt(partes[4]), amigos));
                    }
                }
            }
        }
    }
    public boolean isDono(Player player, Location loc) {
        for (Terreno t : terrenosProtegidos) {
            if (t.contem(loc)) {
                // Retorna true apenas se o UUID do jogador for o UUID do dono do terreno
                return t.dono.equals(player.getUniqueId());
            }
        }
        // Se não houver terreno no local, tecnicamente ele não é o dono de uma área protegida
        return false;
    }

    private static class Terreno {
        UUID dono; String mundo; int minX, maxX, minZ, maxZ; List<UUID> amigos;
        Terreno(UUID dono, Location p1, Location p2) {
            this.dono = dono; this.mundo = p1.getWorld().getName();
            this.minX = Math.min(p1.getBlockX(), p2.getBlockX()); this.maxX = Math.max(p1.getBlockX(), p2.getBlockX());
            this.minZ = Math.min(p1.getBlockZ(), p2.getBlockZ()); this.maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
            this.amigos = new ArrayList<>();
        }
        Terreno(UUID dono, String mundo, int minX, int maxX, int minZ, int maxZ, List<UUID> amigos) {
            this.dono = dono; this.mundo = mundo; this.minX = minX; this.maxX = maxX; this.minZ = minZ; this.maxZ = maxZ; this.amigos = amigos;
        }
        boolean contem(Location loc) {
            return loc.getWorld().getName().equals(this.mundo) && loc.getBlockX() >= minX && loc.getBlockX() <= maxX && loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
        }
        int getArea() { return (maxX - minX + 1) * (maxZ - minZ + 1); }
        boolean sobrepoe(Terreno outro) {
            return this.mundo.equals(outro.mundo) && this.minX <= outro.maxX && this.maxX >= outro.minX && this.minZ <= outro.maxZ && this.maxZ >= outro.minZ;
        }
    }

    // ==========================================
    // SISTEMA DE LIMITES
    // ==========================================
    private int getBlocosUsados(UUID uuid) {
        int usados = 0;
        for (Terreno t : terrenosProtegidos) {
            if (t.dono.equals(uuid)) usados += t.getArea();
        }
        return usados;
    }

    private int getLimiteTotal(UUID uuid) {
        int limiteComprado = plugin.getSalvos().getInt("limites_terreno." + uuid.toString(), 0);
        return LIMITE_BASE + limiteComprado;
    }

    public void adicionarLimite(Player player, int quantidade) {
        String path = "limites_terreno." + player.getUniqueId().toString();
        int limiteAtual = plugin.getSalvos().getInt(path, 0);
        plugin.getSalvos().set(path, limiteAtual + quantidade);
        plugin.saveSalvos();
    }

    // ==========================================
    // SELEÇÃO COM A VARA
    // ==========================================
    @EventHandler
    public void onSelect(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.BLAZE_ROD && item.hasItemMeta() &&
                item.getItemMeta().getPersistentDataContainer().has(keyVara, PersistentDataType.BYTE)) {

            if (event.getClickedBlock() == null) return;
            event.setCancelled(true);

            Location clicada = event.getClickedBlock().getLocation();

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                pos1.put(player.getUniqueId(), clicada);
                player.sendMessage("§d§lPosição 1 §fmarcada em: X: " + clicada.getBlockX() + " Z: " + clicada.getBlockZ());
            }
            else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                pos2.put(player.getUniqueId(), clicada);
                player.sendMessage("§d§lPosição 2 §fmarcada em: X: " + clicada.getBlockX() + " Z: " + clicada.getBlockZ());
            }

            if (pos1.containsKey(player.getUniqueId()) && pos2.containsKey(player.getUniqueId())) {
                player.sendMessage("§aÁrea selecionada! Digite §e/proteger §apara confirmar sua área.");
            }
        }
    }

    // ==========================================
    // COMANDOS DE GERENCIAMENTO DO TERRENO
    // ==========================================
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // COMANDO: /PROTEGER
        if (command.getName().equalsIgnoreCase("proteger")) {
            if (!pos1.containsKey(uuid) || !pos2.containsKey(uuid)) {
                player.sendMessage("§cMarque a Posição 1 e a Posição 2 com a Vara da Proteção primeiro!");
                return true;
            }

            Location p1 = pos1.get(uuid);
            Location p2 = pos2.get(uuid);

            if (!p1.getWorld().equals(p2.getWorld())) {
                player.sendMessage("§cOs pontos precisam estar no mesmo mundo!");
                return true;
            }

            Terreno novoTerreno = new Terreno(uuid, p1, p2);

            for (Terreno t : terrenosProtegidos) {
                if (novoTerreno.sobrepoe(t)) {
                    player.sendMessage("§cVocê não pode proteger aqui! Esta área invade um terreno que já tem dono.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return true;
                }
            }

            int areaNova = novoTerreno.getArea();
            int limiteTotal = getLimiteTotal(uuid);
            int blocosUsados = getBlocosUsados(uuid);

            if ((blocosUsados + areaNova) > limiteTotal) {
                player.sendMessage("§cEste terreno custa §e" + areaNova + " blocos§c.");
                player.sendMessage("§cVocê já usa §e" + blocosUsados + "§c de §e" + limiteTotal + "§c disponíveis.");
                player.sendMessage("§aCompre mais limite de blocos no Menu da Loja!");
                return true;
            }

            terrenosProtegidos.add(novoTerreno);
            pos1.remove(uuid);
            pos2.remove(uuid);
            salvarTerrenos();

            player.sendMessage("§a🎉 Sucesso! Terreno protegido.");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            return true;
        }

        // COMANDO: /DESPROTEGER
        if (command.getName().equalsIgnoreCase("desproteger")) {
            Location locAtual = player.getLocation();
            Terreno terrenoAtual = null;

            for (Terreno t : terrenosProtegidos) {
                if (t.contem(locAtual)) {
                    terrenoAtual = t;
                    break;
                }
            }

            if (terrenoAtual != null) {
                if (!terrenoAtual.dono.equals(uuid)) {
                    player.sendMessage("§cVocê não pode deletar o terreno de outro jogador!");
                    return true;
                }
                int areaDevolvida = terrenoAtual.getArea();
                terrenosProtegidos.remove(terrenoAtual);
                salvarTerrenos();
                player.sendMessage("§aVocê deletou este terreno. §e" + areaDevolvida + " blocos §aforam devolvidos ao seu limite!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
            } else {
                player.sendMessage("§cVocê não está dentro de nenhum terreno protegido por você.");
            }
            return true;
        }

        // ==========================================
        // NOVOS COMANDOS: /ADDAMIGO E /REMOVEAMIGO
        // ==========================================
        if (command.getName().equalsIgnoreCase("addamigo") || command.getName().equalsIgnoreCase("removeamigo")) {
            if (args.length == 0) {
                player.sendMessage("§cUso correto: /" + command.getName().toLowerCase() + " <jogador>");
                return true;
            }

            Location locAtual = player.getLocation();
            Terreno terrenoAtual = null;

            for (Terreno t : terrenosProtegidos) {
                if (t.contem(locAtual)) {
                    terrenoAtual = t;
                    break;
                }
            }

            if (terrenoAtual == null) {
                player.sendMessage("§cVocê precisa estar em pé dentro do seu terreno para fazer isso!");
                return true;
            }

            if (!terrenoAtual.dono.equals(uuid)) {
                player.sendMessage("§cEste terreno não pertence a você!");
                return true;
            }

            // Pega o jogador (mesmo que ele esteja offline)
            OfflinePlayer amigo = Bukkit.getOfflinePlayer(args[0]);

            if (command.getName().equalsIgnoreCase("addamigo")) {
                if (terrenoAtual.amigos.contains(amigo.getUniqueId())) {
                    player.sendMessage("§eEste jogador já é seu amigo neste terreno!");
                } else {
                    terrenoAtual.amigos.add(amigo.getUniqueId());
                    salvarTerrenos();
                    player.sendMessage("§aO jogador §f" + amigo.getName() + " §aagora tem permissão para construir aqui!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                }
            }
            else if (command.getName().equalsIgnoreCase("removeamigo")) {
                if (terrenoAtual.amigos.contains(amigo.getUniqueId())) {
                    terrenoAtual.amigos.remove(amigo.getUniqueId());
                    salvarTerrenos();
                    player.sendMessage("§cO jogador §f" + amigo.getName() + " §cperdeu a permissão neste terreno.");
                } else {
                    player.sendMessage("§eEste jogador não está na lista de amigos deste terreno.");
                }
            }
            return true;
        }
        if (command.getName().equalsIgnoreCase("claimlist")) {
            // 1. Puxa todos os terrenos que pertencem a este jogador
            List<Terreno> meusTerrenos = new ArrayList<>();
            for (Terreno t : terrenosProtegidos) {
                if (t.dono.equals(uuid)) {
                    meusTerrenos.add(t);
                }
            }

            if (meusTerrenos.isEmpty()) {
                player.sendMessage("§cVocê não tem nenhum terreno protegido no momento.");
                return true;
            }

            // 2. Se ele só digitou /claimlist, mostra o menu interativo no chat
            if (args.length == 0) {
                player.sendMessage(" ");
                player.sendMessage("§e§lSeus Terrenos Protegidos:");

                for (int i = 0; i < meusTerrenos.size(); i++) {
                    Terreno t = meusTerrenos.get(i);
                    // Calcula o meio exato do terreno
                    int centroX = (t.minX + t.maxX) / 2;
                    int centroZ = (t.minZ + t.maxZ) / 2;

                    // Cria o texto base
                    net.md_5.bungee.api.chat.TextComponent mensagem = new net.md_5.bungee.api.chat.TextComponent(
                            "§8- §aTerreno " + (i + 1) + " §f(" + t.mundo + " | X: " + centroX + " Z: " + centroZ + ") "
                    );

                    // Cria o botão clicável
                    net.md_5.bungee.api.chat.TextComponent botaoTp = new net.md_5.bungee.api.chat.TextComponent("§e§l[TELEPORTAR]");
                    botaoTp.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                            net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/claimlist " + (i + 1)
                    ));

                    mensagem.addExtra(botaoTp);
                    player.spigot().sendMessage(mensagem);
                }
                player.sendMessage(" ");
                return true;
            }

            // 3. Se o chat executou /claimlist <numero> ao clicar no botão
            try {
                int index = Integer.parseInt(args[0]) - 1;

                if (index < 0 || index >= meusTerrenos.size()) {
                    player.sendMessage("§cTerreno não encontrado.");
                    return true;
                }

                Terreno t = meusTerrenos.get(index);
                org.bukkit.World mundo = Bukkit.getWorld(t.mundo);

                if (mundo == null) {
                    player.sendMessage("§cO mundo desse terreno não está carregado!");
                    return true;
                }

                int centroX = (t.minX + t.maxX) / 2;
                int centroZ = (t.minZ + t.maxZ) / 2;

                // Pega o bloco mais alto para ele não nascer preso debaixo da terra
                int ySeguro = mundo.getHighestBlockYAt(centroX, centroZ) + 1;

                Location locDeTeleporte = new Location(mundo, centroX + 0.5, ySeguro, centroZ + 0.5);

                player.teleport(locDeTeleporte);
                player.sendMessage("§aWoosh! Teleportado para o Terreno " + (index + 1) + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

            } catch (NumberFormatException e) {
                player.sendMessage("§cUso correto: /claimlist [numero]");
            }

            return true;
        }

        return false;
    }

    // ==========================================
    // PROTEÇÃO DOS BLOCOS (Anti-Griefing)
    // ==========================================
    private boolean podeMexer(Player player, Location loc) {
        if (player.isOp()) return true;
        for (Terreno t : terrenosProtegidos) {
            if (t.contem(loc)) {
                // NOVO: Libera acesso se for o dono OU se for amigo!
                return t.dono.equals(player.getUniqueId()) || t.amigos.contains(player.getUniqueId());
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!podeMexer(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não tem permissão neste terreno!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!podeMexer(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não tem permissão neste terreno!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractProtect(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType().isInteractable() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (!podeMexer(event.getPlayer(), event.getClickedBlock().getLocation())) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§cEste terreno é protegido!");
                }
            }
        }
    }
}