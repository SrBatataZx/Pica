package srbatata.gamesarelife.core;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import srbatata.gamesarelife.*;
import srbatata.gamesarelife.armor.ArmorManager;
import srbatata.gamesarelife.blocos.PedraTeleporte;
import srbatata.gamesarelife.comandos.CmdCarteira;
import srbatata.gamesarelife.comandos.CmdPagar;
import srbatata.gamesarelife.dados.GereWaystone;
import srbatata.gamesarelife.itens.AprendizConstruct;
import srbatata.gamesarelife.itens.OlhoDoTeleporte;
import srbatata.gamesarelife.itens.eventos.EvAutoColeta;
import srbatata.gamesarelife.itens.eventos.EvAxe;
import srbatata.gamesarelife.itens.eventos.EvPa;
import srbatata.gamesarelife.itens.eventos.EvPick;
import srbatata.gamesarelife.menus.*;
import srbatata.gamesarelife.sistemas.*;
import srbatata.gamesarelife.sistemas.comandos.*;
import srbatata.gamesarelife.util.ComandoItemNome;

public class PluginRegistry {

    private final Principal plugin;
    private final EcoImplement economia;
    private final GereWaystone gereWaystone; // Nova dependência

    public PluginRegistry(Principal plugin, EcoImplement economia, GereWaystone gereWaystone) {
        this.plugin = plugin;
        this.economia = economia;
        this.gereWaystone = gereWaystone;
    }

    public void registrarTudo() {

        // --- 1. SISTEMAS QUE DEPENDEM DE OUTROS ---
        SistemaTerrenos terrenos = new SistemaTerrenos(plugin);
        SistemaMissoes missoes = new SistemaMissoes(plugin, economia);
        MenuLoja loja = new MenuLoja(plugin, economia, terrenos);
        SistemaKits kits = new SistemaKits(plugin);
        ArmazemAprendiz armazemAprendiz = new ArmazemAprendiz(plugin);

        MenuPerfil menuPerfil = new MenuPerfil(plugin, missoes, armazemAprendiz, economia.dados());
        MenuPrincipal menuPrincipal = new MenuPrincipal(plugin, menuPerfil, loja, kits);

        new ArmorManager(plugin, terrenos);
        // --- 2. REGISTRO DE COMANDOS ---
        regCmd("picareta", new EvAutoColeta(plugin));
        regCmd("picaretaadmin", new EvAutoColeta(plugin));
        regCmd("machado", new AprendizConstruct(plugin));
        regCmd("carteira", new CmdCarteira(economia));
        regCmd("pagar", new CmdPagar(economia));
        regCmd("picareload", new ComandoRecarregar(plugin));
        regCmd("menu", menuPrincipal);

        // Terrenos
        regCmd("terreno", terrenos);

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

        regCmd("discord", new ComandoDiscord());
        regCmd("itemnome", new ComandoItemNome());
        regCmd("setlojaspawn", new ComandoSetLoja(plugin));

        // --- 3. REGISTRO DE EVENTOS ---
        regEvt(new EvPick(plugin));
        regEvt(new EvAxe(plugin));
        regEvt(new EvPa(plugin));
        regEvt(new ProtecaoFerramentas(plugin));
        regEvt(terrenos);
        regEvt(missoes);
        regEvt(loja);
        regEvt(kits);
        regEvt(menuPrincipal);
        regEvt(menuPerfil);
        regEvt(armazemAprendiz);
        regEvt(new PedraTeleporte(plugin, gereWaystone));
        regEvt(new OlhoDoTeleporte(plugin, gereWaystone));
        regEvt(new SistemaPesca(plugin, economia));
        regEvt(new SistemaMochila(plugin));
        regEvt(new SistemaLojaPlacas(plugin, economia, terrenos));
    }

    // Atalhos para economizar código
    private void regCmd(String nome, CommandExecutor executor) {
        if (plugin.getCommand(nome) != null) {
            plugin.getCommand(nome).setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter completer) {
                plugin.getCommand(nome).setTabCompleter(completer);
            }
        } else {
            plugin.getLogger().warning("Comando /" + nome + " nao encontrado no plugin.yml!");
        }
    }

    private void regEvt(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }
}