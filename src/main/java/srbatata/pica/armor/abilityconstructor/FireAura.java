package srbatata.pica.armor.abilityconstructor;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import srbatata.pica.armor.Ability;

public class FireAura implements Ability {
    @Override
    public void execute(Player player) {
        // Efeito visual de aura de fogo
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.05);
    }
}