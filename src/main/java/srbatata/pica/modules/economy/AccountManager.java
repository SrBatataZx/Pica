package srbatata.pica.modules.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class AccountManager {

    private final File arquivo;
    private final FileConfiguration config;

    public AccountManager(Plugin plugin) {
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

        // SALVAMENTO ASSÍNCRONO: Salva o arquivo a cada 5 minutos (6000 ticks) sem travar o servidor
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::salvar, 6000L, 6000L);
    }

    public double getSaldo(OfflinePlayer jogador) {
        return config.getDouble(jogador.getUniqueId().toString() + ".saldo", 0.0);
    }

    public void setSaldo(OfflinePlayer jogador, double valor) {
        config.set(jogador.getUniqueId().toString() + ".saldo", valor);
        // REMOVIDO: A chamada salvar() ficava aqui e causava lag.
    }

    public void salvar() {
        try {
            config.save(arquivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}