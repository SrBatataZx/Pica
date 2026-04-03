package srbatata.gamesarelife.blocos;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import srbatata.gamesarelife.dados.GereWaystone;

import java.util.*;

public class PedraTeleporte implements Listener {

    private final JavaPlugin plugin;
    private final GereWaystone gereWaystone;
    public static NamespacedKey WAYSTONE_ITEM_KEY;
    public static NamespacedKey SAVED_WAYSTONES_KEY;

    // Mapa para controlar quem está digitando o nome
    private final Map<UUID, String> aguardandoNome = new HashMap<>();

    public PedraTeleporte(JavaPlugin plugin, GereWaystone gereWaystone) {
        this.plugin = plugin;
        this.gereWaystone = gereWaystone;
        WAYSTONE_ITEM_KEY = new NamespacedKey(plugin, "item_pedra_teleporte");
        SAVED_WAYSTONES_KEY = new NamespacedKey(plugin, "player_waystones");
        registrarReceita();
    }

    public ItemStack getPedraTeleporteItem() {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Pedra do Teleporte").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text("Coloque no chão para criar um ponto de teleporte.").color(NamedTextColor.GRAY),
                Component.text("Clique direito para salvar este local!").color(NamedTextColor.YELLOW)
        ));
        meta.getPersistentDataContainer().set(WAYSTONE_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void registrarReceita() {
        ItemStack item = getPedraTeleporteItem();
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "craft_pedra_teleporte"), item);
        recipe.shape("CCC", "CEC", "CCC");
        recipe.setIngredient('C', Material.COBBLESTONE);
        recipe.setIngredient('E', Material.ENDER_EYE);
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(WAYSTONE_ITEM_KEY, PersistentDataType.BYTE)) {
            Location loc = event.getBlockPlaced().getLocation();

            // Agora salvamos a localização + um separador (;) + o UUID do jogador
            String locString = serializarLocation(loc);
            String dadosParaSalvar = locString + ";" + event.getPlayer().getUniqueId().toString();

            gereWaystone.adicionarWaystone(dadosParaSalvar);
            event.getPlayer().sendMessage(Component.text("Waystone criada e protegida com sucesso!").color(NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        String locString = serializarLocation(loc);

        // Busca se existe uma waystone nesta coordenada
        String waystoneData = getWaystoneData(locString);

        if (waystoneData != null) {
            Player player = event.getPlayer();

            // Extrai o UUID do dono (fica depois do ';')
            String[] partes = waystoneData.split(";");
            String donoUUID = partes.length > 1 ? partes[1] : "";

            // 1. Verificação de permissão
            if (!donoUUID.equals(player.getUniqueId().toString()) && !player.hasPermission("waystone.admin")) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Você não é o dono dessa Pedra de Teleporte!").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            // --- CORREÇÃO AQUI ---

            // 2. Remove a Waystone da memória/arquivo
            gereWaystone.removerWaystone(waystoneData);

            // 3. Impede QUALQUER drop natural do bloco (evita dropar magnetita comum)
            event.setDropItems(false);
            event.getBlock().setType(Material.AIR); // Força o bloco a virar ar imediatamente

            // 4. Dropa o item correto com as chaves PDC (PersistentDataContainer)
            loc.getWorld().dropItemNaturally(loc, getPedraTeleporteItem());

            player.sendMessage(Component.text("Waystone destruída e recuperada!").color(NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f);
        }
    }

    @EventHandler
    public void onExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        // Remove as Waystones da lista de blocos a serem destruídos por explosões
        event.blockList().removeIf(block -> {
            String locString = serializarLocation(block.getLocation());
            return getWaystoneData(locString) != null;
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // 1. Validações básicas (Mão principal e se clicou em um bloco)
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Location loc = event.getClickedBlock().getLocation();
        String locString = serializarLocation(loc);

        // 2. Verifica se o bloco clicado é de fato uma Waystone registrada no servidor
        if (getWaystoneData(locString) != null) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            // Pegamos a lista de waystones que o jogador já salvou
            String salvas = player.getPersistentDataContainer().getOrDefault(SAVED_WAYSTONES_KEY, PersistentDataType.STRING, "");

            // --- LÓGICA DE REMOÇÃO (SHIFT + CLIQUE) ---
            if (player.isSneaking()) {
                if (removerWaystoneDoJogador(player, locString)) {
                    player.sendMessage(Component.text("Você apagou a localização dessa Pedra de Teleporte.").color(NamedTextColor.YELLOW));
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.5f, 1.5f);
                } else {
                    player.sendMessage(Component.text("Você ainda não memorizou essa Pedra.").color(NamedTextColor.RED));
                }
                return; // Encerra aqui para não disparar o salvamento abaixo
            }

            // --- LÓGICA DE SALVAMENTO (CLIQUE NORMAL) ---
            if (salvas.contains(locString)) {
                player.sendMessage(Component.text("Você já salvou essa Pedra de Teleporte!").color(NamedTextColor.RED));
                return;
            }

            // Adiciona o jogador no mapa de espera para o nome
            aguardandoNome.put(uuid, locString);
            player.sendMessage(Component.text("Digite no chat o nome para esta Pedra de Teleporte. (30s)").color(NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);

            // Timeout (permanece igual)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (aguardandoNome.containsKey(uuid)) {
                    String locPendente = aguardandoNome.remove(uuid);
                    if (player.isOnline()) {
                        player.sendMessage(Component.text("Tempo esgotado! Nome padrão aplicado.").color(NamedTextColor.GRAY));
                        efetuarSalvamentoWaystone(player, locPendente, "Local Desconhecido");
                    }
                }
            }, 600L);
        }
    }

    // Intercepta o chat nativo do Paper de forma assíncrona
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (aguardandoNome.containsKey(uuid)) {
            event.setCancelled(true);

            String nomeEscolhido = PlainTextComponentSerializer.plainText().serialize(event.message());
            String locString = aguardandoNome.remove(uuid);

            nomeEscolhido = nomeEscolhido.replace(";", "").replace("@", "").trim();
            if (nomeEscolhido.length() > 20) nomeEscolhido = nomeEscolhido.substring(0, 20);
            if (nomeEscolhido.isEmpty()) nomeEscolhido = "Local Desconhecido";

            // Congela as variáveis para usar dentro da Lambda
            final String nomeFinal = nomeEscolhido;
            final String locFinal = locString;

            // Volta para a thread principal (síncrona) para alterar o PDC e enviar mensagens
            Bukkit.getScheduler().runTask(plugin, () -> efetuarSalvamentoWaystone(player, locFinal, nomeFinal));
        }
    }

    // Evita vazamento de memória se o jogador deslogar enquanto o plugin espera o chat
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        aguardandoNome.remove(event.getPlayer().getUniqueId());
    }

    public static String serializarLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public static Location desserializarLocation(String str) {
        String[] partes = str.split(",");
        if (partes.length < 4) return null;
        return new Location(Bukkit.getWorld(partes[0]), Integer.parseInt(partes[1]), Integer.parseInt(partes[2]), Integer.parseInt(partes[3]));
    }
    // Método para buscar os dados completos da waystone no arquivo
    private String getWaystoneData(String locString) {
        for (String w : gereWaystone.getWaystonesAtivas()) {
            if (w.startsWith(locString)) {
                return w;
            }
        }
        return null;
    }
    // Método auxiliar para salvar o dado diretamente no jogador
    private void efetuarSalvamentoWaystone(Player player, String locString, String nomeEscolhido) {
        String salvas = player.getPersistentDataContainer().getOrDefault(SAVED_WAYSTONES_KEY, PersistentDataType.STRING, "");

        String novaEntrada = locString + "@" + nomeEscolhido;
        if (salvas.isEmpty()) salvas = novaEntrada;
        else salvas += ";" + novaEntrada;

        player.getPersistentDataContainer().set(SAVED_WAYSTONES_KEY, PersistentDataType.STRING, salvas);

        player.sendMessage(Component.text("Local salvo como: " + nomeEscolhido + "!").color(NamedTextColor.AQUA));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
    private boolean removerWaystoneDoJogador(Player player, String locString) {
        String salvas = player.getPersistentDataContainer().getOrDefault(SAVED_WAYSTONES_KEY, PersistentDataType.STRING, "");

        if (salvas.isEmpty() || !salvas.contains(locString)) return false;

        // Transformamos em uma lista para facilitar a remoção exata
        // Formato salvo: mundo,x,y,z@Nome;mundo2,x2,y2,z2@Nome2
        List<String> listaArray = new ArrayList<>(Arrays.asList(salvas.split(";")));
        boolean removido = listaArray.removeIf(entrada -> entrada.startsWith(locString));

        if (removido) {
            // Unimos novamente a String usando o delimitador ';'
            String novaString = String.join(";", listaArray);

            if (novaString.isEmpty()) {
                player.getPersistentDataContainer().remove(SAVED_WAYSTONES_KEY);
            } else {
                player.getPersistentDataContainer().set(SAVED_WAYSTONES_KEY, PersistentDataType.STRING, novaString);
            }
            return true;
        }

        return false;
    }
}