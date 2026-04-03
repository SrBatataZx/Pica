package srbatata.gamesarelife;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class ComandoDiscord implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verifica se quem digitou o comando foi um jogador (o console não pode clicar)
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem testar este comando.");
            return true;
        }

        Player player = (Player) sender;

        // Envia o cabeçalho
        player.sendMessage("");
        player.sendMessage("§9§lDISCORD DO SERVIDOR");
        player.sendMessage("§fJunte-se a nós para novidades, suporte e muito mais!");

        // 1. Cria a parte do texto normal
        TextComponent mensagem = new TextComponent("§eClique aqui para entrar: ");

        // 2. Cria a parte do texto que será o link visível (com cor e sublinhado)
        TextComponent link = new TextComponent("§b§nhttp://discord.gg/euh75ek2nZ");

        // 3. Adiciona a função de clicar no link (ATENÇÃO: É obrigatório ter o https:// aqui)
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/euh75ek2nZ"));

        // 4. (Opcional) Mostra uma mensagem ao passar o mouse por cima
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aClique para abrir o convite!")));

        // Junta o link clicável à mensagem inicial
        mensagem.addExtra(link);

        // 5. Envia a mensagem usando a API do Spigot (Isso é o que faz o clique funcionar)
        player.spigot().sendMessage(mensagem);

        player.sendMessage("");

        return true;
    }
}
