package srbatata.gamesarelife.core;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import srbatata.gamesarelife.*;
import srbatata.gamesarelife.armor.ArmorManager;
import srbatata.gamesarelife.blocos.PedraTeleporte;
import srbatata.gamesarelife.dados.GereWaystone;
import srbatata.gamesarelife.itens.AprendizConstruct;
import srbatata.gamesarelife.itens.OlhoDoTeleporte;
import srbatata.gamesarelife.itens.eventos.EvAutoColeta;
import srbatata.gamesarelife.itens.eventos.EvAxe;
import srbatata.gamesarelife.itens.eventos.EvPa;
import srbatata.gamesarelife.itens.eventos.EvPick;
import srbatata.gamesarelife.dados.GereWaystone; // Import necessário

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
        SistemaLoja loja = new SistemaLoja(plugin, economia, terrenos);
        SistemaKits kits = new SistemaKits(plugin);
        MenuPrincipal menuPrincipal = new MenuPrincipal(plugin, missoes, loja, kits);
        new ArmorManager(plugin, terrenos);
        // --- 2. REGISTRO DE COMANDOS ---
        regCmd("picareta", new EvAutoColeta(plugin));
        regCmd("picaretaadmin", new EvAutoColeta(plugin));
        regCmd("machado", new AprendizConstruct(plugin));
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
        regEvt(new EvPick(plugin));
        regEvt(new EvAxe(plugin));
        regEvt(new EvPa(plugin));
        regEvt(new ProtecaoFerramentas(plugin));
        regEvt(terrenos);
        regEvt(missoes);
        regEvt(loja);
        regEvt(kits);
        regEvt(menuPrincipal);
        regEvt(new PedraTeleporte(plugin, gereWaystone));
        regEvt(new OlhoDoTeleporte(plugin, gereWaystone));
        regEvt(new SistemaPesca(plugin, economia));
        regEvt(new SistemaMochila(plugin));
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