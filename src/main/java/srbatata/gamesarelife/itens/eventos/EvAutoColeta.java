package srbatata.gamesarelife.itens.eventos;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import srbatata.gamesarelife.core.Principal;

public class EvAutoColeta implements CommandExecutor {

    private final Principal plugin;

    public EvAutoColeta(Principal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verifica se o jogador é OP ou tem permissão
        if (!sender.isOp()) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.");
            return true;
        }

        // Inverte o valor atual salvo na config
        boolean atual = plugin.getConfig().getBoolean("sistema_coleta_ativa", true);
        plugin.getConfig().set("sistema_coleta_ativa", !atual);
        plugin.saveConfig();

        if (!atual) {
            sender.sendMessage("§a[Principal] O sistema de Auto-Coleta e Lixeira foi ATIVADO.");
        } else {
            sender.sendMessage("§c[Principal] O sistema de Auto-Coleta e Lixeira foi DESATIVADO.");
        }

        return true;
    }
}