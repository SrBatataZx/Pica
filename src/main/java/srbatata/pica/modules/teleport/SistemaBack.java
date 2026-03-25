package srbatata.pica.modules.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import srbatata.pica.core.Pica;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class SistemaBack implements CommandExecutor, Listener {

    private final Pica plugin;
    // Guarda um histórico de posições (LinkedList é perfeito para fila de 10 segundos)
    private final Map<UUID, LinkedList<Location>> historico = new HashMap<>();

    public SistemaBack(Pica plugin) {
        this.plugin = plugin;
        iniciarRastreadorNoTempo();
    }

    private void iniciarRastreadorNoTempo() {
        // Corre a cada 20 ticks (1 segundo)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID uuid = p.getUniqueId();
                historico.putIfAbsent(uuid, new LinkedList<>());

                LinkedList<Location> locs = historico.get(uuid);
                locs.addLast(p.getLocation()); // Guarda onde ele está AGORA

                // Se o histórico tiver mais de 10 segundos, removemos o mais antigo
                if (locs.size() > 10) {
                    locs.removeFirst();
                }
            }
        }, 0L, 20L);
    }

    // Limpa a memória quando o jogador sai do servidor
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        historico.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("back")) {
            UUID uuid = player.getUniqueId();
            LinkedList<Location> locs = historico.get(uuid);

            if (locs != null && !locs.isEmpty()) {
                // A primeira posição da lista é exatamente a de 10 segundos atrás
                Location locAntiga = locs.getFirst();

                player.teleport(locAntiga);
                player.sendMessage("§d⌛ Zzzzt! Viajaste 10 segundos de volta no tempo!");
                // Um som de magia/ilusão para dar o efeito de viagem no tempo
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);

                // Limpa o histórico após usar para não deixar o jogador "spammar" o comando
                locs.clear();
            } else {
                player.sendMessage("§cO tecido do tempo ainda está instável! Aguarda uns segundos.");
            }
            return true;
        }

        return false;
    }
}