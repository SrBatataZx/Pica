package srbatata.pica.armor.armoconstructor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import srbatata.pica.armor.CustomArmor;
import srbatata.pica.armor.abilityconstructor.FireAura;
import srbatata.pica.core.Pica;
import srbatata.pica.util.ItemBuilder;

import java.util.List;

public class FireArmor extends CustomArmor {
    public static final String ARMOR_KEY = "is_fire_armor";
    public FireArmor(Pica plugin) {
        super("fire_armor",
                new ItemBuilder(Material.NETHERITE_CHESTPLATE)
                        .name(Component.text("Peitoral de Fogo").color(NamedTextColor.GOLD))
                        .lore(List.of(Component.text("Um item lendário...")))
                        .enchanted()
                        .build(),
                new FireAura());
    }
    // Método para registrar a receita de craft
    @Override
    public void registerRecipe(Pica plugin) {
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, this.getId());
        org.bukkit.inventory.ShapedRecipe recipe = new org.bukkit.inventory.ShapedRecipe(key, this.getItem());

        recipe.shape("L L", "LCL", "LLL");
        recipe.setIngredient('L', org.bukkit.Material.LAVA_BUCKET);
        recipe.setIngredient('C', org.bukkit.Material.NETHERITE_CHESTPLATE);

        org.bukkit.Bukkit.addRecipe(recipe);
    }

    @Override
    public boolean isWearingFullSet(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || !chest.hasItemMeta()) return false;

        // Verifica a tag persistente em vez do nome (Muito mais seguro!)
        return chest.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(Pica.getPlugin(), ARMOR_KEY), PersistentDataType.BYTE);
    }
}