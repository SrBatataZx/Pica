package srbatata.gamesarelife.armor.armorconstruct.minerarmor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType; // Import atualizado
import srbatata.gamesarelife.armor.CustomArmor;
import srbatata.gamesarelife.armor.abilityconstructor.miner.MinerNightVision;
import srbatata.gamesarelife.core.Principal;
import srbatata.gamesarelife.util.ItemBuilder;

import java.util.List;

public class MinerHelmet extends CustomArmor {
    public static final String ARMOR_KEY = "is_miner_armor";
    private final Principal plugin;

    public MinerHelmet(Principal plugin) {
        super("miner_helmet",
                new ItemBuilder(Material.IRON_HELMET)
                        .name(Component.text("Capacete de Minerador").color(NamedTextColor.AQUA))
                        .lore(List.of(
                                Component.text("Concede"),
                                Component.text("Visão noturna")
                        ))
                        .pdc(new NamespacedKey(plugin, ARMOR_KEY), PersistentDataType.BYTE, (byte) 1)
                        .enchanted()
                        .build(),
                new MinerNightVision(plugin));
        this.plugin = plugin;
    }

    @Override
    public void registerRecipe(Principal plugin) {
        ItemStack nightVision = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) nightVision.getItemMeta();

        // CORREÇÃO PARA 1.21: Uso do setBasePotionType
        meta.setBasePotionType(PotionType.NIGHT_VISION);
        nightVision.setItemMeta(meta);

        NamespacedKey key = new NamespacedKey(plugin, this.getId());
        org.bukkit.inventory.ShapedRecipe recipe = new org.bukkit.inventory.ShapedRecipe(key, this.getItem());

        recipe.shape("LLL", "LEL", "   ");
        recipe.setIngredient('L', Material.IRON_INGOT);
        // Exige exatamente a poção de visão noturna
        recipe.setIngredient('E', new RecipeChoice.ExactChoice(nightVision));

        org.bukkit.Bukkit.addRecipe(recipe);
    }

    @Override
    public boolean isWearingFullSet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || !helmet.hasItemMeta()) return false;

        // Use a chave diretamente do objeto para evitar erros de criação de NamespacedKey
        NamespacedKey key = new NamespacedKey(plugin, ARMOR_KEY);

        return helmet.getItemMeta().getPersistentDataContainer()
                .has(key, PersistentDataType.BYTE);
    }
}