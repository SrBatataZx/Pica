package srbatata.gamesarelife.armor.armorconstruct.minerarmor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.armor.CustomArmor;
import srbatata.gamesarelife.armor.abilityconstructor.miner.MinerSpeed;
import srbatata.gamesarelife.core.Principal;
import srbatata.gamesarelife.util.ItemBuilder;

import java.util.List;

public class MinerBoots extends CustomArmor {
    public static final String ARMOR_KEY = "is_miner_armor";
    private final Principal plugin;
    public MinerBoots(Principal plugin) {
        super("miner_boots", // ID único corrigido
                new ItemBuilder(Material.IRON_BOOTS)
                        .name(Component.text("Bota de Minerador").color(NamedTextColor.AQUA))
                        .lore(List.of(
                                Component.text("Concede"),
                                Component.text("+ velocidade para mineração!")
                        ))
                        // ESSENCIAL: Adicionando a tag escondida para identificação segura
                        .pdc(new NamespacedKey(plugin, ARMOR_KEY), PersistentDataType.BYTE, (byte) 1)
                        .enchanted()
                        .build(),
                new MinerSpeed(plugin)); // Instancia a habilidade correta
        this.plugin = plugin;
    }


    @Override
    public void registerRecipe(Principal plugin) {
        NamespacedKey key = new NamespacedKey(plugin, this.getId());
        org.bukkit.inventory.ShapedRecipe recipe = new org.bukkit.inventory.ShapedRecipe(key, this.getItem());

        recipe.shape("   ", "L L", "E E");
        recipe.setIngredient('L', Material.IRON_INGOT);
        recipe.setIngredient('E', Material.GOLDEN_CARROT);

        org.bukkit.Bukkit.addRecipe(recipe);
    }

    @Override
    public boolean isWearingFullSet(Player player) {
        ItemStack chest = player.getInventory().getBoots();
        if (chest == null || !chest.hasItemMeta()) return false;

        // Identifica pela Tag invisível gerada pelo ItemBuilder, 100% à prova de falhas
        return chest.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, ARMOR_KEY), PersistentDataType.BYTE);
    }
}
