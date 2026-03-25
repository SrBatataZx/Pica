package srbatata.pica;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;

public class EconomiaImplementacao implements Economy {

    private final GerenciadorDeContas dados;

    // Recebe o gerenciador pelo construtor
    public EconomiaImplementacao(GerenciadorDeContas dados) {
        this.dados = dados;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "MeuPluginDeEconomia";
    }

    @Override
    public String currencyNamePlural() {
        return "Moedas";
    }

    @Override
    public String currencyNameSingular() {
        return "Moeda";
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true; // No nosso sistema, todo mundo tem conta por padrão
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return dados.getSaldo(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    // MÉTODO DE REMOVER DINHEIRO
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Não pode remover valores negativos.");
        }

        double saldoAtual = getBalance(player);
        if (saldoAtual < amount) {
            return new EconomyResponse(0, saldoAtual, EconomyResponse.ResponseType.FAILURE, "Saldo insuficiente.");
        }

        dados.setSaldo(player, saldoAtual - amount);
        return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
    }

    // MÉTODO DE ADICIONAR DINHEIRO
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Não pode depositar valores negativos.");
        }

        double saldoAtual = getBalance(player);
        dados.setSaldo(player, saldoAtual + amount);
        return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
    }

    /* Abaixo estão os métodos obrigatórios do Vault que não vamos usar neste exemplo básico.
       Basta deixá-los assim:
    */
    @Override public boolean hasAccount(String playerName) { return true; }
    @Override public boolean hasAccount(String playerName, String worldName) { return hasAccount(playerName); }
    @Override public boolean hasAccount(OfflinePlayer player, String worldName) { return hasAccount(player); }

    @Override public double getBalance(String playerName) { return 0; }
    @Override public double getBalance(String playerName, String world) { return 0; }
    @Override public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }

    @Override public boolean has(String playerName, double amount) { return false; }
    @Override public boolean has(String playerName, String worldName, double amount) { return false; }
    @Override public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }

    @Override public EconomyResponse withdrawPlayer(String playerName, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use UUID"); }
    @Override public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return withdrawPlayer(playerName, amount); }
    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) { return withdrawPlayer(player, amount); }

    @Override public EconomyResponse depositPlayer(String playerName, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use UUID"); }
    @Override public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return depositPlayer(playerName, amount); }
    @Override public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) { return depositPlayer(player, amount); }

    @Override public boolean createPlayerAccount(String playerName) { return false; }
    @Override public boolean createPlayerAccount(OfflinePlayer player) { return true; }
    @Override public boolean createPlayerAccount(String playerName, String worldName) { return false; }
    @Override public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return true; }

    @Override public int fractionalDigits() { return 2; }
    @Override public String format(double amount) { return String.format("%.2f", amount) + " " + currencyNamePlural(); }

    // --- SISTEMA DE BANCOS (Não utilizado neste exemplo) ---
    @Override public boolean hasBankSupport() { return false; }
    @Override public EconomyResponse createBank(String name, String player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos desativados"); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos desativados"); }
    @Override public EconomyResponse deleteBank(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos desativados"); }
    @Override public EconomyResponse bankBalance(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos desativados"); }
    @Override public EconomyResponse bankHas(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos desativados"); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos desativados"); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos desativados"); }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos desativados"); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos desativados"); }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos desativados"); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bancos desativados"); }
    @Override public List<String> getBanks() { return Collections.emptyList(); }
}