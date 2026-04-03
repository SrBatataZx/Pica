package srbatata.gamesarelife.comandos;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdPagar implements CommandExecutor {

    private final Economy economia;

    public CmdPagar(Economy economia) {
        this.economia = economia;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player jogador)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        // Valida se o jogador digitou os argumentos corretamente: /pagar <jogador> <quantia>
        if (args.length != 2) {
            jogador.sendMessage("§cUso correto: /pagar <jogador> <quantia>");
            return true;
        }

        OfflinePlayer alvo = Bukkit.getOfflinePlayer(args[0]);

        // Evita transferência para si mesmo
        if (alvo.getUniqueId().equals(jogador.getUniqueId())) {
            jogador.sendMessage("§cVocê não pode enviar dinheiro para si mesmo.");
            return true;
        }

        double valor;
        try {
            valor = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            jogador.sendMessage("§cPor favor, digite um valor numérico válido.");
            return true;
        }

        // Evita transferências negativas ou zeradas (exploits)
        if (valor <= 0) {
            jogador.sendMessage("§cO valor deve ser maior que zero.");
            return true;
        }

        // Processa a transação usando a API do Vault
        EconomyResponse respostaSaque = economia.withdrawPlayer(jogador, valor);

        if (respostaSaque.transactionSuccess()) {
            economia.depositPlayer(alvo, valor);
            jogador.sendMessage("§aVocê enviou §e" + economia.format(valor) + " §apara §f" + alvo.getName() + "§a.");

            // Notifica o alvo se ele estiver online
            if (alvo.isOnline()) {
                ((Player) alvo).sendMessage("§aVocê recebeu §e" + economia.format(valor) + " §ade §f" + jogador.getName() + "§a.");
            }
        } else {
            jogador.sendMessage("§cVocê não tem saldo suficiente para esta transação.");
        }

        return true;
    }
}