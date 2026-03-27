package srbatata.pica.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(Component name) {
        // Itens customizados geralmente não devem ter o itálico padrão do Minecraft
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        return this;
    }
    public ItemBuilder enchanted() {
        // Método moderno da 1.21 para forçar o brilho
        meta.setEnchantmentGlintOverride(true);
        return this;
    }

    public ItemBuilder lore(List<Component> loreLines) {
        // Remove o itálico de todas as linhas da lore para um visual mais limpo
        List<Component> cleanedLore = loreLines.stream()
                .map(line -> line.decoration(TextDecoration.ITALIC, false))
                .toList();
        meta.lore(cleanedLore);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}