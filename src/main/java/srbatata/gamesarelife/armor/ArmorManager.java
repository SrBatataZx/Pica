package srbatata.gamesarelife.armor;

import org.bukkit.Bukkit;
import srbatata.gamesarelife.sistemas.SistemaTerrenos;
import srbatata.gamesarelife.armor.armorconstruct.ExplorerArmor;
import srbatata.gamesarelife.armor.armorconstruct.minerarmor.MinerBoots;
import srbatata.gamesarelife.armor.armorconstruct.minerarmor.MinerChestplate;
import srbatata.gamesarelife.armor.armorconstruct.minerarmor.MinerHelmet;
import srbatata.gamesarelife.armor.armorconstruct.minerarmor.MinerLeggings;
import srbatata.gamesarelife.core.Principal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ArmorManager {

    // Usamos um Set para evitar duplicatas e unificar o armazenamento
    private final Set<CustomArmor> registeredArmors = new HashSet<>();

    public ArmorManager(Principal plugin, SistemaTerrenos terrenos) {
        // 1. Instanciar e Adicionar as armaduras à coleção
        registerArmors(plugin, terrenos);

        // 2. Registrar as receitas de todas as armaduras automaticamente
        registeredArmors.forEach(armor -> armor.registerRecipe(plugin));

        // 3. Iniciar o processamento das habilidades
        startAbilityLoop(plugin);
    }

    private void registerArmors(Principal plugin, SistemaTerrenos terrenos) {
        // Adicione aqui suas novas armaduras futuramente
        registeredArmors.add(new ExplorerArmor(plugin, terrenos));
        registeredArmors.add(new MinerChestplate(plugin));
        registeredArmors.add(new MinerHelmet(plugin));
        registeredArmors.add(new MinerBoots(plugin));
        registeredArmors.add(new MinerLeggings(plugin));
    }

    private void startAbilityLoop(Principal plugin) {
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