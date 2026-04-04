package srbatata.gamesarelife.core;

import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import srbatata.gamesarelife.dados.GereWaystone;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask; // [MELHORIA] Import da Task

import java.io.File;
import java.io.IOException;

public final class Principal extends JavaPlugin {

    private File salvosFile;
    private FileConfiguration salvosConfig;
    private EcoGerente gerenciadorContas;
    private GereWaystone gereWaystone;

    private BukkitTask discordTask;

    // [MELHORIA] Guarda o Registry para poder acessar o método de desligamento
    private PluginRegistry registry;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault não encontrado! Desativando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        criarArquivoSalvos();

        this.gereWaystone = new GereWaystone(this);
        this.gerenciadorContas = new EcoGerente(this);
        EcoImplement minhaEco = new EcoImplement(gerenciadorContas);
        getServer().getServicesManager().register(Economy.class, minhaEco, this, ServicePriority.Highest);

        // Guarda a instância na variável de classe em vez de ser apenas local
        this.registry = new PluginRegistry(this, minhaEco, gereWaystone);
        this.registry.registrarTudo();

        getLogger().info("Plugin Principal iniciado com sucesso!");

        this.discordTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) return;

                TextComponent linha1 = new TextComponent("§9§lDISCORD DO SERVIDOR");
                TextComponent linha2 = new TextComponent("§fJunte-se a nós para novidades, suporte e muito mais!");

                TextComponent mensagemLink = new TextComponent("§8» §eClique aqui para entrar: ");
                TextComponent link = new TextComponent("§b§nhttp://discord.gg/euh75ek2nZ");

                link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/euh75ek2nZ"));
                link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aAbrir Discord")));

                mensagemLink.addExtra(link);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage("");
                    p.spigot().sendMessage(linha1);
                    p.spigot().sendMessage(linha2);
                    p.spigot().sendMessage(mensagemLink);
                    p.sendMessage("");
                }
            }
        }.runTaskTimer(this, 20L * 60 * 10, 20L * 60 * 10);
    }

    @Override
    public void onDisable() {
        // Desliga tarefas locais do Principal
        if (this.discordTask != null && !this.discordTask.isCancelled()) {
            this.discordTask.cancel();
        }

        // [MELHORIA APLICADA] Desliga todos os sub-sistemas em cascata através do Registry
        if (this.registry != null) {
            this.registry.desativarSistemas();
        }

        saveSalvos();
        getLogger().info("Plugin Principal desativado com sucesso. Memória limpa!");
    }

    // ==========================================
    // GERENCIADOR DO ARQUIVO salvos.yml
    // ==========================================
    private void criarArquivoSalvos() {
        salvosFile = new File(getDataFolder(), "salvos.yml");
        if (!salvosFile.exists()) {
            salvosFile.getParentFile().mkdirs();
            try {
                salvosFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        salvosConfig = YamlConfiguration.loadConfiguration(salvosFile);
    }

    public FileConfiguration getSalvos() {
        return salvosConfig;
    }

    public void saveSalvos() {
        try {
            salvosConfig.save(salvosFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recarregarSalvos() {
        if (salvosFile == null) {
            salvosFile = new File(getDataFolder(), "salvos.yml");
        }
        salvosConfig = YamlConfiguration.loadConfiguration(salvosFile);
    }

    public String getMsgSemPermissao() {
        String msg = getConfig().getString("mensagens.sem_permissao", "&cVocê não tem permissão!");
        return msg.replace("&", "§");
    }
}