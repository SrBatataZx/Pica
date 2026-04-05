package srbatata.gamesarelife.sistemas; // Mude para seu pacote exato

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.core.Principal;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SistemaTerrenos implements Listener, CommandExecutor, TabCompleter {

    // Instância Singleton para acesso global
    private static SistemaTerrenos instance;

    private final Principal plugin;
    public final NamespacedKey keyVara;
    private final String urlBanco;

    // [NOVO] Conexão persistente com o banco de dados
    private Connection conexao;

    private static final List<String> SUBCOMANDOS = List.of("proteger", "desproteger", "adicionar", "remover", "membros", "listar", "renomear", "ajuda");

    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private final Map<UUID, Integer> cacheBlocosUsados = new HashMap<>();
    private final Map<UUID, Map<Location, Material>> bordasAtivas = new HashMap<>();
    private final List<Terreno> terrenosProtegidos = new ArrayList<>();
    private final Map<UUID, Terreno> jogadorNoTerreno = new HashMap<>();

    private final Map<UUID, String> nomesCache = new ConcurrentHashMap<>();
    private org.bukkit.scheduler.BukkitTask taskBordas;

    private final int LIMITE_BASE = 400;

    public SistemaTerrenos(Principal plugin) {
        // Define a instância ao inicializar
        instance = this;

        this.plugin = plugin;
        this.keyVara = new NamespacedKey(plugin, "vara_protecao");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.urlBanco = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/terrenos.db";

        conectarBanco(); // [MODIFICADO] Inicia a conexão fixa com o banco
        carregarTerrenos();
        iniciarVisualizadorDeBordas();
    }

    // Método para pegar a instância do sistema de terrenos
    public static SistemaTerrenos getInstance() {
        return instance;
    }

    // [NOVO] Método para inicializar o banco de dados e manter a conexão aberta
    private void conectarBanco() {
        try {
            if (conexao != null && !conexao.isClosed()) return;
            conexao = DriverManager.getConnection(urlBanco);
            criarTabela();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Terrenos] Erro ao conectar ao SQLite: " + e.getMessage());
        }
    }

    public void desativar() {
        if (taskBordas != null && !taskBordas.isCancelled()) {
            taskBordas.cancel();
        }
        bordasAtivas.clear();
        pos1.clear();
        pos2.clear();
        cacheBlocosUsados.clear();
        nomesCache.clear();
        jogadorNoTerreno.clear();

        // [NOVO] Fecha a conexão com o banco de dados de forma segura
        try {
            if (conexao != null && !conexao.isClosed()) {
                conexao.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao fechar conexão SQLite: " + e.getMessage());
        }
    }

    private void criarTabela() {
        String sql = """
            CREATE TABLE IF NOT EXISTS terrenos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dono VARCHAR(36) NOT NULL,
                mundo VARCHAR(50) NOT NULL,
                minX INTEGER NOT NULL,
                maxX INTEGER NOT NULL,
                minZ INTEGER NOT NULL,
                maxZ INTEGER NOT NULL,
                amigos TEXT,
                nome VARCHAR(100) NOT NULL
            );
            """;

        // [MODIFICADO] Utiliza a conexão persistente
        try (Statement stmt = conexao.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("[Terrenos] Erro ao criar tabela SQLite: " + e.getMessage());
        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pos1.remove(uuid);
        pos2.remove(uuid);
        jogadorNoTerreno.remove(uuid);
        cacheBlocosUsados.remove(uuid);

        // Limpeza de cache para evitar Memory Leaks reais
        nomesCache.remove(uuid);

        if (bordasAtivas.containsKey(uuid)) {
            for (Location loc : bordasAtivas.get(uuid).keySet()) {
                event.getPlayer().sendBlockChange(loc, loc.getBlock().getBlockData());
            }
            bordasAtivas.remove(uuid);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void aoExplodir(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isBlocoProtegido(block.getLocation()));
    }

    public boolean isBlocoProtegido(Location loc) {
        return getTerrenoLocal(loc) != null;
    }

    private void iniciarVisualizadorDeBordas() {
        this.taskBordas = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack item = player.getInventory().getItemInMainHand();
                boolean segurandoVara = item.getType() == Material.BLAZE_ROD && item.hasItemMeta() &&
                        item.getItemMeta().getPersistentDataContainer().has(keyVara, PersistentDataType.BYTE);

                UUID uuid = player.getUniqueId();
                Map<Location, Material> bordasAntigas = bordasAtivas.getOrDefault(uuid, new HashMap<>());

                if (segurandoVara) {
                    Map<Location, Material> bordasNovas = calcularBordasVisiveis(player);

                    for (Map.Entry<Location, Material> entry : bordasAntigas.entrySet()) {
                        Location loc = entry.getKey();
                        if (!bordasNovas.containsKey(loc) || bordasNovas.get(loc) != entry.getValue()) {
                            player.sendBlockChange(loc, loc.getBlock().getBlockData());
                        }
                    }

                    for (Map.Entry<Location, Material> entry : bordasNovas.entrySet()) {
                        Location loc = entry.getKey();
                        if (!bordasAntigas.containsKey(loc) || bordasAntigas.get(loc) != entry.getValue()) {
                            player.sendBlockChange(loc, Bukkit.createBlockData(entry.getValue()));
                        }
                    }

                    bordasAtivas.put(uuid, bordasNovas);

                    int usados = cacheBlocosUsados.computeIfAbsent(uuid, this::getBlocosUsados);
                    int total = getLimiteTotal(uuid);
                    String cor = (usados >= total) ? "§c" : (usados >= total * 0.8) ? "§e" : "§a";
                    String barra = gerarBarraProgresso(usados, total, 10);
                    player.sendActionBar("§6§lTERRENOS: " + cor + usados + " §8/ " + total + " §8[" + barra + "§8]");

                } else if (!bordasAntigas.isEmpty()) {
                    for (Location loc : bordasAntigas.keySet()) {
                        player.sendBlockChange(loc, loc.getBlock().getBlockData());
                    }
                    bordasAtivas.remove(uuid);
                }
            }
        }, 0L, 10L);
    }

    private Map<Location, Material> calcularBordasVisiveis(Player player) {
        Map<Location, Material> bordas = new HashMap<>();
        String mundo = player.getWorld().getName();
        int pX = player.getLocation().getBlockX();
        int pZ = player.getLocation().getBlockZ();
        int pY = player.getLocation().getBlockY() - 1;
        UUID uuid = player.getUniqueId();

        for (Terreno t : terrenosProtegidos) {
            if (!t.mundo.equals(mundo)) continue;

            if (pX < t.minX - 30 || pX > t.maxX + 30 || pZ < t.minZ - 30 || pZ > t.maxZ + 30) {
                continue;
            }

            Material cor = (t.dono.equals(uuid) || t.amigos.contains(uuid))
                    ? Material.LIME_STAINED_GLASS
                    : Material.RED_STAINED_GLASS;

            for (int x = t.minX; x <= t.maxX; x++) {
                bordas.put(new Location(player.getWorld(), x, pY, t.minZ), cor);
                bordas.put(new Location(player.getWorld(), x, pY, t.maxZ), cor);
            }
            for (int z = t.minZ + 1; z < t.maxZ; z++) {
                bordas.put(new Location(player.getWorld(), t.minX, pY, z), cor);
                bordas.put(new Location(player.getWorld(), t.maxX, pY, z), cor);
            }
        }
        return bordas;
    }

    private String getNomeDonoOtimizado(UUID uuid) {
        return nomesCache.computeIfAbsent(uuid, k -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) return p.getName();

            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            return op.getName() != null ? op.getName() : "Desconhecido";
        });
    }

    @EventHandler
    public void aoMover(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        Terreno terrenoOndeEsta = getTerrenoLocal(event.getTo());
        Terreno terrenoAnterior = jogadorNoTerreno.get(player.getUniqueId());

        if (terrenoOndeEsta != null && terrenoOndeEsta != terrenoAnterior) {
            String nomeDono = getNomeDonoOtimizado(terrenoOndeEsta.dono);
            player.sendActionBar("§6§l[Terrenos] §fVocê entrou no terreno de: §e" + nomeDono);
            player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.5f, 1.5f);
            jogadorNoTerreno.put(player.getUniqueId(), terrenoOndeEsta);
        }
        else if (terrenoOndeEsta == null && terrenoAnterior != null) {
            String nomeDono = getNomeDonoOtimizado(terrenoAnterior.dono);
            player.sendActionBar("§6§l[Terrenos] §fVocê saiu do terreno de: §e" + nomeDono);
            jogadorNoTerreno.remove(player.getUniqueId());
        }
    }

    private void salvarTerrenoAsync(Terreno t) {
        CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO terrenos (id, dono, mundo, minX, maxX, minZ, maxZ, amigos, nome)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET 
                amigos=excluded.amigos, nome=excluded.nome;
                """;

            // [MODIFICADO] Utiliza a conexão persistente
            try (PreparedStatement ps = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                if (t.id == -1) { ps.setNull(1, Types.INTEGER); } else { ps.setInt(1, t.id); }

                ps.setString(2, t.dono.toString());
                ps.setString(3, t.mundo);
                ps.setInt(4, t.minX);
                ps.setInt(5, t.maxX);
                ps.setInt(6, t.minZ);
                ps.setInt(7, t.maxZ);

                List<String> amigosStrList = new ArrayList<>();
                for (UUID amigoId : t.amigos) amigosStrList.add(amigoId.toString());
                ps.setString(8, String.join(",", amigosStrList));
                ps.setString(9, t.nome);

                ps.executeUpdate();

                if (t.id == -1) {
                    try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            t.id = generatedKeys.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Erro ao salvar terreno no banco: " + e.getMessage());
            }
        });
    }

    private void deletarTerrenoAsync(Terreno t) {
        if (t.id == -1) return;

        CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM terrenos WHERE id = ?";
            // [MODIFICADO] Utiliza a conexão persistente
            try (PreparedStatement ps = conexao.prepareStatement(sql)) {
                ps.setInt(1, t.id);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Erro ao deletar terreno no banco: " + e.getMessage());
            }
        });
    }

    private void carregarTerrenos() {
        terrenosProtegidos.clear();

        String sqlSelect = "SELECT * FROM terrenos";
        // [MODIFICADO] Utiliza a conexão persistente
        try (Statement stmt = conexao.createStatement();
             ResultSet rs = stmt.executeQuery(sqlSelect)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                UUID dono = UUID.fromString(rs.getString("dono"));
                String mundo = rs.getString("mundo");
                int minX = rs.getInt("minX");
                int maxX = rs.getInt("maxX");
                int minZ = rs.getInt("minZ");
                int maxZ = rs.getInt("maxZ");
                String nome = rs.getString("nome");

                Set<UUID> amigos = new HashSet<>();
                String amigosStr = rs.getString("amigos");
                if (amigosStr != null && !amigosStr.isEmpty()) {
                    for (String amigoId : amigosStr.split(",")) {
                        try { amigos.add(UUID.fromString(amigoId)); } catch (Exception ignored) {}
                    }
                }

                terrenosProtegidos.add(new Terreno(id, dono, mundo, minX, maxX, minZ, maxZ, amigos, nome));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Terrenos] Erro ao carregar terrenos do SQLite: " + e.getMessage());
        }

        if (plugin.getSalvos().contains("terrenos_salvos")) {
            plugin.getLogger().info("[Terrenos] Iniciando migração do YAML para SQLite...");

            for (String uuidStr : plugin.getSalvos().getConfigurationSection("terrenos_salvos").getKeys(false)) {
                UUID dono;
                try { dono = UUID.fromString(uuidStr); } catch (Exception e) { continue; }

                List<String> lista = plugin.getSalvos().getStringList("terrenos_salvos." + uuidStr);
                for (String dados : lista) {
                    String[] partes = dados.split(";");
                    if (partes.length >= 5) {
                        Set<UUID> amigos = new HashSet<>();
                        if (partes.length >= 6 && !partes[5].isEmpty()) {
                            for (String idAmigo : partes[5].split(",")) {
                                try { amigos.add(UUID.fromString(idAmigo)); } catch (Exception ignored) {}
                            }
                        }
                        String nome = (partes.length >= 7) ? partes[6] : "Sem Nome";

                        try {
                            String mundo = partes[0];
                            int minX = Integer.parseInt(partes[1]);
                            int maxX = Integer.parseInt(partes[2]);
                            int minZ = Integer.parseInt(partes[3]);
                            int maxZ = Integer.parseInt(partes[4]);

                            Terreno t = new Terreno(-1, dono, mundo, minX, maxX, minZ, maxZ, amigos, nome);
                            terrenosProtegidos.add(t);
                            salvarTerrenoAsync(t);
                        } catch (Exception ignored) {}
                    }
                }
            }

            plugin.getSalvos().set("terrenos_salvos", null);
            plugin.saveSalvos();
            plugin.getLogger().info("[Terrenos] Migração concluída com sucesso! YAML limpo.");
        }
    }

    public boolean isDono(Player player, Location loc) {
        Terreno t = getTerrenoLocal(loc);
        return t != null && t.dono.equals(player.getUniqueId());
    }

    private static class Terreno {
        int id = -1;
        UUID dono;
        String mundo;
        int minX, maxX, minZ, maxZ;
        Set<UUID> amigos;
        String nome;

        Terreno(UUID dono, Location p1, Location p2, String nome) {
            this.dono = dono; this.mundo = p1.getWorld().getName();
            this.minX = Math.min(p1.getBlockX(), p2.getBlockX()); this.maxX = Math.max(p1.getBlockX(), p2.getBlockX());
            this.minZ = Math.min(p1.getBlockZ(), p2.getBlockZ()); this.maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
            this.amigos = new HashSet<>();
            this.nome = nome;
        }

        Terreno(int id, UUID dono, String mundo, int minX, int maxX, int minZ, int maxZ, Set<UUID> amigos, String nome) {
            this.id = id; this.dono = dono; this.mundo = mundo; this.minX = minX; this.maxX = maxX;
            this.minZ = minZ; this.maxZ = maxZ; this.amigos = amigos; this.nome = nome;
        }

        boolean contem(Location loc) {
            return loc.getWorld().getName().equals(this.mundo) && loc.getBlockX() >= minX && loc.getBlockX() <= maxX && loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
        }

        int getArea() { return (maxX - minX + 1) * (maxZ - minZ + 1); }

        boolean sobrepoe(Terreno outro) {
            return this.mundo.equals(outro.mundo) && this.minX <= outro.maxX && this.maxX >= outro.minX && this.minZ <= outro.maxZ && this.maxZ >= outro.minZ;
        }
    }

    private int getBlocosUsados(UUID uuid) {
        return terrenosProtegidos.stream()
                .filter(t -> t.dono.equals(uuid))
                .mapToInt(Terreno::getArea)
                .sum();
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
                player.sendMessage("§aÁrea selecionada! Digite §e/terreno proteger §apara confirmar sua área.");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("terreno")) {
            if (args.length == 0) {
                enviarAjuda(player);
                return true;
            }

            UUID uuid = player.getUniqueId();
            String subComando = args[0].toLowerCase();

            switch (subComando) {
                case "proteger" -> comandoProteger(player, uuid, args);
                case "desproteger" -> comandoDesproteger(player, uuid);
                case "adicionar" -> comandoAdicionar(player, uuid, args);
                case "remover" -> comandoRemover(player, uuid, args);
                case "membros" -> comandoMembros(player, uuid);
                case "listar" -> comandoListar(player, uuid, args);
                case "renomear" -> comandoRenomear(player, uuid, args);
                case "ajuda" -> enviarAjuda(player);
                default -> player.sendMessage("§cComando desconhecido. Digite /terreno ajuda.");
            }
        }
        return true;
    }

    private void enviarAjuda(Player player) {
        player.sendMessage(" ");
        player.sendMessage("§6§lSISTEMA DE TERRENOS §8- §fAjuda");
        player.sendMessage("§e/terreno proteger [nome] §8- §fProtege a área e dá um nome.");
        player.sendMessage("§e/terreno desproteger §8- §fDeleta o terreno onde você está.");
        player.sendMessage("§e/terreno renomear <nome> §8- §fMuda o nome do terreno atual.");
        player.sendMessage("§e/terreno adicionar <jogador> §8- §fDá permissão a um amigo.");
        player.sendMessage("§e/terreno remover <jogador> §8- §fRemove a permissão de um amigo.");
        player.sendMessage("§e/terreno membros §8- §fLista quem tem permissão no terreno atual.");
        player.sendMessage("§e/terreno listar §8- §fMostra e teleporta para seus terrenos.");
        player.sendMessage(" ");
    }

    private void comandoProteger(Player player, UUID uuid, String[] args) {
        if (!pos1.containsKey(uuid) || !pos2.containsKey(uuid)) {
            player.sendMessage("§cMarque a Posição 1 e a Posição 2 com a Vara da Proteção primeiro!");
            return;
        }

        Location p1 = pos1.get(uuid);
        Location p2 = pos2.get(uuid);

        if (!Objects.equals(p1.getWorld(), p2.getWorld())) {
            player.sendMessage("§cOs pontos precisam estar no mesmo mundo!");
            return;
        }

        String nomeTerreno = "Meu Terreno";
        if (args.length > 1) {
            nomeTerreno = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replace(";", "");
        }

        Terreno novoTerreno = new Terreno(uuid, p1, p2, nomeTerreno);

        for (Terreno t : terrenosProtegidos) {
            if (novoTerreno.sobrepoe(t)) {
                player.sendMessage("§cVocê não pode proteger aqui! Esta área invade um terreno que já tem dono.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        int areaNova = novoTerreno.getArea();
        int limiteTotal = getLimiteTotal(uuid);
        int blocosUsados = getBlocosUsados(uuid);

        if ((blocosUsados + areaNova) > limiteTotal) {
            player.sendMessage("§cEste terreno custa §e" + areaNova + " blocos§c.");
            player.sendMessage("§cVocê já usa §e" + blocosUsados + "§c de §e" + limiteTotal + "§c disponíveis.");
            player.sendMessage("§aCompre mais limite de blocos no Menu da Loja!");
            return;
        }

        terrenosProtegidos.add(novoTerreno);
        salvarTerrenoAsync(novoTerreno);
        pos1.remove(uuid);
        pos2.remove(uuid);
        cacheBlocosUsados.remove(uuid);

        player.sendMessage("§a🎉 Sucesso! Terreno §e" + nomeTerreno + " §aprotegido.");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    private void comandoRenomear(Player player, UUID uuid, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUso correto: /terreno renomear <novo nome>");
            return;
        }

        Terreno terrenoAtual = getTerrenoLocal(player.getLocation());
        if (!validarDonoTerreno(player, uuid, terrenoAtual)) return;

        String novoNome = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replace(";", "");
        terrenoAtual.nome = novoNome;
        salvarTerrenoAsync(terrenoAtual);

        player.sendMessage("§aO nome do terreno foi alterado para: §e" + novoNome);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
    }

    private void comandoDesproteger(Player player, UUID uuid) {
        Terreno terrenoAtual = getTerrenoLocal(player.getLocation());

        if (terrenoAtual != null) {
            if (!terrenoAtual.dono.equals(uuid)) {
                player.sendMessage("§cVocê não pode deletar o terreno de outro jogador!");
                return;
            }
            int areaDevolvida = terrenoAtual.getArea();
            terrenosProtegidos.remove(terrenoAtual);
            deletarTerrenoAsync(terrenoAtual);
            player.sendMessage("§aVocê deletou este terreno. §e" + areaDevolvida + " blocos §aforam devolvidos ao seu limite!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
        } else {
            player.sendMessage("§cVocê não está dentro de nenhum terreno protegido por você.");
        }
        cacheBlocosUsados.remove(uuid);
    }

    private void comandoAdicionar(Player player, UUID uuid, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUso correto: /terreno adicionar <jogador>");
            return;
        }

        Terreno terrenoAtual = getTerrenoLocal(player.getLocation());
        if (!validarDonoTerreno(player, uuid, terrenoAtual)) return;

        OfflinePlayer amigo = Bukkit.getOfflinePlayer(args[1]);

        if (terrenoAtual.amigos.contains(amigo.getUniqueId())) {
            player.sendMessage("§eEste jogador já é seu amigo neste terreno!");
        } else {
            terrenoAtual.amigos.add(amigo.getUniqueId());
            salvarTerrenoAsync(terrenoAtual);
            player.sendMessage("§aO jogador §f" + amigo.getName() + " §aagora tem permissão para construir aqui!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        }
    }

    private void comandoRemover(Player player, UUID uuid, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUso correto: /terreno remover <jogador>");
            return;
        }

        Terreno terrenoAtual = getTerrenoLocal(player.getLocation());
        if (!validarDonoTerreno(player, uuid, terrenoAtual)) return;

        OfflinePlayer amigo = Bukkit.getOfflinePlayer(args[1]);

        if (terrenoAtual.amigos.contains(amigo.getUniqueId())) {
            terrenoAtual.amigos.remove(amigo.getUniqueId());
            salvarTerrenoAsync(terrenoAtual);
            player.sendMessage("§cO jogador §f" + amigo.getName() + " §cperdeu a permissão neste terreno.");
        } else {
            player.sendMessage("§eEste jogador não está na lista de amigos deste terreno.");
        }
    }

    private void comandoMembros(Player player, UUID uuid) {
        Terreno terrenoAtual = getTerrenoLocal(player.getLocation());

        if (terrenoAtual == null || (!terrenoAtual.dono.equals(uuid) && !terrenoAtual.amigos.contains(uuid))) {
            player.sendMessage("§cVocê precisa estar dentro de um terreno seu (ou que tenha permissão) para ver os membros.");
            return;
        }

        if (terrenoAtual.amigos.isEmpty()) {
            player.sendMessage("§eEste terreno não possui membros adicionados.");
            return;
        }

        player.sendMessage("§6§lMembros deste terreno:");
        for (UUID amigoId : terrenoAtual.amigos) {
            String nome = getNomeDonoOtimizado(amigoId);
            player.sendMessage("§8- §a" + nome);
        }
    }

    private void comandoListar(Player player, UUID uuid, String[] args) {
        List<Terreno> meusTerrenos = terrenosProtegidos.stream()
                .filter(t -> t.dono.equals(uuid))
                .toList();

        if (meusTerrenos.isEmpty()) {
            player.sendMessage("§cVocê não possui terreno.");
            return;
        }

        if (args.length == 1) {
            player.sendMessage("§e§lSeus Terrenos:");
            for (int i = 0; i < meusTerrenos.size(); i++) {
                Terreno t = meusTerrenos.get(i);

                net.md_5.bungee.api.chat.TextComponent mensagem = new net.md_5.bungee.api.chat.TextComponent(
                        "§8- §a" + t.nome + " §f(" + t.mundo + ") "
                );

                net.md_5.bungee.api.chat.TextComponent botaoTp = new net.md_5.bungee.api.chat.TextComponent("§e§l[TELEPORTAR]");
                botaoTp.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/terreno listar " + (i + 1)
                ));

                mensagem.addExtra(botaoTp);
                player.spigot().sendMessage(mensagem);
            }
            return;
        }

        try {
            int index = Integer.parseInt(args[1]) - 1;

            if (index < 0 || index >= meusTerrenos.size()) {
                player.sendMessage("§cTerreno não encontrado.");
                return;
            }

            Terreno t = meusTerrenos.get(index);
            org.bukkit.World mundo = Bukkit.getWorld(t.mundo);

            if (mundo == null) {
                player.sendMessage("§cO mundo desse terreno não está carregado!");
                return;
            }

            int centroX = (t.minX + t.maxX) / 2;
            int centroZ = (t.minZ + t.maxZ) / 2;
            int ySeguro = mundo.getHighestBlockYAt(centroX, centroZ) + 1;

            player.teleportAsync(new Location(mundo, centroX + 0.5, ySeguro, centroZ + 0.5)).thenAccept(sucesso -> {
                if (sucesso) {
                    player.sendMessage("§aWoosh! Teleportado para o Terreno " + (index + 1) + "!");
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                }
            });

        } catch (NumberFormatException e) {
            player.sendMessage("§cUso correto: /terreno listar [numero]");
        }
    }

    private Terreno getTerrenoLocal(Location loc) {
        for (Terreno t : terrenosProtegidos) {
            if (t.contem(loc)) return t;
        }
        return null;
    }

    private boolean validarDonoTerreno(Player player, UUID uuid, Terreno terreno) {
        if (terreno == null) {
            player.sendMessage("§cVocê precisa estar em pé dentro do seu terreno para fazer isso!");
            return false;
        }
        if (!terreno.dono.equals(uuid)) {
            player.sendMessage("§cEste terreno não pertence a você!");
            return false;
        }
        return true;
    }

    private boolean podeMexer(Player player, Location loc) {
        if (player.isOp()) return true;
        Terreno t = getTerrenoLocal(loc);
        if (t != null) {
            return t.dono.equals(player.getUniqueId()) || t.amigos.contains(player.getUniqueId());
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
        if (event.getClickedBlock() == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType().isInteractable()) {
            if (!podeMexer(event.getPlayer(), event.getClickedBlock().getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cEste terreno é protegido!");
            }
        }

        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock().getType() == Material.FARMLAND) {
            if (!podeMexer(event.getPlayer(), event.getClickedBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoAcenderFogo(BlockIgniteEvent event) {
        if (event.getPlayer() != null) {
            if (!podeMexer(event.getPlayer(), event.getBlock().getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cVocê não pode acender fogo neste terreno!");
            }
        } else {
            if (isBlocoProtegido(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQueimarBloco(BlockBurnEvent event) {
        if (isBlocoProtegido(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoEspalharFogo(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE || event.getSource().getType() == Material.SOUL_FIRE) {
            if (isBlocoProtegido(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoUsarBalde(org.bukkit.event.player.PlayerBucketEmptyEvent event) {
        if (!podeMexer(event.getPlayer(), event.getBlockClicked().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode despejar líquidos aqui!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoEncherBalde(org.bukkit.event.player.PlayerBucketFillEvent event) {
        if (!podeMexer(event.getPlayer(), event.getBlockClicked().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode pegar líquidos daqui!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoPistaoEmpurrar(org.bukkit.event.block.BlockPistonExtendEvent event) {
        Terreno terrenoPistao = getTerrenoLocal(event.getBlock().getLocation());
        for (org.bukkit.block.Block bloco : event.getBlocks()) {
            Location destino = bloco.getLocation().add(event.getDirection().getDirection());
            Terreno terrenoDestino = getTerrenoLocal(destino);

            if (terrenoDestino != null && terrenoDestino != terrenoPistao) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoPistaoPuxar(org.bukkit.event.block.BlockPistonRetractEvent event) {
        Terreno terrenoPistao = getTerrenoLocal(event.getBlock().getLocation());
        for (org.bukkit.block.Block bloco : event.getBlocks()) {
            Terreno terrenoAlvo = getTerrenoLocal(bloco.getLocation());
            if (terrenoAlvo != null && terrenoAlvo != terrenoPistao) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoAtacarEntidade(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player atacante) {
            if (!(event.getEntity() instanceof org.bukkit.entity.Monster)) {
                if (!podeMexer(atacante, event.getEntity().getLocation())) {
                    event.setCancelled(true);
                    atacante.sendMessage("§cVocê não pode atacar entidades neste terreno!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoInteragirEntidade(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (!podeMexer(event.getPlayer(), event.getRightClicked().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFluidoEscorrer(org.bukkit.event.block.BlockFromToEvent event) {
        Terreno origem = getTerrenoLocal(event.getBlock().getLocation());
        Terreno destino = getTerrenoLocal(event.getToBlock().getLocation());

        if (origem != destino && destino != null) {
            event.setCancelled(true);
        }
    }

    private String gerarBarraProgresso(int atual, int total, int tamanho) {
        if (total == 0) return "§c" + "■".repeat(tamanho);

        int preenchido = (int) Math.round((double) atual / total * tamanho);
        preenchido = Math.min(preenchido, tamanho);
        int vazio = tamanho - preenchido;

        return "§a" + "■".repeat(preenchido) + "§7" + "■".repeat(vazio);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return SUBCOMANDOS.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        else if (args.length == 2 && (args[0].equalsIgnoreCase("adicionar") || args[0].equalsIgnoreCase("remover"))) {
            return null;
        }

        return new ArrayList<>();
    }
}