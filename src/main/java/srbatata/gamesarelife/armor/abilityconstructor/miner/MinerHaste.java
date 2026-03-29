package srbatata.gamesarelife.armor.abilityconstructor.miner;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import srbatata.gamesarelife.armor.Ability;
import srbatata.gamesarelife.core.Principal;

public class MinerHaste implements Ability {

    private final NamespacedKey keyMinerador;

    public MinerHaste(Principal plugin) {
        this.keyMinerador = new NamespacedKey(plugin, "is_miner_armor");
    }

    @Override
    public void execute(Player p) {
        int pecasHaste = 0;

        // Verifica o Peitoral e a Calça
        if (ehPecaMinerador(p.getInventory().getChestplate())) pecasHaste++;
        if (ehPecaMinerador(p.getInventory().getLeggings())) pecasHaste++;

        if (pecasHaste > 0) {
            int nivelHaste = pecasHaste - 1; // 1 peça = Haste I (0), 2 peças = Haste II (1)
            PotionEffect atual = p.getPotionEffect(PotionEffectType.HASTE);

            // Aumentamos a duração para 400 ticks (20s) e renovamos quando faltar 10s (200 ticks)
            // Isso evita completamente o reset na animação de quebrar o bloco
            if (atual == null || atual.getDuration() <= 100 || atual.getAmplifier() != nivelHaste) {
                // particles = false, icon = false
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, nivelHaste, false, false, false), true);
            }
        }
    }

    private boolean ehPecaMinerador(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyMinerador, PersistentDataType.BYTE);
    }
}