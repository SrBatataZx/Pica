package srbatata.gamesarelife.armor.abilityconstructor;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import srbatata.gamesarelife.SistemaTerrenos;
import srbatata.gamesarelife.armor.Ability; // Certifique-se de usar a interface Ability

public class ExplorerAbility implements Ability {

    private final SistemaTerrenos sistemaTerrenos;

    // Construtor pede o SistemaTerrenos para podermos usar a verificação
    public ExplorerAbility(SistemaTerrenos sistemaTerrenos) {
        this.sistemaTerrenos = sistemaTerrenos;
    }

    @Override
    public void execute(Player p) {
        // Se o código chegou aqui, o jogador JÁ ESTÁ usando a armadura.
        // Não precisamos verificar o nome do item de novo.

        // 1. Partículas de Velocidade se estiver correndo
        if (p.isSprinting()) {
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2, 0.05);
        }

        // 2. Lógica de Voo (Apenas se não estiver no criativo/espectador)
        if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {

            // Verifica se está no terreno dele
            boolean noTerreno = sistemaTerrenos.isDono(p, p.getLocation());

            if (noTerreno) {
                // Permite voar
                p.setAllowFlight(true);

                // Se estiver ativamente voando, solta partículas
                if (p.isFlying()) {
                    p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation(), 2, 0.3, 0.3, 0.3, 0.02);
                }
            } else {
                // Se saiu do terreno, desativa o voo
                desativarVoo(p);
            }
        }
    }

    private void desativarVoo(Player p) {
        if (p.getAllowFlight()) {
            p.setAllowFlight(false);
            p.setFlying(false);
        }
    }
}