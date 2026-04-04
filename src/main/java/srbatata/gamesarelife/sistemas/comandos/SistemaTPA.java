package srbatata.gamesarelife.sistemas.comandos;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SistemaTPA implements CommandExecutor {

    // Guarda quem pediu TPA para quem. [Alvo -> [Nome Solicitante -> UUID Solicitante]]
    private final Map<UUID, Map<String, UUID>> pedidosTpa = new HashMap<>();

    // NOVO: Guarda o cooldown de quem ENVIA o TPA [UUID Solicitante -> Tempo em MS]
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // Tempo de cooldown: 60 segundos (ajuste como preferir)
    private final long COOLDOWN_TIME = TimeUnit.SECONDS.toMillis(60);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "tpa" -> {
                if (args.length == 0) {
                    player.sendMessage("§cUso: /tpa <jogador>");
                    return true;
                }

                // --- VERIFICAÇÃO DE COOLDOWN ---
                if (cooldowns.containsKey(player.getUniqueId())) {
                    long tempoRestante = cooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
                    if (tempoRestante > 0) {
                        long segundos = TimeUnit.MILLISECONDS.toSeconds(tempoRestante);
                        player.sendMessage("§c" + "Aguarde " + segundos + "s para enviar outro pedido de teleporte.");
                        return true;
                    }
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
                pedidosTpa.computeIfAbsent(alvo.getUniqueId(), k -> new HashMap<>())
                        .put(player.getName().toLowerCase(), player.getUniqueId());

                // --- APLICA O COOLDOWN ---
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN_TIME);

                player.sendMessage("§aPedido de TPA enviado para §f" + alvo.getName() + "§a.");
                alvo.sendMessage("§eO jogador §f" + player.getName() + " §equer se teleportar até você.");
                alvo.sendMessage("§eDigite §a/tpaccept " + player.getName() + " §eou §c/tpadeny " + player.getName());
                return true;
            }

            case "tpaccept" -> {
                if (args.length == 0) {
                    player.sendMessage("§cUso: /tpaccept <jogador>");
                    return true;
                }

                String nomeSolicitante = args[0].toLowerCase();
                Map<String, UUID> meusPedidos = pedidosTpa.get(player.getUniqueId());

                if (meusPedidos != null && meusPedidos.containsKey(nomeSolicitante)) {
                    Player solicitante = Bukkit.getPlayer(meusPedidos.get(nomeSolicitante));

                    if (solicitante != null && solicitante.isOnline()) {
                        solicitante.teleportAsync(player.getLocation()).thenAccept(success -> {
                            if (success) {
                                solicitante.sendMessage("§a" + player.getName() + " aceitou seu pedido de TPA!");
                                player.sendMessage("§aVocê aceitou o pedido de TPA de " + solicitante.getName() + "!");
                                solicitante.playSound(solicitante.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                            }
                        });
                    } else {
                        player.sendMessage("§cEsse jogador não está mais online.");
                    }

                    meusPedidos.remove(nomeSolicitante);
                } else {
                    player.sendMessage("§cVocê não tem nenhum pedido pendente de " + args[0] + ".");
                }
                return true;
            }

            case "tpadeny" -> {
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
        }

        return false;
    }
    // No SistemaTPA.java, adicione o Listener e limpe os dados
    @EventHandler
    public void aoSair(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pedidosTpa.remove(uuid); // Remove pedidos pendentes para quem saiu
        cooldowns.remove(uuid);  // Opcional: limpa cooldown ao sair
    }
}