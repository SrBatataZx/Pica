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

import java.io.File;
import java.io.IOException;

public final class Principal extends JavaPlugin {

    private File salvosFile;
    private FileConfiguration salvosConfig;
    private EcoGerente gerenciadorContas;
    private GereWaystone gereWaystone; // Nossa nova variável

    public static @NotNull String getPlugin() {
        return "";
    }

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault não encontrado! Desativando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Inicializa Arquivos
        saveDefaultConfig();
        criarArquivoSalvos();

        this.gereWaystone = new GereWaystone(this);
        // Inicializa Economia
        this.gerenciadorContas = new EcoGerente(this);
        EcoImplement minhaEco = new EcoImplement(gerenciadorContas);
        getServer().getServicesManager().register(Economy.class, minhaEco, this, ServicePriority.Highest);

        // Chama o Registry para registrar comandos e eventos
        PluginRegistry registry = new PluginRegistry(this, minhaEco, gereWaystone);
        registry.registrarTudo();

        getLogger().info("Plugin Principal iniciado com sucesso!");

        new BukkitRunnable() {
            @Override
            public void run() {
                // Verifica se há jogadores online para não processar sem necessidade
                if (Bukkit.getOnlinePlayers().isEmpty()) return;

                // 1. Criar as linhas de texto simples
                // Usamos TextComponent para tudo para manter o padrão spigot().sendMessage
                TextComponent linha1 = new TextComponent("§9§lDISCORD DO SERVIDOR");
                TextComponent linha2 = new TextComponent("§fJunte-se a nós para novidades, suporte e muito mais!");

                // 2. Montar a linha clicável
                TextComponent mensagemLink = new TextComponent("§8» §eClique aqui para entrar: ");
                TextComponent link = new TextComponent("§b§nhttp://discord.gg/euh75ek2nZ");

                // Configurar o clique e o hover (passar o mouse)
                link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/euh75ek2nZ"));
                link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text("§aAbrir Discord")));

                mensagemLink.addExtra(link);

                // Envia para todos online
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(""); // Linha em branco estética
                    p.spigot().sendMessage(linha1);
                    p.spigot().sendMessage(linha2);
                    p.spigot().sendMessage(mensagemLink);
                    p.sendMessage(""); // Linha em branco estética
                }
            }
        }.runTaskTimer(this, 20L * 60 * 10, 20L * 60 * 10);
    }

    @Override
    public void onDisable() {
        // Aproveite para salvar os dados ao desligar!
        if (gerenciadorContas != null) {
            // Se você implementou o cache que sugeri antes, chame o salvar aqui
        }
        saveSalvos();
        getLogger().info("Plugin Principal desativado.");
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

    // ADICIONE ESTE NOVO MÉTODO AQUI:
    public void recarregarSalvos() {
        if (salvosFile == null) {
            salvosFile = new File(getDataFolder(), "salvos.yml");
        }
        salvosConfig = YamlConfiguration.loadConfiguration(salvosFile);
    }

    // ==========================================
    // SISTEMA DE MENSAGENS PADRÃO
    // ==========================================
    public String getMsgSemPermissao() {
        // Puxa da config. Se não existir lá, usa uma mensagem de segurança
        String msg = getConfig().getString("mensagens.sem_permissao", "&cVocê não tem permissão!");
        return msg.replace("&", "§"); // Transforma o & em cor do Minecraft
    }
}