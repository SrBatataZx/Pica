package srbatata.gamesarelife.core;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import srbatata.gamesarelife.armor.ArmorManager;
import srbatata.gamesarelife.dados.GereWaystone;

import java.io.File;
import java.io.IOException;

public final class Principal extends JavaPlugin {

    private File salvosFile;
    private FileConfiguration salvosConfig;
    private EcoGerente gerenciadorContas;
    private ArmorManager armorManager;
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