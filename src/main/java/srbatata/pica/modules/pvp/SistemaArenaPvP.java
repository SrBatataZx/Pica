package srbatata.pica.modules.pvp;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import srbatata.pica.core.Pica;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SistemaArenaPvP implements Listener, CommandExecutor {

    private final Pica plugin;
    private final Economy economia; // Integração com sua economia
    private File areasFile;
    private FileConfiguration areasConfig;
    private final Map<UUID, UUID> convites = new HashMap<>();

    private final int RAIO_ARENA = 25; // Raio da arena

    public SistemaArenaPvP(Pica plugin, Economy economia) {
        this.plugin = plugin;
        this.economia = economia;
        criarArquivoAreas();
        iniciarEfeitoParticulas();
    }

    private void criarArquivoAreas() {
        areasFile = new File(plugin.getDataFolder(), "areas.yml");
        if (!areasFile.exists()) {
            try { areasFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        areasConfig = YamlConfiguration.loadConfiguration(areasFile);
    }

    private void salvarConfig() {
        try { areasConfig.save(areasFile); } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (args.length == 0) {
            p.sendMessage("§cUse: /pvp <nome>, /pvp accept ou /pvp setarea");
            return true;
        }

        if (args[0].equalsIgnoreCase("setarea")) {
            if (!p.hasPermission("pica.admin")) {
                p.sendMessage(plugin.getMsgSemPermissao());
                return true;
            }
            Location loc = p.getLocation();
            areasConfig.set("arena.world", loc.getWorld().getName());
            areasConfig.set("arena.x", loc.getX());
            areasConfig.set("arena.y", loc.getY());
            areasConfig.set("arena.z", loc.getZ());
            salvarConfig();
            p.sendMessage("§a§l[!] §aCentro da arena PvP definido!");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            aceitarDuelo(p);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || target.equals(p)) {
            p.sendMessage("§cJogador não encontrado.");
            return true;
        }

        convites.put(target.getUniqueId(), p.getUniqueId());
        p.sendMessage("§aDesafio enviado! (30s)");
        target.sendMessage("§6§lPvP §8» §f" + p.getName() + " §ete desafiou! §a/pvp accept");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (convites.containsKey(target.getUniqueId()) && convites.get(target.getUniqueId()).equals(p.getUniqueId())) {
                convites.remove(target.getUniqueId());
            }
        }, 20L * 30);

        return true;
    }

    private void aceitarDuelo(Player p) {
        if (!convites.containsKey(p.getUniqueId())) {
            p.sendMessage("§cSem convites pendentes.");
            return;
        }

        Player autor = Bukkit.getPlayer(convites.get(p.getUniqueId()));
        if (autor == null || !areasConfig.contains("arena.x")) {
            p.sendMessage("§cErro ao iniciar duelo.");
            convites.remove(p.getUniqueId());
            return;
        }

        World world = Bukkit.getWorld(areasConfig.getString("arena.world"));
        Location loc = new Location(world, areasConfig.getDouble("arena.x"), areasConfig.getDouble("arena.y"), areasConfig.getDouble("arena.z"));

        p.teleport(loc);
        autor.teleport(loc);
        p.setHealth(20);
        autor.setHealth(20);

        Bukkit.broadcastMessage("§6§lPvP §8» §f" + autor.getName() + " §ee §f" + p.getName() + " §eestão duelando agora!");
        convites.remove(p.getUniqueId());
    }

    // --- BARREIRA: EMPURRAR PARA DENTRO ---
    @EventHandler
    public void aoMover(PlayerMoveEvent e) {
        Location para = e.getTo();
        if (para == null) return;

        // Se o jogador estiver dentro do mundo da arena
        if (areasConfig.contains("arena.world") && para.getWorld().getName().equals(areasConfig.getString("arena.world"))) {

            // Se ele estiver fora do raio, mas perto o suficiente para ser considerado na arena
            double distSq = Math.pow(para.getX() - areasConfig.getDouble("arena.x"), 2) +
                    Math.pow(para.getZ() - areasConfig.getDouble("arena.z"), 2);

            if (distSq > Math.pow(RAIO_ARENA, 2) && distSq < Math.pow(RAIO_ARENA + 10, 2)) {
                Player p = e.getPlayer();

                // Calcula vetor em direção ao centro
                Location centro = new Location(para.getWorld(), areasConfig.getDouble("arena.x"), para.getY(), areasConfig.getDouble("arena.z"));
                Vector direcaoAoCentro = centro.toVector().subtract(para.toVector()).normalize().multiply(0.5);
                direcaoAoCentro.setY(0.2); // Pequeno pulinho para dentro

                p.setVelocity(direcaoAoCentro);
                p.sendMessage("§c§l[!] §cVocê não pode sair da Arena durante o combate!");
                p.playSound(p.getLocation(), Sound.ENTITY_WANDERING_TRADER_HURT, 1, 1);
            }
        }
    }

    @EventHandler
    public void aoMorrer(PlayerDeathEvent e) {
        Player vitima = e.getEntity();

        // Verifica se a vítima estava dentro da arena
        if (estaNaArena(vitima.getLocation())) {
            Player matador = vitima.getKiller();

            if (matador != null) {
                // 1. Forçar recarregamento da config para garantir que pegou o valor novo
                plugin.reloadConfig();
                double valorRecompensa = plugin.getConfig().getDouble("arena.recompensa", 0.0);

                // 2. Entrega o dinheiro via Vault
                if (valorRecompensa > 0) {
                    economia.depositPlayer(matador, valorRecompensa);

                    // Mensagem DIRETA para o matador (Confirmação de recebimento)
                    matador.sendMessage("§a§l+ $" + valorRecompensa + " §7recebidos pela vitória no PvP!");
                } else {
                    // Log de aviso no console caso o valor seja 0 (ajuda no debug)
                    Bukkit.getLogger().warning("[Pica-PvP] O matador " + matador.getName() + " nao recebeu nada pois a recompensa esta 0 na config!");
                }

                // 3. Anúncio GLOBAL para o servidor
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§6§lPvP §8» §f" + matador.getName() + " §evenceu o duelo contra §f" + vitima.getName() + "§e!");
                if (valorRecompensa > 0) {
                    Bukkit.broadcastMessage("§6§lPvP §8» §ePrêmio entregue: §a$" + valorRecompensa);
                }
                Bukkit.broadcastMessage("");

                matador.playSound(matador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            }

            // Teleporte de volta após 10 ticks (0.5 segundos)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Se a vítima ainda estiver na tela de morte, força o respawn
                if (vitima.isDead()) {
                    vitima.spigot().respawn();
                }

                Location spawn = vitima.getWorld().getSpawnLocation();
                vitima.teleport(spawn);

                if (matador != null && matador.isOnline()) {
                    matador.teleport(spawn);
                }
            }, 10L);
        }
    }

    private void iniciarEfeitoParticulas() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!areasConfig.contains("arena.x")) return;
                World world = Bukkit.getWorld(areasConfig.getString("arena.world"));
                if (world == null) return;

                double cx = areasConfig.getDouble("arena.x"), cy = areasConfig.getDouble("arena.y"), cz = areasConfig.getDouble("arena.z");
                for (int i = 0; i < 360; i += 10) {
                    double angle = i * Math.PI / 180;
                    double x = cx + (RAIO_ARENA * Math.cos(angle)), z = cz + (RAIO_ARENA * Math.sin(angle));
                    world.spawnParticle(Particle.FLAME, new Location(world, x, cy + 1, z), 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void aoQuebrar(BlockBreakEvent e) {
        if (estaNaArena(e.getBlock().getLocation()) && !e.getPlayer().hasPermission("pica.admin")) e.setCancelled(true);
    }

    @EventHandler
    public void aoColocar(BlockPlaceEvent e) {
        if (estaNaArena(e.getBlock().getLocation()) && !e.getPlayer().hasPermission("pica.admin")) e.setCancelled(true);
    }

    private boolean estaNaArena(Location loc) {
        if (!areasConfig.contains("arena.x") || !loc.getWorld().getName().equals(areasConfig.getString("arena.world"))) return false;
        double distSq = Math.pow(loc.getX() - areasConfig.getDouble("arena.x"), 2) + Math.pow(loc.getZ() - areasConfig.getDouble("arena.z"), 2);
        return distSq <= Math.pow(RAIO_ARENA, 2);
    }
}