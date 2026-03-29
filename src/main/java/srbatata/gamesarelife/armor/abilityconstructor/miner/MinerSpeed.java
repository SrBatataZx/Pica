package srbatata.gamesarelife.armor.abilityconstructor.miner;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import srbatata.gamesarelife.armor.Ability;
import srbatata.gamesarelife.core.Principal;

public class MinerSpeed implements Ability {
    private final NamespacedKey keyMinerador;

    public MinerSpeed(Principal plugin) {
        this.keyMinerador = new NamespacedKey(plugin, "is_miner_armor");
    }
    @Override
    public void execute(Player p) {
        PotionEffect atual = p.getPotionEffect(PotionEffectType.SPEED);
        // Renova de 5 em 5 segundos
        if (atual == null || atual.getDuration() <= 100) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0, false, false, false), true);
        }
    }
}