package srbatata.gamesarelife.armor;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface Ability {
    void execute(Player player);
}