package srbatata.pica.core;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class GerenciadorDeContas {

    private final File arquivo;
    private final FileConfiguration config;

    public GerenciadorDeContas(Plugin plugin) {
        // Cria o arquivo contas.yml na pasta do plugin
        this.arquivo = new File(plugin.getDataFolder(), "contas.yml");
        if (!arquivo.exists()) {
            arquivo.getParentFile().mkdirs();
            try {
                arquivo.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(arquivo);
    }

    // Pega o saldo do jogador (se não existir, retorna 0)
    public double getSaldo(OfflinePlayer jogador) {
        return config.getDouble(jogador.getUniqueId().toString() + ".saldo", 0.0);
    }

    // Define o saldo e salva o arquivo
    public void setSaldo(OfflinePlayer jogador, double valor) {
        config.set(jogador.getUniqueId().toString() + ".saldo", valor);
        salvar();
    }

    private void salvar() {
        try {
            config.save(arquivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
