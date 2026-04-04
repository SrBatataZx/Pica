package srbatata.gamesarelife.sistemas.comandos;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.core.Principal;

public class ComandoSetLoja implements CommandExecutor {

    private final Principal plugin;
    private final NamespacedKey licencaKey;

    public ComandoSetLoja(Principal plugin) {
        this.plugin = plugin;
        this.licencaKey = new NamespacedKey(plugin, "licenca_terreno_loja");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        // Verifica se o jogador possui a licença necessária
        if (!player.getPersistentDataContainer().has(licencaKey, PersistentDataType.BYTE)) {
            player.sendMessage("§c§l[!] §cVocê precisa de uma Licença de Loja para definir um spawn!");
            return true;
        }

        // Salva a localização e o UUID do dono no salvos.yml
        String path = "lojas_publicas." + player.getUniqueId();
        plugin.getSalvos().set(path + ".location", player.getLocation());
        plugin.getSalvos().set(path + ".owner_name", player.getName());
        plugin.saveSalvos();

        player.sendMessage("§a🎉 Spawn da sua loja definido com sucesso!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        return true;
    }
}
