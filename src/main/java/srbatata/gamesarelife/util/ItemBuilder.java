package srbatata.gamesarelife.util; // Ajuste para o seu pacote atual

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    // Método para definir o nome do item
    public ItemBuilder name(Component name) {
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        return this;
    }

    // Método para definir a lore (descrição)
    public ItemBuilder lore(List<Component> loreLines) {
        List<Component> cleanedLore = loreLines.stream()
                .map(line -> line.decoration(TextDecoration.ITALIC, false))
                .toList();
        meta.lore(cleanedLore);
        return this;
    }

    // MÉTODOS NOVOS ABAIXO:

    /**
     * Adiciona dados persistentes invisíveis ao item (Muito seguro para verificar custom items).
     */
    public <T, Z> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        meta.getPersistentDataContainer().set(key, type, value);
        return this;
    }

    /**
     * Adiciona o brilho de encantamento ao item sem adicionar encantamentos reais (Específico da 1.21).
     */
    public ItemBuilder enchanted() {
        meta.setEnchantmentGlintOverride(true);
        return this;
    }

    // Finaliza e constrói o ItemStack
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}