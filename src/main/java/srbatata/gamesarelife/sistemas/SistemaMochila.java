package srbatata.gamesarelife.sistemas;

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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.core.Principal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SistemaMochila implements Listener {

    private final Principal plugin;

    // CHAVES NOVAS
    private final NamespacedKey keyMochilaMarker;
    private final NamespacedKey keyMochilaID;
    private final NamespacedKey keyMochilaDono;

    // CHAVES ANTIGAS (Para conversão)
    private final NamespacedKey keyMochilaMarkerAntiga;
    private final NamespacedKey keyMochilaIDAntiga;
    private final NamespacedKey keyMochilaDonoAntiga;

    private final Map<UUID, String> mochilasAbertas = new HashMap<>();

    public SistemaMochila(Principal plugin) {
        this.plugin = plugin;

        // Chaves atuais do plugin
        this.keyMochilaMarker = new NamespacedKey(plugin, "is_mochila");
        this.keyMochilaID = new NamespacedKey(plugin, "mochila_id");
        this.keyMochilaDono = new NamespacedKey(plugin, "mochila_dono");

        // Chaves legadas
        this.keyMochilaMarkerAntiga = NamespacedKey.fromString("pica:is_mochila");
        this.keyMochilaIDAntiga = NamespacedKey.fromString("pica:mochila_id");
        this.keyMochilaDonoAntiga = NamespacedKey.fromString("pica:mochila_dono");

        registrarReceita();
    }

    // ==========================================
    // SISTEMA DE MIGRAÇÃO (NOVIDADE)
    // ==========================================
    private void converterMochilaAntiga(ItemStack item, ItemMeta meta) {
        boolean modificou = false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Se encontrar o marcador antigo, transfere todos os dados para as chaves novas
        if (pdc.has(keyMochilaMarkerAntiga, PersistentDataType.BYTE)) {
            pdc.set(keyMochilaMarker, PersistentDataType.BYTE, pdc.getOrDefault(keyMochilaMarkerAntiga, PersistentDataType.BYTE, (byte) 1));
            pdc.remove(keyMochilaMarkerAntiga);
            modificou = true;
        }
        if (pdc.has(keyMochilaIDAntiga, PersistentDataType.STRING)) {
            pdc.set(keyMochilaID, PersistentDataType.STRING, pdc.get(keyMochilaIDAntiga, PersistentDataType.STRING));
            pdc.remove(keyMochilaIDAntiga);
            modificou = true;
        }
        if (pdc.has(keyMochilaDonoAntiga, PersistentDataType.STRING)) {
            pdc.set(keyMochilaDono, PersistentDataType.STRING, pdc.get(keyMochilaDonoAntiga, PersistentDataType.STRING));
            pdc.remove(keyMochilaDonoAntiga);
            modificou = true;
        }

        if (modificou) {
            item.setItemMeta(meta);
        }
    }

    // ==========================================
    // 1. RECEITA BASE
    // ==========================================
    private void registrarReceita() {
        ItemStack mochila = new ItemStack(Material.OXIDIZED_CHISELED_COPPER);
        ItemMeta meta = mochila.getItemMeta();
        meta.setDisplayName("§6§lMochila do Aventureiro");
        meta.setLore(Arrays.asList("§7Dono: §fNinguém", "§7Uma mochila resistente para", "§7guardar seus tesouros.", "", "§eBotão Direito para abrir!"));

        // Já utiliza a nova chave
        meta.getPersistentDataContainer().set(keyMochilaMarker, PersistentDataType.BYTE, (byte) 1);
        mochila.setItemMeta(meta);

        // Aconselho atualizar a chave da receita também
        ShapedRecipe receita = new ShapedRecipe(new NamespacedKey(plugin, "mochila_aventureiro"), mochila);
        receita.shape("LLL", "LSL", "LLL");
        receita.setIngredient('L', Material.LEATHER);
        receita.setIngredient('S', Material.SHULKER_SHELL);

        Bukkit.addRecipe(receita);
    }

    // ==========================================
    // 2. MOMENTO DO CRAFT (Apenas gera o ID)
    // ==========================================
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack recipeResult = event.getRecipe() != null ? event.getRecipe().getResult() : null;

        if (recipeResult != null && recipeResult.hasItemMeta() && recipeResult.getItemMeta().getPersistentDataContainer().has(keyMochilaMarker, PersistentDataType.BYTE)) {

            if (event.isShiftClick()) {
                event.setCancelled(true);
                ((Player) event.getWhoClicked()).sendMessage("§cPor favor, crafte a mochila clicando normalmente (sem usar o Shift)!");
                return;
            }

            ItemStack result = event.getCurrentItem();
            if (result != null && result.hasItemMeta()) {
                ItemMeta meta = result.getItemMeta();

                // Gera apenas o ID único da mochila, mas NÃO vincula o dono ainda
                String novoID = UUID.randomUUID().toString();
                meta.getPersistentDataContainer().set(keyMochilaID, PersistentDataType.STRING, novoID);

                // Mantém a lore original de "Dono: Ninguém"
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
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            // Verificamos e convertemos caso alguém tente colocar uma mochila velha no chão
            converterMochilaAntiga(item, meta);

            if (meta.getPersistentDataContainer().has(keyMochilaMarker, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cVocê não pode colocar a mochila no chão! Use o botão direito para abrir.");
            }
        }
    }

    // ==========================================
    // 4. ABRIR O INVENTÁRIO (E VINCULAR O DONO)
    // ==========================================
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        // Conversão Silenciosa ocorre aqui antes de qualquer validação
        converterMochilaAntiga(item, meta);

        if (meta.getPersistentDataContainer().has(keyMochilaMarker, PersistentDataType.BYTE)) {
            event.setCancelled(true);

            Player player = event.getPlayer();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();

            if (!pdc.has(keyMochilaID, PersistentDataType.STRING)) {
                player.sendMessage("§cEsta mochila é inválida. Crafte uma nova!");
                return;
            }

            // [MODIFICAÇÃO] Verifica se a mochila JÁ TEM um dono
            if (!pdc.has(keyMochilaDono, PersistentDataType.STRING)) {
                // Se não tiver dono, vincula a mochila ao jogador que acabou de abrir
                pdc.set(keyMochilaDono, PersistentDataType.STRING, player.getUniqueId().toString());

                // Atualiza a linha do dono na Lore
                List<String> lore = meta.getLore();
                if (lore != null && !lore.isEmpty()) {
                    lore.set(0, "§7Dono: §a" + player.getName());
                }
                meta.setLore(lore);
                item.setItemMeta(meta); // Aplica a modificação no item na mão do jogador

                player.sendMessage("§a§l[!] §aEsta mochila foi vinculada a você!");
            } else {
                // Se já tiver dono, verifica se o dono atual é quem está tentando abrir
                String donoID = pdc.get(keyMochilaDono, PersistentDataType.STRING);
                if (!donoID.equals(player.getUniqueId().toString())) {
                    player.sendMessage("§cEsta mochila pertence a outro jogador! Somente o dono pode abrir.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
            }

            String idMochila = pdc.get(keyMochilaID, PersistentDataType.STRING);
            abrirMochila(player, idMochila);
        }
    }

    private void abrirMochila(Player player, String id) {
        Inventory invMochila = Bukkit.createInventory(null, 36, "§8Sua Mochila");

        if (plugin.getSalvos().contains("mochilas." + id)) {
            ItemStack[] conteudos = plugin.getSalvos().getList("mochilas." + id).toArray(new ItemStack[0]);
            invMochila.setContents(conteudos);
        }

        mochilasAbertas.put(player.getUniqueId(), id);
        player.openInventory(invMochila);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);
    }

    // ==========================================
    // 5. PROTEÇÃO ANTI-INCEPTION
    // ==========================================
    @EventHandler
    public void onClickMochila(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§8Sua Mochila")) {

            ItemStack clicado = event.getCurrentItem();
            if (clicado != null && clicado.hasItemMeta()) {
                ItemMeta meta = clicado.getItemMeta();
                converterMochilaAntiga(clicado, meta); // Evita colocar mochila velha dentro de nova

                if (meta.getPersistentDataContainer().has(keyMochilaMarker, PersistentDataType.BYTE)) {
                    event.setCancelled(true);
                    ((Player) event.getWhoClicked()).sendMessage("§cVocê não pode guardar uma mochila dentro de outra!");
                }
            }

            if (event.getClick().name().contains("NUMBER_KEY")) {
                ItemStack atalho = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                if (atalho != null && atalho.hasItemMeta()) {
                    ItemMeta meta = atalho.getItemMeta();
                    converterMochilaAntiga(atalho, meta);

                    if (meta.getPersistentDataContainer().has(keyMochilaMarker, PersistentDataType.BYTE)) {
                        event.setCancelled(true);
                        ((Player) event.getWhoClicked()).sendMessage("§cVocê não pode guardar uma mochila dentro de outra!");
                    }
                }
            }
        }
    }

    // ==========================================
    // 6. SALVAR COM SEGURANÇA AO FECHAR / SAIR
    // ==========================================
    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        if (mochilasAbertas.containsKey(player.getUniqueId())) {
            String id = mochilasAbertas.remove(player.getUniqueId());

            plugin.getSalvos().set("mochilas." + id, Arrays.asList(event.getInventory().getContents()));
            plugin.saveSalvos();

            player.sendMessage("§aMochila salva com sucesso!");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
        }
    }

    @EventHandler
    public void aoSairMochila(org.bukkit.event.player.PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (mochilasAbertas.containsKey(uuid)) {
            String id = mochilasAbertas.remove(uuid);
            // Salva a mochila caso ele desconecte abruptamente com ela aberta
            plugin.getSalvos().set("mochilas." + id, Arrays.asList(e.getPlayer().getOpenInventory().getTopInventory().getContents()));
            plugin.saveSalvos();
        }
    }
}