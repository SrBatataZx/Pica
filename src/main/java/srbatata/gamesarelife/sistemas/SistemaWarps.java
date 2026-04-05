package srbatata.gamesarelife.sistemas;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import srbatata.gamesarelife.core.Principal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SistemaWarps implements CommandExecutor, TabCompleter {

    private final Principal plugin;
    private File warpsFile;
    private FileConfiguration warpsConfig;

    // Mapa em memória para acesso ultrarrápido aos locais e ao TabComplete
    private final Map<String, Location> warps = new HashMap<>();

    public SistemaWarps(Principal plugin) {
        this.plugin = plugin;
        criarArquivo();
        carregarWarps();
    }

    // --- GERENCIAMENTO DE ARQUIVOS ---
    private void criarArquivo() {
        warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        if (!warpsFile.exists()) {
            try { warpsFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
    }

    private void salvarArquivo() {
        try { warpsConfig.save(warpsFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void carregarWarps() {
        warps.clear();
        if (warpsConfig.getConfigurationSection("warps") == null) return;

        for (String nome : warpsConfig.getConfigurationSection("warps").getKeys(false)) {
            String path = "warps." + nome;
            World mundo = Bukkit.getWorld(warpsConfig.getString(path + ".mundo"));

            if (mundo != null) {
                Location loc = new Location(
                        mundo,
                        warpsConfig.getDouble(path + ".x"),
                        warpsConfig.getDouble(path + ".y"),
                        warpsConfig.getDouble(path + ".z"),
                        (float) warpsConfig.getDouble(path + ".yaw"),
                        (float) warpsConfig.getDouble(path + ".pitch")
                );
                warps.put(nome.toLowerCase(), loc);
            }
        }
    }

    // --- EXECUÇÃO DOS COMANDOS ---
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        // Switch Expressions para lidar com os comandos /warp, /setwarp e /delwarp
        switch (command.getName().toLowerCase()) {
            case "setwarp" -> definirWarp(p, args);
            case "delwarp" -> deletarWarp(p, args);
            case "warp" -> teleportarWarp(p, args);
        }
        return true;
    }

    private void definirWarp(Player p, String[] args) {
        if (!p.hasPermission("gamesarelife.admin")) {
            p.sendMessage(plugin.getMsgSemPermissao());
            return;
        }

        if (args.length == 0) {
            p.sendMessage("§cUso: /setwarp <nome>");
            return;
        }

        String nome = args[0].toLowerCase();
        Location loc = p.getLocation();

        // Salva na memória
        warps.put(nome, loc);

        // Salva no YAML
        String path = "warps." + nome;
        warpsConfig.set(path + ".mundo", loc.getWorld().getName());
        warpsConfig.set(path + ".x", loc.getX());
        warpsConfig.set(path + ".y", loc.getY());
        warpsConfig.set(path + ".z", loc.getZ());
        warpsConfig.set(path + ".yaw", loc.getYaw());
        warpsConfig.set(path + ".pitch", loc.getPitch());
        salvarArquivo();

        p.sendMessage("§aWarp §f" + nome + " §acriada com sucesso no seu local atual!");
    }

    private void deletarWarp(Player p, String[] args) {
        if (!p.hasPermission("gamesarelife.admin")) {
            p.sendMessage(plugin.getMsgSemPermissao());
            return;
        }

        if (args.length == 0) {
            p.sendMessage("§cUso: /delwarp <nome>");
            return;
        }

        String nome = args[0].toLowerCase();

        if (warps.remove(nome) != null) {
            warpsConfig.set("warps." + nome, null); // Remove do YAML
            salvarArquivo();
            p.sendMessage("§aWarp §f" + nome + " §adeletada com sucesso!");
        } else {
            p.sendMessage("§cEsta warp não existe.");
        }
    }

    private void teleportarWarp(Player p, String[] args) {
        if (args.length == 0) {
            p.sendMessage("§cUso: /warp <nome>");
            return;
        }

        String nome = args[0].toLowerCase();
        Location loc = warps.get(nome);

        if (loc == null) {
            p.sendMessage("§cWarp não encontrada.");
            return;
        }

        p.sendMessage("§aTeleportando para a warp §f" + nome + "§a...");

        // Método exclusivo da API Paper: Teleporta de forma assíncrona para não causar travamentos!
        p.teleportAsync(loc).thenAccept(sucesso -> {
            if (!sucesso) {
                p.sendMessage("§cNão foi possível teleportar para este local (obstruído).");
            }
        });
    }

    // --- AUTOCOMPLETE (TAB COMPLETER) ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // Autocomplete apenas para o primeiro argumento do comando
        if (args.length == 1) {
            String comandoNome = command.getName().toLowerCase();

            // Se for /warp ou /delwarp, sugere os nomes das warps salvas
            if (comandoNome.equals("warp") || comandoNome.equals("delwarp")) {
                return warps.keySet().stream()
                        .filter(nome -> nome.startsWith(args[0].toLowerCase()))
                        .toList(); // Java 16+ Stream API toList()
            }

            // Se for /setwarp, mostra uma dica visual
            if (comandoNome.equals("setwarp") && sender.hasPermission("pica.admin")) {
                return List.of("<nome_da_nova_warp>");
            }
        }
        return List.of();
    }
}