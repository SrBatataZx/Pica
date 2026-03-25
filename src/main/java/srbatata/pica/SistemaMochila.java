package srbatata.pica;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SistemaMochila implements Listener {

    private final Pica plugin;
    private final NamespacedKey keyMochilaMarker;
    private final NamespacedKey keyMochilaID;
    private final NamespacedKey keyMochilaDono;

    // Guarda qual mochila o jogador está acessando no momento para salvar com segurança
    private final Map<UUID, String> mochilasAbertas = new HashMap<>();

    public SistemaMochila(Pica plugin) {
        this.plugin = plugin;
        this.keyMochilaMarker = new NamespacedKey(plugin, "is_mochila");
        this.keyMochilaID = new NamespacedKey(plugin, "mochila_id");
        this.keyMochilaDono = new NamespacedKey(plugin, "mochila_dono");
        registrarReceita();
    }

    // ==========================================
    // 1. RECEITA BASE (Sem ID ainda)
    // ==========================================
    private void registrarReceita() {
        ItemStack mochila = new ItemStack(Material.OXIDIZED_CHISELED_COPPER);
        ItemMeta meta = mochila.getItemMeta();
        meta.setDisplayName("§6§lMochila do Aventureiro");
        meta.setLore(Arrays.asList("§7Dono: §fNinguém", "§7Uma mochila resistente para", "§7guardar seus tesouros.", "", "§eBotão Direito para abrir!"));

        // Coloca apenas uma marcação dizendo "Eu sou uma mochila"
        meta.getPersistentDataContainer().set(keyMochilaMarker, PersistentDataType.BYTE, (byte) 1);
        mochila.setItemMeta(meta);

        ShapedRecipe receita = new ShapedRecipe(new NamespacedKey(plugin, "mochila_aventureiro"), mochila);
        receita.shape("LLL", "LSL", "LLL");
        receita.setIngredient('L', Material.LEATHER);
        receita.setIngredient('S', Material.SHULKER_SHELL);

        Bukkit.addRecipe(receita);
    }

    // ==========================================
    // 2. MOMENTO DO CRAFT (Gera ID e Vincula)
    // ==========================================
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack recipeResult = event.getRecipe() != null ? event.getRecipe().getResult() : null;

        if (recipeResult != null && recipeResult.hasItemMeta() && recipeResult.getItemMeta().getPersistentDataContainer().has(keyMochilaMarker, PersistentDataType.BYTE)) {

            // Proíbe craftar com Shift para evitar que o Minecraft clone o ID
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ((Player) event.getWhoClicked()).sendMessage("§cPor favor, crafte a mochila clicando normalmente (sem usar o Shift)!");
                return;
            }

            // Pega o item que ele está tirando da bancada e transforma em ÚNICO
            ItemStack result = event.getCurrentItem();
            if (result != null && result.hasItemMeta()) {
                ItemMeta meta = result.getItemMeta();
                Player player = (Player) event.getWhoClicked();

                String novoID = UUID.randomUUID().toString();
                String donoID = player.getUniqueId().toString();

                meta.getPersistentDataContainer().set(keyMochilaID, PersistentDataType.STRING, novoID);
                meta.getPersistentDataContainer().set(keyMochilaDono, PersistentDataType.STRING, donoID);

                // Atualiza o nome do dono na descrição
                List<String> lore = meta.getLore();
                if (lore != null && !lore.isEmpty()) {
                    lore.set(0, "§7Dono: §a" + player.getName());
                }
                meta.setLore(lore);
                result.setItemMeta(meta);
            }
        }
    }

    // ==========================================
    // 3. IMPEDIR DE COLOCAR NO CHÃO
    // ==========================================
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(keyMochilaMarker, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode colocar a mochila no chão! Use o botão direito para abrir.");
        }
    }

    // ==========================================
    // 4. ABRIR O INVENTÁRIO
    // ==========================================
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(keyMochilaMarker, PersistentDataType.BYTE)) {
            event.setCancelled(true);

            Player player = event.getPlayer();

            if (!meta.getPersistentDataContainer().has(keyMochilaID, PersistentDataType.STRING)) {
                player.sendMessage("§cEsta mochila é inválida. Crafte uma nova!");
                return;
            }

            // Verifica se quem está abrindo é o Dono Original
            String donoID = meta.getPersistentDataContainer().get(keyMochilaDono, PersistentDataType.STRING);
            if (donoID != null && !donoID.equals(player.getUniqueId().toString())) {
                player.sendMessage("§cEsta mochila pertence a outro jogador! Somente o dono pode abrir.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            String idMochila = meta.getPersistentDataContainer().get(keyMochilaID, PersistentDataType.STRING);
            abrirMochila(player, idMochila);
        }
    }

    private void abrirMochila(Player player, String id) {
        Inventory invMochila = Bukkit.createInventory(null, 36, "§8Sua Mochila");

        // Carrega os itens salvos
        if (plugin.getSalvos().contains("mochilas." + id)) {
            ItemStack[] conteudos = plugin.getSalvos().getList("mochilas." + id).toArray(new ItemStack[0]);
            invMochila.setContents(conteudos);
        }

        mochilasAbertas.put(player.getUniqueId(), id); // Guarda na memória qual mochila ele abriu
        player.openInventory(invMochila);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);
    }

    // ==========================================
    // 5. PROTEÇÃO ANTI-INCEPTION E ANTI-DUPE
    // ==========================================
    @EventHandler
    public void onClickMochila(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§8Sua Mochila")) {

            // Impede o clique normal numa mochila dentro do menu
            ItemStack clicado = event.getCurrentItem();
            if (clicado != null && clicado.hasItemMeta() && clicado.getItemMeta().getPersistentDataContainer().has(keyMochilaMarker, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                ((Player) event.getWhoClicked()).sendMessage("§cVocê não pode guardar uma mochila dentro de outra!");
            }

            // Impede de usar os números (1 a 9) do teclado para jogar a mochila lá dentro
            if (event.getClick().name().contains("NUMBER_KEY")) {
                ItemStack atalho = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                if (atalho != null && atalho.hasItemMeta() && atalho.getItemMeta().getPersistentDataContainer().has(keyMochilaMarker, PersistentDataType.BYTE)) {
                    event.setCancelled(true);
                    ((Player) event.getWhoClicked()).sendMessage("§cVocê não pode guardar uma mochila dentro de outra!");
                }
            }
        }
    }

    // ==========================================
    // 6. SALVAR COM SEGURANÇA AO FECHAR
    // ==========================================
    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        if (mochilasAbertas.containsKey(player.getUniqueId())) {
            String id = mochilasAbertas.remove(player.getUniqueId());

            // Salva os itens no arquivo salvos.yml
            plugin.getSalvos().set("mochilas." + id, Arrays.asList(event.getInventory().getContents()));
            plugin.saveSalvos();

            player.sendMessage("§aMochila salva com sucesso!");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
        }
    }
}