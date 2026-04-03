package srbatata.gamesarelife.comandos;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdCarteira implements CommandExecutor {

    private final Economy economia;

    public CmdCarteira(Economy economia) {
        this.economia = economia;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Uso de Pattern Matching (Java 16+) para verificar e instanciar a variável 'jogador' na mesma linha
        if (!(sender instanceof Player jogador)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        // Obtém e exibe o saldo
        double saldo = economia.getBalance(jogador);
        jogador.sendMessage("§aSeu saldo é: §e" + economia.format(saldo));

        return true;
    }
}