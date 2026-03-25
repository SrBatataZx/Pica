package srbatata.pica.core;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import srbatata.pica.modules.economy.EconomiaImplementacao;
import srbatata.pica.modules.economy.GerenciadorDeContas;

import java.io.File;
import java.io.IOException;

public final class Pica extends JavaPlugin {

    private File salvosFile;
    private FileConfiguration salvosConfig;
    private GerenciadorDeContas gerenciadorContas;

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

        // Inicializa Economia
        this.gerenciadorContas = new GerenciadorDeContas(this);
        EconomiaImplementacao minhaEco = new EconomiaImplementacao(gerenciadorContas);
        getServer().getServicesManager().register(Economy.class, minhaEco, this, ServicePriority.Highest);

        // Chama o Registry para registrar comandos e eventos
        PluginRegistry registry = new PluginRegistry(this, minhaEco);
        registry.registrarTudo();

        getLogger().info("Plugin Pica iniciado com sucesso!");
    }

    @Override
    public void onDisable() {
        // Aproveite para salvar os dados ao desligar!
        if (gerenciadorContas != null) {
            // Se você implementou o cache que sugeri antes, chame o salvar aqui
        }
        saveSalvos();
        getLogger().info("Plugin Pica desativado.");
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