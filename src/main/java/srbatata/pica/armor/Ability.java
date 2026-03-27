package srbatata.pica.armor;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface Ability {
    void execute(Player player);
}