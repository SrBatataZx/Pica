package srbatata.pica;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;

public class PluginRegistry {

    private final Pica plugin;
    private final EconomiaImplementacao economia;

    public PluginRegistry(Pica plugin, EconomiaImplementacao economia) {
        this.plugin = plugin;
        this.economia = economia;
    }

    public void registrarTudo() {
        // --- 1. SISTEMAS QUE DEPENDEM DE OUTROS ---
        SistemaTerrenos terrenos = new SistemaTerrenos(plugin);
        SistemaMissoes missoes = new SistemaMissoes(plugin, economia);
        SistemaLoja loja = new SistemaLoja(plugin, economia, terrenos);
        SistemaKits kits = new SistemaKits(plugin);
        MenuPrincipal menuPrincipal = new MenuPrincipal(plugin, missoes, loja, kits);

        // --- 2. REGISTRO DE COMANDOS ---
        regCmd("picareta", new ComandoPicareta(plugin));
        regCmd("picaretaadmin", new ComandoPicaretaAdmin(plugin));
        regCmd("machado", new ComandoMachado(plugin));
        regCmd("money", new ComandoMoney(economia));
        regCmd("picareload", new ComandoRecarregar(plugin));
        regCmd("menu", menuPrincipal);

        // Terrenos
        regCmd("proteger", terrenos);
        regCmd("desproteger", terrenos);
        regCmd("addamigo", terrenos);
        regCmd("removeamigo", terrenos);
        regCmd("claimlist", terrenos);

        // Teleporte e Utilidades
        SistemaBack back = new SistemaBack(plugin);
        regCmd("back", back);
        regEvt(back);

        SistemaSpawn spawn = new SistemaSpawn(plugin);
        regCmd("spawn", spawn);
        regCmd("setspawn", spawn);

        SistemaHomes homes = new SistemaHomes(plugin);
        regCmd("home", homes);
        regCmd("sethome", homes);
        regCmd("delhome", homes);

        SistemaTPA tpa = new SistemaTPA();
        regCmd("tpa", tpa);
        regCmd("tpaccept", tpa);
        regCmd("tpadeny", tpa);
        regCmd("rtp", new SistemaRTP());

        SistemaArenaPvP pvp = new SistemaArenaPvP(plugin, economia);
        regCmd("pvp", pvp);
        regEvt(pvp);

        // --- 3. REGISTRO DE EVENTOS ---
        regEvt(new EventosPicareta(plugin));
        regEvt(new EventosMachado(plugin));
        regEvt(new ProtecaoFerramentas(plugin));
        regEvt(terrenos);
        regEvt(missoes);
        regEvt(loja);
        regEvt(kits);
        regEvt(menuPrincipal);
        regEvt(new SistemaPesca(plugin, economia));
        regEvt(new SistemaMochila(plugin));
        regEvt(new SistemaArmaduraMinerador(plugin));
        regEvt(new SistemaArmadurasCustomizadas(plugin, terrenos));
    }

    // Atalhos para economizar código
    private void regCmd(String nome, CommandExecutor executor) {
        if (plugin.getCommand(nome) != null) {
            plugin.getCommand(nome).setExecutor(executor);
        } else {
            plugin.getLogger().warning("Comando /" + nome + " nao encontrado no plugin.yml!");
        }
    }

    private void regEvt(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }
}