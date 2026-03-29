package srbatata.gamesarelife;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SistemaRTP implements CommandExecutor {

    // Guarda quando foi a última vez que o jogador usou o RTP (em milissegundos)
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();

    // 10 minutos em milissegundos (10 * 60 * 1000)
    private final long COOLDOWN_TEMPO = 600000;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // Verifica o Cooldown (Apenas ignora se for OP)
        if (cooldowns.containsKey(uuid) && !player.isOp()) {
            long tempoRestante = (cooldowns.get(uuid) + COOLDOWN_TEMPO) - System.currentTimeMillis();

            if (tempoRestante > 0) {
                long minutos = (tempoRestante / 1000) / 60;
                long segundos = (tempoRestante / 1000) % 60;
                player.sendMessage("§cVocê precisa esperar " + minutos + "m e " + segundos + "s para usar o RTP novamente!");
                return true;
            }
        }

        player.sendMessage("§eProcurando um local seguro... Aguarde!");

        // Inicia a busca (Em um servidor grande, o ideal é fazer isso de forma "assíncrona")
        Location localSeguro = encontrarLocalSeguro(player.getWorld());

        if (localSeguro != null) {
            player.teleport(localSeguro);
            player.sendMessage("§aWoosh! Você foi teleportado para um lugar aleatório!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);

            // Aplica o cooldown
            cooldowns.put(uuid, System.currentTimeMillis());
        } else {
            player.sendMessage("§cNão foi possível encontrar um local seguro. Tente novamente.");
        }

        return true;
    }

    private Location encontrarLocalSeguro(World mundo) {
        int maxTentativas = 20;

        for (int i = 0; i < maxTentativas; i++) {
            // Gera coordenadas aleatórias entre -5000 e +5000
            int x = random.nextInt(10000) - 5000;
            int z = random.nextInt(10000) - 5000;

            // Pega o bloco mais alto naquelas coordenadas
            Block blocoMaiorAlto = mundo.getHighestBlockAt(x, z);
            Material tipo = blocoMaiorAlto.getType();

            // Evita jogar o jogador na água, lava ou no teto do nether (se aplicável)
            if (tipo != Material.WATER && tipo != Material.LAVA && tipo != Material.MAGMA_BLOCK) {
                // Adiciona +1 no Y para ele não nascer com o pé preso no chão e centraliza o bloco (0.5)
                return new Location(mundo, x + 0.5, blocoMaiorAlto.getY() + 1, z + 0.5);
            }
        }
        return null; // Se não achar após 20 tentativas
    }
}