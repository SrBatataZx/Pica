package srbatata.pica.armor;

import org.bukkit.Bukkit;
import srbatata.pica.armor.armoconstructor.FireArmor;
import srbatata.pica.core.Pica;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ArmorManager {

    // Usamos um Set para evitar duplicatas e unificar o armazenamento
    private final Set<CustomArmor> registeredArmors = new HashSet<>();

    public ArmorManager(Pica plugin) {
        // 1. Instanciar e Adicionar as armaduras à coleção
        registerArmors(plugin);

        // 2. Registrar as receitas de todas as armaduras automaticamente
        registeredArmors.forEach(armor -> armor.registerRecipe(plugin));

        // 3. Iniciar o processamento das habilidades
        startAbilityLoop(plugin);
    }

    private void registerArmors(Pica plugin) {
        // Adicione aqui suas novas armaduras futuramente
        registeredArmors.add(new FireArmor(plugin));
    }

    private void startAbilityLoop(Pica plugin) {
        // Na 1.21, o scheduler do Bukkit ainda é padrão,
        // mas para efeitos visuais, poderíamos usar o Folia ou Async se necessário.
        // Mantendo Síncrono para garantir compatibilidade com buffs de atributos.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            // Loop otimizado: Só iteramos sobre jogadores online
            for (var player : Bukkit.getOnlinePlayers()) {
                for (var armor : registeredArmors) {
                    if (armor.isWearingFullSet(player)) {
                        // Pattern Matching e Optional (Java 21 style)
                        armor.getAbility().ifPresent(ability -> ability.execute(player));
                    }
                }
            }
        }, 0L, 20L); // 20 ticks = 1 segundo
    }

    /**
     * Retorna uma visão imutável das armaduras para segurança
     */
    public Set<CustomArmor> getRegisteredArmors() {
        return Collections.unmodifiableSet(registeredArmors);
    }
}