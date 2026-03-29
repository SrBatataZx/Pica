package srbatata.gamesarelife.armor.abilityconstructor.miner;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import srbatata.gamesarelife.armor.Ability;
import srbatata.gamesarelife.core.Principal;

public class MinerNightVision implements Ability {
    public MinerNightVision(Principal plugin) {
        // Removi a key daqui pois a verificação de item deve ser feita na CustomArmor
    }

    @Override
    public void execute(Player p) {
        PotionEffect atual = p.getPotionEffect(PotionEffectType.NIGHT_VISION);

        // Se não tiver o efeito ou faltarem menos de 15 segundos (300 ticks)
        if (atual == null || atual.getDuration() <= 300) {
            // Aplicamos 20 segundos (400 ticks)
            // particles: false, icon: false -> Para parecer visão natural do capacete
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, false, false, false), true);
        }
    }
}