package srbatata.pica;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoRecarregar implements CommandExecutor {

    private final Pica plugin;

    public ComandoRecarregar(Pica plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verifica se quem digitou é um Player e se ele TEM permissão de OP (Admin)
        if (sender instanceof Player && !sender.isOp()) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }

        // 1. Recarrega a config.yml padrão
        plugin.reloadConfig();

        // 2. Recarrega o salvos.yml
        plugin.recarregarSalvos();

        sender.sendMessage("§a🎉 Todas as configurações do plugin Pica foram recarregadas!");

        if (sender instanceof Player) {
            ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        }

        return true;
    }
}