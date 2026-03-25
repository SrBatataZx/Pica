package srbatata.pica.modules.teleport;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import srbatata.pica.core.PicaPlugin;

public class SistemaSpawn implements CommandExecutor {

    private final PicaPlugin plugin;

    public SistemaSpawn(PicaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("setspawn")) {
            // Em vez de escrever o texto direto, nós puxamos o atalho do plugin!
            if (!player.isOp()) {
                player.sendMessage(plugin.getMsgSemPermissao());
                return true;
            }

            // Guarda a localização no salvos.yml
            plugin.getSalvos().set("spawn_servidor", player.getLocation());
            plugin.saveSalvos();

            player.sendMessage("§aPonto de Spawn definido com sucesso!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            Location spawn = plugin.getSalvos().getLocation("spawn_servidor");

            if (spawn != null) {
                player.teleport(spawn);
                player.sendMessage("§aTeleportado para o Spawn!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            } else {
                // Caso o Admnistrador ainda não tenha usado /setspawn, vai para o spawn do mundo
                player.teleport(player.getWorld().getSpawnLocation());
                player.sendMessage("§aTeleportado para o spawn do mundo!");
            }
            return true;
        }

        return false;
    }
}