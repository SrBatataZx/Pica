package srbatata.pica;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoMoney implements CommandExecutor {

    private final Economy economia;

    // Recebemos a economia no construtor
    public ComandoMoney(Economy economia) {
        this.economia = economia;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Verifica se quem enviou o comando é um jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        Player jogador = (Player) sender;

        // Comando: /money (Vê o próprio saldo)
        if (args.length == 0) {
            double saldo = economia.getBalance(jogador);
            jogador.sendMessage(ChatColor.GREEN + "Seu saldo é: " + ChatColor.YELLOW + economia.format(saldo));
            return true;
        }

        // Comando: /money <jogador> (Vê o saldo de outro jogador)
        if (args.length == 1) {
            OfflinePlayer alvo = Bukkit.getOfflinePlayer(args[0]);
            double saldoAlvo = economia.getBalance(alvo);
            jogador.sendMessage(ChatColor.GREEN + "Saldo de " + alvo.getName() + ": " + ChatColor.YELLOW + economia.format(saldoAlvo));
            return true;
        }

        // Comando: /money pay <jogador> <quantia> (Envia dinheiro)
        if (args.length == 3 && args[0].equalsIgnoreCase("pay")) {
            OfflinePlayer alvo = Bukkit.getOfflinePlayer(args[1]);

            // Impede o jogador de enviar dinheiro para si mesmo
            if (alvo.getUniqueId().equals(jogador.getUniqueId())) {
                jogador.sendMessage(ChatColor.RED + "Você não pode enviar dinheiro para si mesmo.");
                return true;
            }

            double valor;
            try {
                valor = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                jogador.sendMessage(ChatColor.RED + "Por favor, digite um valor numérico válido.");
                return true;
            }

            if (valor <= 0) {
                jogador.sendMessage(ChatColor.RED + "O valor deve ser maior que zero.");
                return true;
            }

            // Tenta remover o dinheiro de quem enviou
            EconomyResponse respostaSaque = economia.withdrawPlayer(jogador, valor);

            if (respostaSaque.transactionSuccess()) {
                // Se o saque deu certo, deposita na conta do alvo
                economia.depositPlayer(alvo, valor);
                jogador.sendMessage(ChatColor.GREEN + "Você enviou " + economia.format(valor) + " para " + alvo.getName() + ".");

                // Se o alvo estiver online, avisa ele também
                if (alvo.isOnline()) {
                    ((Player) alvo).sendMessage(ChatColor.GREEN + "Você recebeu " + economia.format(valor) + " de " + jogador.getName() + ".");
                }
            } else {
                // Se falhou (provavelmente por falta de saldo), avisa o jogador
                jogador.sendMessage(ChatColor.RED + "Você não tem saldo suficiente. " + respostaSaque.errorMessage);
            }
            return true;
        }

        // Se o jogador digitar algo errado, mostra o uso correto
        jogador.sendMessage(ChatColor.RED + "Uso correto:");
        jogador.sendMessage(ChatColor.RED + "/money");
        jogador.sendMessage(ChatColor.RED + "/money <jogador>");
        jogador.sendMessage(ChatColor.RED + "/money pay <jogador> <quantia>");
        return true;
    }
}
