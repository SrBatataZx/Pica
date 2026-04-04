package srbatata.gamesarelife.sistemas;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import srbatata.gamesarelife.core.Principal;

import java.util.UUID;

public class SistemaLojaPlacas implements Listener {

    private final Principal plugin;
    private final Economy econ;
    private final SistemaTerrenos sistemaTerrenos; // Injeção necessária

    private final NamespacedKey keyDono;
    private final NamespacedKey keyTipo;
    private final NamespacedKey keyQtd;
    private final NamespacedKey keyItem;
    private final NamespacedKey keyValor;
    private final NamespacedKey licencaKey;

    public SistemaLojaPlacas(Principal plugin, Economy econ, SistemaTerrenos sistemaTerrenos) {
        this.plugin = plugin;
        this.econ = econ;
        this.sistemaTerrenos = sistemaTerrenos; // Inicialização
        this.keyDono = new NamespacedKey(plugin, "loja_dono");
        this.keyTipo = new NamespacedKey(plugin, "loja_tipo");
        this.keyQtd = new NamespacedKey(plugin, "loja_qtd");
        this.keyItem = new NamespacedKey(plugin, "loja_item");
        this.keyValor = new NamespacedKey(plugin, "loja_valor");
        this.licencaKey = new NamespacedKey(plugin, "licenca_terreno_loja");
    }

    private int getLojasCriadas(UUID uuid) {
        return plugin.getSalvos().getInt("limites_loja." + uuid.toString(), 0);
    }

    private void alterarLojasCriadas(UUID uuid, int quantidade) {
        int atual = getLojasCriadas(uuid);
        plugin.getSalvos().set("limites_loja." + uuid.toString(), Math.max(0, atual + quantidade));
        plugin.saveSalvos();
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String linha1 = PlainTextComponentSerializer.plainText().serialize(event.line(0)).trim().toUpperCase();

        if (linha1.equals("C") || linha1.equals("V") || linha1.equals("COMPRA") || linha1.equals("VENDA")) {
            Block block = event.getBlock();

            // --- NOVA VERIFICAÇÃO DE TERRENO ---
            // Verifica se o local está protegido e se o jogador é o dono
            if (!sistemaTerrenos.isDono(player, block.getLocation())) {
                player.sendMessage("§c§l[!] §cVocê só pode criar lojas dentro de terrenos que você é o dono!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                event.setCancelled(true);
                block.breakNaturally();
                return;
            }
            // ----------------------------------

            boolean possuiLicenca = player.getPersistentDataContainer().has(licencaKey, PersistentDataType.BYTE);
            if (!possuiLicenca) {
                player.sendMessage("§c§l[!] §cVocê precisa adquirir uma Licença de Terreno no seu Perfil para criar lojas!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                event.setCancelled(true);
                block.breakNaturally();
                return;
            }

            if (!(block.getBlockData() instanceof Directional directional)) return;

            Block blocoTras = block.getRelative(directional.getFacing().getOppositeFace());
            if (blocoTras.getType() != Material.CHEST && blocoTras.getType() != Material.TRAPPED_CHEST && blocoTras.getType() != Material.BARREL) {
                player.sendMessage("§cAs placas de loja devem estar anexadas a um Baú ou Barril!");
                return;
            }

            int limiteMaximo = player.hasPermission("pica.loja.suporte") ? 20 : 10;
            if (getLojasCriadas(player.getUniqueId()) >= limiteMaximo) {
                player.sendMessage("§cVocê atingiu o limite máximo de " + limiteMaximo + " lojas!");
                event.setCancelled(true);
                block.breakNaturally();
                return;
            }

            try {
                int quantidade = Integer.parseInt(PlainTextComponentSerializer.plainText().serialize(event.line(1)).trim());
                String nomeItem = PlainTextComponentSerializer.plainText().serialize(event.line(2)).trim().toUpperCase().replace(" ", "_");
                Material material = Material.matchMaterial(nomeItem);
                double valor = Double.parseDouble(PlainTextComponentSerializer.plainText().serialize(event.line(3)).trim());

                if (material == null) {
                    player.sendMessage("§cItem não encontrado! Digite o nome em inglês (ex: DIAMOND).");
                    return;
                }
                if (quantidade <= 0 || valor <= 0) {
                    player.sendMessage("§cA quantidade e o valor devem ser maiores que zero!");
                    return;
                }

                boolean isVenda = linha1.startsWith("V");

                Sign signState = (Sign) block.getState();
                signState.getPersistentDataContainer().set(keyDono, PersistentDataType.STRING, player.getUniqueId().toString());
                signState.getPersistentDataContainer().set(keyTipo, PersistentDataType.STRING, isVenda ? "V" : "C");
                signState.getPersistentDataContainer().set(keyQtd, PersistentDataType.INTEGER, quantidade);
                signState.getPersistentDataContainer().set(keyItem, PersistentDataType.STRING, material.name());
                signState.getPersistentDataContainer().set(keyValor, PersistentDataType.DOUBLE, valor);
                signState.update();

                event.line(0, Component.text(isVenda ? "[VENDE-SE]" : "[COMPRA-SE]")
                        .color(isVenda ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD));
                event.line(1, Component.text(quantidade + "x").color(NamedTextColor.WHITE));
                event.line(2, Component.text(material.name()).color(NamedTextColor.YELLOW));
                event.line(3, Component.text("$" + econ.format(valor)).color(NamedTextColor.GOLD));

                alterarLojasCriadas(player.getUniqueId(), 1);
                player.sendMessage("§aLoja criada com sucesso! (" + getLojasCriadas(player.getUniqueId()) + "/" + limiteMaximo + ")");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);

            } catch (NumberFormatException e) {
                player.sendMessage("§cErro de formatação! Use: Linha 1: C/V (Compra / Venda)| L2: Quantidade | L3: Item (Em ingles) use o comando /itemnome com o item na mao | L4: Valor");
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) return;

        // Se tiver a key de dono, é uma loja
        if (sign.getPersistentDataContainer().has(keyDono, PersistentDataType.STRING)) {
            event.setCancelled(true); // Cancela para não abrir possíveis interações
            Player cliente = event.getPlayer();

            // Resgata os dados invisíveis
            UUID donoID = UUID.fromString(sign.getPersistentDataContainer().get(keyDono, PersistentDataType.STRING));
            String tipo = sign.getPersistentDataContainer().get(keyTipo, PersistentDataType.STRING);
            int qtd = sign.getPersistentDataContainer().get(keyQtd, PersistentDataType.INTEGER);
            Material mat = Material.matchMaterial(sign.getPersistentDataContainer().get(keyItem, PersistentDataType.STRING));
            double valor = sign.getPersistentDataContainer().get(keyValor, PersistentDataType.DOUBLE);

            if (cliente.getUniqueId().equals(donoID)) {
                cliente.sendMessage("§eEsta é a sua própria loja!");
                return;
            }

            // Acessa o inventário do Baú
            if (!(block.getBlockData() instanceof Directional directional)) return;
            Block blocoTras = block.getRelative(directional.getFacing().getOppositeFace());

            if (!(blocoTras.getState() instanceof Chest chest)) {
                cliente.sendMessage("§cO baú desta loja foi corrompido!");
                return;
            }

            ItemStack itemNegociado = new ItemStack(mat, qtd);

            // COMPRAR DA LOJA DO JOGADOR (Placa diz "V" - Vende-se)
            if (tipo.equals("V")) {
                if (!chest.getInventory().containsAtLeast(new ItemStack(mat), qtd)) {
                    cliente.sendMessage("§cEsta loja está sem estoque!");
                    return;
                }
                if (!econ.has(cliente, valor)) {
                    cliente.sendMessage("§cVocê não tem dinheiro suficiente. Custa $" + valor);
                    return;
                }

                // Transferência de itens e dinheiro
                econ.withdrawPlayer(cliente, valor);
                econ.depositPlayer(Bukkit.getOfflinePlayer(donoID), valor);

                chest.getInventory().removeItem(itemNegociado);
                cliente.getInventory().addItem(itemNegociado);

                cliente.sendMessage("§aVocê comprou " + qtd + "x " + mat.name() + " por $" + valor);
                cliente.playSound(cliente.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
            // VENDER PARA A LOJA DO JOGADOR (Placa diz "C" - Compra-se)
            else if (tipo.equals("C")) {
                if (!cliente.getInventory().containsAtLeast(new ItemStack(mat), qtd)) {
                    cliente.sendMessage("§cVocê não possui itens suficientes para vender!");
                    return;
                }

                // Verifica se o baú está cheio
                if (!chest.getInventory().addItem(itemNegociado.clone()).isEmpty()) {
                    cliente.sendMessage("§cO baú do comprador está cheio!");
                    return;
                }
                // Limpa o item que o addItem falho possa ter retornado (pois testamos em clone e cancelamos)
                chest.getInventory().removeItem(itemNegociado);

                if (!econ.has(Bukkit.getOfflinePlayer(donoID), valor)) {
                    cliente.sendMessage("§cO dono da loja não tem dinheiro para comprar seus itens!");
                    return;
                }

                // Transferência
                econ.withdrawPlayer(Bukkit.getOfflinePlayer(donoID), valor);
                econ.depositPlayer(cliente, valor);

                cliente.getInventory().removeItem(itemNegociado);
                chest.getInventory().addItem(itemNegociado);

                cliente.sendMessage("§aVocê vendeu " + qtd + "x " + mat.name() + " por $" + valor);
                cliente.playSound(cliente.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // 1. Se quebrou a PLACA da Loja
        if (block.getState() instanceof Sign sign) {
            if (sign.getPersistentDataContainer().has(keyDono, PersistentDataType.STRING)) {
                UUID donoID = UUID.fromString(sign.getPersistentDataContainer().get(keyDono, PersistentDataType.STRING));
                Player player = event.getPlayer();

                if (!player.getUniqueId().equals(donoID) && !player.hasPermission("pica.admin")) {
                    player.sendMessage("§cVocê não pode quebrar a loja de outro jogador!");
                    event.setCancelled(true);
                    return;
                }

                alterarLojasCriadas(donoID, -1);
                player.sendMessage("§eLoja desfeita com sucesso.");
            }
        }
        // 2. Se quebrou o BAÚ
        else if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST || block.getType() == Material.BARREL) {
            // Verifica todas as faces do baú para ver se tem uma placa de loja conectada
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP}) {
                Block placaPerto = block.getRelative(face);

                if (placaPerto.getState() instanceof Sign sign) {
                    if (sign.getPersistentDataContainer().has(keyDono, PersistentDataType.STRING)) {
                        // Confirma se a placa realmente está fixada NESTE baú e não no bloco ao lado
                        if (placaPerto.getBlockData() instanceof Directional directional) {
                            if (placaPerto.getRelative(directional.getFacing().getOppositeFace()).equals(block)) {

                                UUID donoID = UUID.fromString(sign.getPersistentDataContainer().get(keyDono, PersistentDataType.STRING));
                                if (!event.getPlayer().getUniqueId().equals(donoID) && !event.getPlayer().hasPermission("pica.admin")) {
                                    event.getPlayer().sendMessage("§cVocê não pode quebrar um baú que contém uma loja ativa!");
                                    event.setCancelled(true);
                                    return;
                                }

                                // Quebra a placa também e reembolsa o limite
                                alterarLojasCriadas(donoID, -1);
                                placaPerto.breakNaturally();
                                event.getPlayer().sendMessage("§eBaú e loja desfeitos.");
                            }
                        }
                    }
                }
            }
        }
    }
}