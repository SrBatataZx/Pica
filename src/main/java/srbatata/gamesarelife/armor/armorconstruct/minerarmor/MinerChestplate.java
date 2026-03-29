package srbatata.gamesarelife.armor.armorconstruct.minerarmor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.armor.CustomArmor;
import srbatata.gamesarelife.armor.abilityconstructor.miner.MinerHaste;
import srbatata.gamesarelife.core.Principal;
import srbatata.gamesarelife.util.ItemBuilder;

import java.util.List;

public class MinerChestplate extends CustomArmor {
    public static final String ARMOR_KEY = "is_miner_armor";
    private final Principal plugin;

    public MinerChestplate(Principal plugin) {
        super("miner_chestplate",
                new ItemBuilder(Material.IRON_CHESTPLATE)
                        .name(Component.text("Roupa de Minerador").color(NamedTextColor.AQUA))
                        .lore(List.of(
                                Component.text("Concede"),
                                Component.text("+ poder de mineração!")
                        ))
                        .pdc(new NamespacedKey(plugin, ARMOR_KEY), PersistentDataType.BYTE, (byte) 1)
                        .enchanted()
                        .build(),
                new MinerHaste(plugin)); // Este carrega a habilidade que lê ambas as peças
        this.plugin = plugin;
    }

    @Override
    public void registerRecipe(Principal plugin) {
        NamespacedKey key = new NamespacedKey(plugin, this.getId());
        org.bukkit.inventory.ShapedRecipe recipe = new org.bukkit.inventory.ShapedRecipe(key, this.getItem());

        recipe.shape("L L", "LEL", "LLL");
        recipe.setIngredient('L', Material.IRON_INGOT);
        recipe.setIngredient('E', Material.GOLDEN_CARROT);

        org.bukkit.Bukkit.addRecipe(recipe);
    }

    @Override
    public boolean isWearingFullSet(Player player) {
        // Retorna true se estiver usando o peito OU a calça
        // Isso ativa a execução do MinerHaste, que lá dentro fará o cálculo de qual nível aplicar.
        ItemStack chest = player.getInventory().getChestplate();
        ItemStack legs = player.getInventory().getLeggings();

        NamespacedKey key = new NamespacedKey(plugin, ARMOR_KEY);

        boolean hasChest = chest != null && chest.hasItemMeta() && chest.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
        boolean hasLegs = legs != null && legs.hasItemMeta() && legs.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);

        return hasChest || hasLegs;
    }
}