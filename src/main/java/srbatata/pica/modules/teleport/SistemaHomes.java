package srbatata.pica.modules.teleport;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import srbatata.pica.core.Pica;

import java.util.Set;

public class SistemaHomes implements CommandExecutor {

    private final Pica plugin;

    public SistemaHomes(Pica plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        String uuid = player.getUniqueId().toString();

        if (command.getName().equalsIgnoreCase("sethome")) {
            if (args.length == 0) {
                player.sendMessage("§cUso correto: /sethome <nome>");
                return true;
            }
            String nomeHome = args[0].toLowerCase();

            int quantidadeAtual = 0;
            if (plugin.getSalvos().contains("homes." + uuid)) {
                quantidadeAtual = plugin.getSalvos().getConfigurationSection("homes." + uuid).getKeys(false).size();
            }

            if (quantidadeAtual >= 3 && !plugin.getSalvos().contains("homes." + uuid + "." + nomeHome)) {
                player.sendMessage("§cVocê já atingiu o limite máximo de 3 homes!");
                return true;
            }

            // Salva no novo arquivo salvos.yml
            plugin.getSalvos().set("homes." + uuid + "." + nomeHome, player.getLocation());
            plugin.saveSalvos();
            player.sendMessage("§aHome §f" + nomeHome + " §asalva com sucesso!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("home")) {
            if (args.length == 0) {
                if (plugin.getSalvos().contains("homes." + uuid)) {
                    Set<String> listaHomes = plugin.getSalvos().getConfigurationSection("homes." + uuid).getKeys(false);
                    player.sendMessage("§eSuas homes: §f" + String.join(", ", listaHomes));
                } else {
                    player.sendMessage("§cVocê não tem nenhuma home salva.");
                }
                return true;
            }

            String nomeHome = args[0].toLowerCase();
            Location loc = plugin.getSalvos().getLocation("homes." + uuid + "." + nomeHome);

            if (loc != null) {
                player.teleport(loc);
                player.sendMessage("§aTeleportado para a home §f" + nomeHome + "§a!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            } else {
                player.sendMessage("§cHome não encontrada!");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("delhome")) {
            if (args.length == 0) {
                player.sendMessage("§cUso correto: /delhome <nome>");
                return true;
            }
            String nomeHome = args[0].toLowerCase();

            if (plugin.getSalvos().contains("homes." + uuid + "." + nomeHome)) {
                plugin.getSalvos().set("homes." + uuid + "." + nomeHome, null);
                plugin.saveSalvos();
                player.sendMessage("§aHome §f" + nomeHome + " §adeletada com sucesso!");
            } else {
                player.sendMessage("§cHome não encontrada!");
            }
            return true;
        }

        return false;
    }
}