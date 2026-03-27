package srbatata.pica.armor;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import srbatata.pica.core.Pica; // Substitua pelo caminho correto da sua Main
import java.util.Optional;

public abstract class CustomArmor {
    private final String id;
    private final ItemStack item;
    private final Ability ability;

    public CustomArmor(String id, ItemStack item, Ability ability) {
        this.id = id;
        this.item = item;
        this.ability = ability;
    }

    /**
     * Define como esta armadura deve ser craftada.
     * @param plugin Instância da classe principal para registrar a NamespacedKey.
     */
    public abstract void registerRecipe(Pica plugin);

    public abstract boolean isWearingFullSet(Player player);

    // Getters
    public String getId() { return id; }
    public ItemStack getItem() { return item; }
    public Optional<Ability> getAbility() { return Optional.ofNullable(ability); }
}