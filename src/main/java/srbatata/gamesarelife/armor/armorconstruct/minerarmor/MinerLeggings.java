package srbatata.gamesarelife.armor.armorconstruct.minerarmor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.armor.CustomArmor;
import srbatata.gamesarelife.core.Principal;
import srbatata.gamesarelife.util.ItemBuilder;

import java.util.List;

public class MinerLeggings extends CustomArmor {
    public static final String ARMOR_KEY = "is_miner_armor";
    private final Principal plugin;

    public MinerLeggings(Principal plugin) {
        super("miner_leggings",
                new ItemBuilder(Material.IRON_LEGGINGS)
                        .name(Component.text("Calça de Minerador").color(NamedTextColor.AQUA))
                        .lore(List.of(
                                Component.text("Concede"),
                                Component.text("+ poder de mineração!")
                        ))
                        .pdc(new NamespacedKey(plugin, ARMOR_KEY), PersistentDataType.BYTE, (byte) 1)
                        .enchanted()
                        .build(),
                null); // <-- NULL! Não precisamos dar a habilidade aqui pois o Chestplate já varre a calça
        this.plugin = plugin;
    }

    @Override
    public void registerRecipe(Principal plugin) {
        NamespacedKey key = new NamespacedKey(plugin, this.getId());
        org.bukkit.inventory.ShapedRecipe recipe = new org.bukkit.inventory.ShapedRecipe(key, this.getItem());

        recipe.shape("LLL", "LEL", "L L");
        recipe.setIngredient('L', Material.IRON_INGOT);
        recipe.setIngredient('E', Material.GOLDEN_CARROT);

        org.bukkit.Bukkit.addRecipe(recipe);
    }

    @Override
    public boolean isWearingFullSet(Player player) {
        // Como o Ability é null, o ArmorManager ignorará esta peça no loop de execução de habilidades.
        // Ela existe apenas para registro no sistema (para crafting).
        return false;
    }
}