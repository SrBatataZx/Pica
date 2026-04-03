package srbatata.gamesarelife.armor.armorconstruct;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.sistemas.SistemaTerrenos;
import srbatata.gamesarelife.armor.CustomArmor;
import srbatata.gamesarelife.armor.abilityconstructor.ExplorerAbility;
import srbatata.gamesarelife.core.Principal;
import srbatata.gamesarelife.util.ItemBuilder;

import java.util.List;

public class ExplorerArmor extends CustomArmor {

    public static final String ARMOR_KEY = "is_explorer_armor";
    private final Principal plugin;

    // Passamos o plugin e o sistema de terrenos no construtor
    public ExplorerArmor(Principal plugin, SistemaTerrenos terrenos) {
        super("explorer_armor", // ID único corrigido
                new ItemBuilder(Material.LEATHER_CHESTPLATE) // Mudei para ELYTRA para combinar com voo
                        .name(Component.text("Traje do Explorador").color(NamedTextColor.AQUA))
                        .lore(List.of(
                                Component.text("Voe livremente dentro"),
                                Component.text("dos seus próprios terrenos!")
                        ))
                        // ESSENCIAL: Adicionando a tag escondida para identificação segura
                        .pdc(new NamespacedKey(plugin, ARMOR_KEY), PersistentDataType.BYTE, (byte) 1)
                        .enchanted()
                        .build(),
                new ExplorerAbility(terrenos)); // Instancia a habilidade correta
        this.plugin = plugin;
    }

    @Override
    public void registerRecipe(Principal plugin) {
        NamespacedKey key = new NamespacedKey(plugin, this.getId());
        org.bukkit.inventory.ShapedRecipe recipe = new org.bukkit.inventory.ShapedRecipe(key, this.getItem());

        recipe.shape("L L", "LEL", "LLL");
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('E', Material.ELYTRA);

        org.bukkit.Bukkit.addRecipe(recipe);
    }

    @Override
    public boolean isWearingFullSet(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || !chest.hasItemMeta()) return false;

        // Identifica pela Tag invisível gerada pelo ItemBuilder, 100% à prova de falhas
        return chest.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, ARMOR_KEY), PersistentDataType.BYTE);
    }
}