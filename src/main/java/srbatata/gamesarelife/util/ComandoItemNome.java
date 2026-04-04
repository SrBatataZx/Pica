package srbatata.gamesarelife.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class ComandoItemNome implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // Utilizando Pattern Matching do Java (16+) para evitar o cast redundante
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Apenas jogadores podem executar este comando.", NamedTextColor.RED));
            return true;
        }

        // Obtém o item da mão principal do jogador
        ItemStack item = player.getInventory().getItemInMainHand();

        // Valida se a mão não está vazia
        if (item.getType() == Material.AIR || item.isEmpty()) {
            player.sendMessage(Component.text("Você não está segurando nenhum item na mão principal!", NamedTextColor.RED));
            return true;
        }

        // 1. Obtém o nome base da API (Material) - Este é o nome usado no código e no seu sistema de lojas
        String nomeMaterial = item.getType().name();

        player.sendMessage(Component.text("---------------------------------", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("Item ID (Material): ", NamedTextColor.YELLOW)
                .append(Component.text(nomeMaterial, NamedTextColor.WHITE)));

        // 2. Verifica se o item possui MetaData e um nome customizado
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component nomeCustomizado = meta.displayName();

            // Exibe o nome exatamente como aparece in-game (com as cores)
            player.sendMessage(Component.text("Nome Customizado (Com Cores): ", NamedTextColor.YELLOW)
                    .append(nomeCustomizado));

            // Converte o Component para uma String plana (remove cores e formatações)
            // Muito útil se você precisar salvar apenas os caracteres no banco de dados
            String textoPlano = PlainTextComponentSerializer.plainText().serialize(nomeCustomizado);
            player.sendMessage(Component.text("Nome Plano (Sem Cores): ", NamedTextColor.YELLOW)
                    .append(Component.text(textoPlano, NamedTextColor.GRAY)));
        }

        player.sendMessage(Component.text("---------------------------------", NamedTextColor.DARK_GRAY));

        return true;
    }
}