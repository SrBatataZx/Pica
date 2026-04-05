package srbatata.gamesarelife.sistemas;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SistemaChatLuckPerms implements Listener {

    private final LuckPerms luckPerms;

    public SistemaChatLuckPerms() {
        this.luckPerms = LuckPermsProvider.get();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoFalarNoChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        String prefixo = user.getCachedData().getMetaData().getPrefix();
        if (prefixo == null) prefixo = "";

        // 1. Prefixo colorido vindo do LuckPerms
        Component prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefixo);

        // 2. Nome do jogador com cor branca (resetando a cor do prefixo)
        Component nomeJogador = Component.text(player.getName()).color(NamedTextColor.WHITE);

        // 3. Separador e Mensagem também em branco
        Component separador = Component.text(": ").color(NamedTextColor.WHITE);
        Component mensagemOriginal = event.message().color(NamedTextColor.WHITE);

        // Montagem final: [Prefixo Colorido] [Nome Branco]: [Mensagem Branca]
        Component formatoFinal = prefixComponent
                .append(nomeJogador)
                .append(separador)
                .append(mensagemOriginal);

        event.renderer((source, sourceDisplayName, message, viewer) -> formatoFinal);
    }
}