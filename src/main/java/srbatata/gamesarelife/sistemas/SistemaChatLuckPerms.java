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

        // 1. Convertemos o prefixo. Ele já contém a cor (ex: &6[VIP] )
        Component prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefixo);

        // 2. Criamos o nome do jogador.
        // Ao dar append no prefixComponent, o nome herda a última cor do prefixo.
        Component nomeJogador = Component.text(player.getName());

        // 3. Criamos o separador e a mensagem com cores resetadas
        // Usamos NamedTextColor.WHITE para garantir que a mensagem seja branca padrão
        Component separador = Component.text(": ").color(NamedTextColor.WHITE);
        Component mensagemOriginal = event.message().color(NamedTextColor.WHITE);

        // Montagem: [Prefixo][Nome] (herdam cor) + [Separador][Mensagem] (brancos)
        Component formatoFinal = prefixComponent
                .append(nomeJogador)
                .append(separador)
                .append(mensagemOriginal);

        event.renderer((source, sourceDisplayName, message, viewer) -> formatoFinal);
    }
}