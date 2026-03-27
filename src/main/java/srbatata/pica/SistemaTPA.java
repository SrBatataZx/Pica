package srbatata.pica;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SistemaTPA implements CommandExecutor {

    // Guarda quem pediu TPA para quem. [Alvo -> [Nome Solicitante -> UUID Solicitante]]
    private final Map<UUID, Map<String, UUID>> pedidosTpa = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("tpa")) {
            if (args.length == 0) {
                player.sendMessage("§cUso: /tpa <jogador>");
                return true;
            }

            Player alvo = Bukkit.getPlayerExact(args[0]);
            if (alvo == null || !alvo.isOnline()) {
                player.sendMessage("§cJogador não encontrado ou offline.");
                return true;
            }

            if (alvo.equals(player)) {
                player.sendMessage("§cVocê não pode enviar TPA para si mesmo!");
                return true;
            }

            // Registra o pedido
            pedidosTpa.putIfAbsent(alvo.getUniqueId(), new HashMap<>());
            pedidosTpa.get(alvo.getUniqueId()).put(player.getName().toLowerCase(), player.getUniqueId());

            player.sendMessage("§aPedido de TPA enviado para §f" + alvo.getName() + "§a.");
            alvo.sendMessage("§eO jogador §f" + player.getName() + " §equer se teleportar até você.");
            alvo.sendMessage("§eDigite §a/tpaccept " + player.getName() + " §eou §c/tpadeny " + player.getName());
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpaccept")) {
            if (args.length == 0) {
                player.sendMessage("§cUso: /tpaccept <jogador>");
                return true;
            }

            String nomeSolicitante = args[0].toLowerCase();
            Map<String, UUID> meusPedidos = pedidosTpa.get(player.getUniqueId());

            if (meusPedidos != null && meusPedidos.containsKey(nomeSolicitante)) {
                Player solicitante = Bukkit.getPlayer(meusPedidos.get(nomeSolicitante));

                if (solicitante != null && solicitante.isOnline()) {
                    solicitante.teleport(player.getLocation());
                    solicitante.sendMessage("§a" + player.getName() + " aceitou seu pedido de TPA!");
                    player.sendMessage("§aVocê aceitou o pedido de TPA de " + solicitante.getName() + "!");
                    solicitante.playSound(solicitante.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                } else {
                    player.sendMessage("§cEsse jogador não está mais online.");
                }

                meusPedidos.remove(nomeSolicitante); // Limpa o pedido
            } else {
                player.sendMessage("§cVocê não tem nenhum pedido pendente de " + args[0] + ".");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpadeny")) {
            if (args.length == 0) {
                player.sendMessage("§cUso: /tpadeny <jogador>");
                return true;
            }

            String nomeSolicitante = args[0].toLowerCase();
            Map<String, UUID> meusPedidos = pedidosTpa.get(player.getUniqueId());

            if (meusPedidos != null && meusPedidos.containsKey(nomeSolicitante)) {
                Player solicitante = Bukkit.getPlayer(meusPedidos.get(nomeSolicitante));

                if (solicitante != null && solicitante.isOnline()) {
                    solicitante.sendMessage("§c" + player.getName() + " negou seu pedido de TPA.");
                }

                player.sendMessage("§cVocê negou o pedido de TPA de " + args[0] + ".");
                meusPedidos.remove(nomeSolicitante);
            } else {
                player.sendMessage("§cVocê não tem nenhum pedido pendente de " + args[0] + ".");
            }
            return true;
        }

        return false;
    }
}