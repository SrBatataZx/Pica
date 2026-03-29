package srbatata.gamesarelife.dados;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GereWaystone {

    private final JavaPlugin plugin;
    private final File arquivo;
    private FileConfiguration config;

    public GereWaystone(JavaPlugin plugin) {
        this.plugin = plugin;
        // Define o nome e o diretório do novo arquivo
        this.arquivo = new File(plugin.getDataFolder(), "waystones.yml");
        carregar();
    }

    // Carrega o arquivo para a memória ou cria um novo se não existir
    public void carregar() {
        if (!arquivo.exists()) {
            arquivo.getParentFile().mkdirs();
            try {
                arquivo.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Não foi possível criar o arquivo waystones.yml!");
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(arquivo);
    }

    // Salva as alterações feitas na memória para o disco
    public void salvar() {
        try {
            config.save(arquivo);
        } catch (IOException e) {
            plugin.getLogger().severe("Não foi possível salvar o arquivo waystones.yml!");
        }
    }

    // Métodos utilitários para abstrair a lógica da Configuração
    public List<String> getWaystonesAtivas() {
        return config.getStringList("waystones_ativas");
    }

    public void adicionarWaystone(String locString) {
        List<String> waystones = getWaystonesAtivas();
        if (!waystones.contains(locString)) {
            waystones.add(locString);
            config.set("waystones_ativas", waystones);
            salvar();
        }
    }

    public void removerWaystone(String locString) {
        List<String> waystones = getWaystonesAtivas();
        if (waystones.contains(locString)) {
            waystones.remove(locString);
            config.set("waystones_ativas", waystones);
            salvar();
        }
    }
}