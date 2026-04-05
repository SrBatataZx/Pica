package srbatata.gamesarelife.sistemas.regioes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.block.Block;
import org.bukkit.Color;
import org.bukkit.Particle;
import srbatata.gamesarelife.core.Principal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SistemaRegioes implements Listener, CommandExecutor, TabCompleter {

    private final Principal plugin;
    private File regioesFile;
    private FileConfiguration regioesConfig;

    // Gerenciamento em memória
    private final Map<String, Regiao> regioes = new HashMap<>();
    private final Map<String, Location> locaisDeTeleporte = new HashMap<>(); // Guarda os destinos de teleporte

    // Seleção temporária dos jogadores (Estilo WorldEdit)
    private final Map<UUID, Location> selecaoPos1 = new HashMap<>();
    private final Map<UUID, Location> selecaoPos2 = new HashMap<>();

    // Listas imutáveis para o Autocomplete
    private static final List<String> SUBCOMANDOS_REGIAO = List.of("pos1", "pos2", "criar", "deletar", "flag", "wand", "setteleport");
    private static final List<String> FLAGS_DISPONIVEIS = List.of("pvp", "quebrar", "colocar");

    public SistemaRegioes(Principal plugin) {
        this.plugin = plugin;
        criarArquivo();
        carregarRegioes();
        iniciarTaskVisualizacao();
    }

    // --- GERENCIAMENTO DE ARQUIVOS ---
    private void criarArquivo() {
        regioesFile = new File(plugin.getDataFolder(), "regioes.yml");
        if (!regioesFile.exists()) {
            try { regioesFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        regioesConfig = YamlConfiguration.loadConfiguration(regioesFile);
    }

    private void salvarArquivo() {
        try { regioesConfig.save(regioesFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void carregarRegioes() {
        regioes.clear();
        locaisDeTeleporte.clear();
        if (regioesConfig.getConfigurationSection("regioes") == null) return;

        for (String nome : regioesConfig.getConfigurationSection("regioes").getKeys(false)) {
            String path = "regioes." + nome;
            Regiao regiao = new Regiao(
                    nome,
                    regioesConfig.getString(path + ".mundo"),
                    regioesConfig.getInt(path + ".minX"),
                    regioesConfig.getInt(path + ".minY"),
                    regioesConfig.getInt(path + ".minZ"),
                    regioesConfig.getInt(path + ".maxX"),
                    regioesConfig.getInt(path + ".maxY"),
                    regioesConfig.getInt(path + ".maxZ")
            );

            // Carrega as flags
            if (regioesConfig.getConfigurationSection(path + ".flags") != null) {
                for (String flag : regioesConfig.getConfigurationSection(path + ".flags").getKeys(false)) {
                    regiao.setFlag(flag, regioesConfig.getBoolean(path + ".flags." + flag));
                }
            }
            regioes.put(nome.toLowerCase(), regiao);

            // Carrega o local de teleporte (se existir)
            if (regioesConfig.contains(path + ".teleporte.mundo")) {
                World mundo = Bukkit.getWorld(regioesConfig.getString(path + ".teleporte.mundo"));
                if (mundo != null) {
                    Location loc = new Location(
                            mundo,
                            regioesConfig.getDouble(path + ".teleporte.x"),
                            regioesConfig.getDouble(path + ".teleporte.y"),
                            regioesConfig.getDouble(path + ".teleporte.z"),
                            (float) regioesConfig.getDouble(path + ".teleporte.yaw"),
                            (float) regioesConfig.getDouble(path + ".teleporte.pitch")
                    );
                    locaisDeTeleporte.put(nome.toLowerCase(), loc);
                }
            }
        }
    }

    // --- COMANDOS ---
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (!p.hasPermission("gamesarelife.admin")) {
            p.sendMessage(plugin.getMsgSemPermissao());
            return true;
        }

        if (args.length == 0) {
            p.sendMessage("§cUse: /regiao pos1, /regiao pos2, /regiao criar <nome>, /regiao flag <nome> <flag> <true|false>, /regiao setteleport <nome>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pos1" -> {
                selecaoPos1.put(p.getUniqueId(), p.getLocation());
                p.sendMessage("§d§l[!] §dPosição 1 marcada no bloco em que você está!");
            }
            case "pos2" -> {
                selecaoPos2.put(p.getUniqueId(), p.getLocation());
                p.sendMessage("§d§l[!] §dPosição 2 marcada no bloco em que você está!");
            }
            case "criar" -> criarRegiao(p, args);
            case "deletar" -> deletarRegiao(p, args);
            case "flag" -> definirFlag(p, args);
            case "wand" -> darVarinha(p);
            case "setteleport" -> definirDestinoTeleporte(p, args);
            default -> p.sendMessage("§cComando de região desconhecido.");
        }
        return true;
    }

    // --- AUTOCOMPLETE (TAB COMPLETER) ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p) || !p.hasPermission("gamesarelife.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return SUBCOMANDOS_REGIAO.stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "criar" -> List.of("<nome_da_regiao>");
                // Adicione o "deletar" aqui nos cases que listam as regiões existentes
                case "flag", "setteleport", "deletar" -> regioes.keySet().stream()
                        .filter(nome -> nome.startsWith(args[1].toLowerCase()))
                        .toList();
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            return FLAGS_DISPONIVEIS.stream()
                    .filter(flag -> flag.startsWith(args[2].toLowerCase()))
                    .toList();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("flag")) {
            return List.of("true", "false").stream()
                    .filter(bool -> bool.startsWith(args[3].toLowerCase()))
                    .toList();
        }

        return List.of();
    }

    // --- LÓGICA DOS COMANDOS ---
    private void criarRegiao(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUso: /regiao criar <nome>");
            return;
        }

        Location pos1 = selecaoPos1.get(p.getUniqueId());
        Location pos2 = selecaoPos2.get(p.getUniqueId());

        if (pos1 == null || pos2 == null) {
            p.sendMessage("§cVocê precisa marcar a pos1 e pos2 primeiro!");
            return;
        }

        if (pos1.getWorld() != pos2.getWorld()) {
            p.sendMessage("§cAs posições precisam estar no mesmo mundo!");
            return;
        }

        String nome = args[1].toLowerCase();
        Regiao novaRegiao = new Regiao(nome, pos1.getWorld().getName(),
                pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());

        novaRegiao.setFlag("pvp", false);
        novaRegiao.setFlag("quebrar", false);
        novaRegiao.setFlag("colocar", false);

        regioes.put(nome, novaRegiao);
        salvarRegiaoNoYAML(novaRegiao);

        p.sendMessage("§aRegião §f" + nome + " §acriada com sucesso com proteções ativadas!");
        selecaoPos1.remove(p.getUniqueId());
        selecaoPos2.remove(p.getUniqueId());
    }
    private void deletarRegiao(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUso: /regiao deletar <nome>");
            return;
        }

        String nome = args[1].toLowerCase();

        // Verifica se a região existe
        if (!regioes.containsKey(nome)) {
            p.sendMessage("§cNão foi possível encontrar a região §f" + nome + "§c.");
            return;
        }

        // 1. Remove da memória principal
        regioes.remove(nome);

        // 2. Remove da memória de teleportes (caso tivesse um Void Loop)
        locaisDeTeleporte.remove(nome);

        // 3. Remove completamente do arquivo YML
        regioesConfig.set("regioes." + nome, null);
        salvarArquivo();

        p.sendMessage("§a§l[!] §aA região §f" + nome + " §afoi deletada e desprotegida com sucesso!");
    }

    private void definirFlag(Player p, String[] args) {
        if (args.length < 4) {
            p.sendMessage("§cUso: /regiao flag <nome> <flag> <true|false>");
            return;
        }

        String nome = args[1].toLowerCase();
        String flag = args[2].toLowerCase();
        boolean valor = Boolean.parseBoolean(args[3]);

        Regiao regiao = regioes.get(nome);
        if (regiao == null) {
            p.sendMessage("§cRegião não encontrada.");
            return;
        }

        regiao.setFlag(flag, valor);
        regioesConfig.set("regioes." + nome + ".flags." + flag, valor);
        salvarArquivo();

        p.sendMessage("§aFlag §f" + flag + " §ada região §f" + nome + " §aalterada para §f" + valor);
    }

    private void definirDestinoTeleporte(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§cUso: /regiao setteleport <nome da regiao>");
            return;
        }

        String nome = args[1].toLowerCase();
        if (!regioes.containsKey(nome)) {
            p.sendMessage("§cEsta região não existe!");
            return;
        }

        Location loc = p.getLocation();
        locaisDeTeleporte.put(nome, loc);

        String path = "regioes." + nome + ".teleporte";
        regioesConfig.set(path + ".mundo", loc.getWorld().getName());
        regioesConfig.set(path + ".x", loc.getX());
        regioesConfig.set(path + ".y", loc.getY());
        regioesConfig.set(path + ".z", loc.getZ());
        regioesConfig.set(path + ".yaw", loc.getYaw());
        regioesConfig.set(path + ".pitch", loc.getPitch());
        salvarArquivo();

        p.sendMessage("§aPonto de teleporte da região §f" + nome + " §adefinido no seu local atual!");
    }

    private void salvarRegiaoNoYAML(Regiao r) {
        String path = "regioes." + r.getNome();
        regioesConfig.set(path + ".mundo", r.getMundo());
        regioesConfig.set(path + ".minX", r.getMinX());
        regioesConfig.set(path + ".minY", r.getMinY());
        regioesConfig.set(path + ".minZ", r.getMinZ());
        regioesConfig.set(path + ".maxX", r.getMaxX());
        regioesConfig.set(path + ".maxY", r.getMaxY());
        regioesConfig.set(path + ".maxZ", r.getMaxZ());

        for (Map.Entry<String, Boolean> entry : r.getFlags().entrySet()) {
            regioesConfig.set(path + ".flags." + entry.getKey(), entry.getValue());
        }
        salvarArquivo();
    }

    private void darVarinha(Player p) {
        ItemStack varinha = new ItemStack(Material.STICK);
        ItemMeta meta = varinha.getItemMeta();

        meta.setDisplayName("§d§lVarinha de Regiões");
        meta.setLore(List.of(
                "§7Utilize esta ferramenta para marcar",
                "§7os pontos da sua nova região.",
                "",
                "§eBotão Esquerdo: §fMarcar Posição 1",
                "§eBotão Direito: §fMarcar Posição 2"
        ));

        NamespacedKey key = new NamespacedKey(plugin, "varinha_admin");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        varinha.setItemMeta(meta);
        p.getInventory().addItem(varinha);
        p.sendMessage("§a§l[!] §aVocê recebeu a Varinha de Regiões!");
    }

    // --- SISTEMA DE VERIFICAÇÃO DE EVENTOS (LISTENERS) ---
    private Regiao obterRegiao(Location loc) {
        for (Regiao r : regioes.values()) {
            if (r.contem(loc)) return r;
        }
        return null;
    }

    @EventHandler
    public void aoUsarVarinha(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        if (!p.hasPermission("gamesarelife.admin")) return;

        ItemStack item = e.getItem();
        if (item == null || item.getItemMeta() == null) return;

        NamespacedKey key = new NamespacedKey(plugin, "varinha_admin");
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;

        Block blocoClicado = e.getClickedBlock();
        if (blocoClicado == null) return;

        e.setCancelled(true);

        Action acao = e.getAction();

        switch (acao) {
            case LEFT_CLICK_BLOCK -> {
                selecaoPos1.put(p.getUniqueId(), blocoClicado.getLocation());
                p.sendMessage("§d§l[!] §dPosição 1 definida em: §f" +
                        blocoClicado.getX() + ", " + blocoClicado.getY() + ", " + blocoClicado.getZ());
            }
            case RIGHT_CLICK_BLOCK -> {
                selecaoPos2.put(p.getUniqueId(), blocoClicado.getLocation());
                p.sendMessage("§d§l[!] §dPosição 2 definida em: §f" +
                        blocoClicado.getX() + ", " + blocoClicado.getY() + ", " + blocoClicado.getZ());
            }
            default -> {}
        }
    }

    @EventHandler
    public void aoMover(PlayerMoveEvent e) {
        // Otimização: ignora pequenos movimentos de câmera, processando apenas mudança real de blocos
        if (!e.hasChangedBlock()) return;

        Location to = e.getTo();
        Regiao r = obterRegiao(to);

        if (r != null && locaisDeTeleporte.containsKey(r.getNome())) {
            Player p = e.getPlayer();
            Location destino = locaisDeTeleporte.get(r.getNome());

            // Teleporta de forma assíncrona (recurso otimizado do Paper)
            p.teleportAsync(destino);
        }
    }
    // --- SISTEMA VISUAL DE PARTÍCULAS (ESP) ---
    private void iniciarTaskVisualizacao() {
        // Roda a cada 10 ticks (meio segundo). É o tempo ideal para a partícula não piscar e não causar lag.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("gamesarelife.admin")) continue;

                ItemStack item = p.getInventory().getItemInMainHand();
                if (item == null || !item.hasItemMeta()) continue;

                // Verifica se o item na mão é a varinha inteligente
                NamespacedKey key = new NamespacedKey(plugin, "varinha_admin");
                if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {

                    // 1. Desenha as regiões JÁ CRIADAS (em Verde Limão)
                    Particle.DustOptions corSalva = new Particle.DustOptions(Color.fromRGB(50, 255, 50), 1.0F);
                    for (Regiao r : regioes.values()) {
                        exibirBordas(p, r, corSalva);
                    }

                    // 2. Desenha a região TEMPORÁRIA que o admin está marcando (em Vermelho)
                    Location pos1 = selecaoPos1.get(p.getUniqueId());
                    Location pos2 = selecaoPos2.get(p.getUniqueId());

                    if (pos1 != null && pos2 != null && pos1.getWorld() == pos2.getWorld()) {
                        // Cria uma região falsa apenas para calcular os limites geométricos
                        Regiao regiaoPendente = new Regiao("temp", pos1.getWorld().getName(),
                                pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                                pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());

                        Particle.DustOptions corPendente = new Particle.DustOptions(Color.fromRGB(255, 50, 50), 1.0F);
                        exibirBordas(p, regiaoPendente, corPendente);
                    }
                }
            }
        }, 0L, 10L);
    }

    private void exibirBordas(Player p, Regiao r, Particle.DustOptions cor) {
        World w = Bukkit.getWorld(r.getMundo());
        if (w == null || !p.getWorld().equals(w)) return;

        double pX = p.getLocation().getX();
        double pZ = p.getLocation().getZ();

        // OTIMIZAÇÃO: Só mostra as partículas se a região estiver a menos de 100 blocos do admin.
        // Isso evita que o servidor calcule milhares de partículas de regiões que estão longe.
        if (pX < r.getMinX() - 100 || pX > r.getMaxX() + 100 || pZ < r.getMinZ() - 100 || pZ > r.getMaxZ() + 100) {
            return;
        }

        // Limites (Adicionamos +1.0 no máximo para a linha contornar o bloco por fora perfeitamente)
        double minX = r.getMinX();
        double minY = r.getMinY();
        double minZ = r.getMinZ();
        double maxX = r.getMaxX() + 1.0;
        double maxY = r.getMaxY() + 1.0;
        double maxZ = r.getMaxZ() + 1.0;

        // Espaço entre cada pontinho da partícula. 2.0 cria uma linha pontilhada estilosa e super leve!
        double espaco = 2.0;

        // --- DESENHO DA GEOMETRIA (12 Arestas do Prisma) ---
        // Arestas de Baixo (Chão)
        for (double x = minX; x <= maxX; x += espaco) {
            p.spawnParticle(Particle.DUST, x, minY, minZ, 1, cor);
            p.spawnParticle(Particle.DUST, x, minY, maxZ, 1, cor);
        }
        for (double z = minZ; z <= maxZ; z += espaco) {
            p.spawnParticle(Particle.DUST, minX, minY, z, 1, cor);
            p.spawnParticle(Particle.DUST, maxX, minY, z, 1, cor);
        }

        // Arestas de Cima (Teto)
        for (double x = minX; x <= maxX; x += espaco) {
            p.spawnParticle(Particle.DUST, x, maxY, minZ, 1, cor);
            p.spawnParticle(Particle.DUST, x, maxY, maxZ, 1, cor);
        }
        for (double z = minZ; z <= maxZ; z += espaco) {
            p.spawnParticle(Particle.DUST, minX, maxY, z, 1, cor);
            p.spawnParticle(Particle.DUST, maxX, maxY, z, 1, cor);
        }

        // Arestas Verticais (As 4 Pilastras)
        for (double y = minY; y <= maxY; y += espaco) {
            p.spawnParticle(Particle.DUST, minX, y, minZ, 1, cor);
            p.spawnParticle(Particle.DUST, maxX, y, minZ, 1, cor);
            p.spawnParticle(Particle.DUST, minX, y, maxZ, 1, cor);
            p.spawnParticle(Particle.DUST, maxX, y, maxZ, 1, cor);
        }
    }

    @EventHandler
    public void aoQuebrarBloco(BlockBreakEvent e) {
        Regiao r = obterRegiao(e.getBlock().getLocation());
        if (r != null) {
            if (!r.getFlag("quebrar", false) && !e.getPlayer().hasPermission("gamesarelife.admin")) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§cVocê não pode quebrar blocos nesta área!");
            }
        }
    }

    @EventHandler
    public void aoColocarBloco(BlockPlaceEvent e) {
        Regiao r = obterRegiao(e.getBlock().getLocation());
        if (r != null) {
            if (!r.getFlag("colocar", false) && !e.getPlayer().hasPermission("gamesarelife.admin")) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§cVocê não pode colocar blocos nesta área!");
            }
        }
    }

    @EventHandler
    public void aoAtacar(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player vitima && e.getDamager() instanceof Player atacante) {
            Regiao r = obterRegiao(vitima.getLocation());
            if (r != null) {
                if (!r.getFlag("pvp", false)) {
                    e.setCancelled(true);
                    atacante.sendMessage("§cO PvP está desativado nesta área!");
                }
            }
        }
    }
}