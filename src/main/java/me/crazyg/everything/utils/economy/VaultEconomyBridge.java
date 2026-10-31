package me.crazyg.everything.utils.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

/**
 * Bridges Everything's internal economy to Vault, allowing Vault-aware plugins
 * to use it. This class is loaded only when Vault is actually present.
 */
public class VaultEconomyBridge implements Economy {

    private final EcoProvider provider;

    public VaultEconomyBridge(EcoProvider provider) {
        this.provider = provider;
    }

    private net.milkbowl.vault.economy.EconomyResponse success(
            double amount, double balance) {
        return new net.milkbowl.vault.economy.EconomyResponse(
            amount, balance,
            net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS,
            null);
    }

    private net.milkbowl.vault.economy.EconomyResponse failure(double balance,
                                                              String message) {
        return new net.milkbowl.vault.economy.EconomyResponse(
            0, balance,
            net.milkbowl.vault.economy.EconomyResponse.ResponseType.FAILURE,
            message);
    }

    @Override
    public boolean isEnabled() {
        return provider != null && provider.isEnabled();
    }

    @Override
    public String getName() {
        return "EverythingEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return provider.format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return "dollars";
    }

    @Override
    public String currencyNameSingular() {
        return "dollar";
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player != null && provider.hasAccount(player);
    }

    @Override
    public boolean hasAccount(String playerName) {
        return !playerName.isBlank() && hasAccount(
            org.bukkit.Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return player != null && provider.createPlayerAccount(player);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(org.bukkit.Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return provider.getBalance(player);
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(org.bukkit.Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(OfflinePlayer player, String worldName) {
        return getBalance(player);
    }

    @Override
    public double getBalance(String playerName, String worldName) {
        return getBalance(playerName);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return provider.has(player, amount);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return has(org.bukkit.Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return failure(getBalance(player), "Cannot withdraw negative amounts");
        }
        double balance = getBalance(player);
        if (balance < amount) {
            return failure(balance, "Insufficient funds");
        }
        boolean success = provider.withdrawPlayer(player, amount);
        return success
            ? success(amount, getBalance(player))
            : failure(getBalance(player), "Withdrawal failed");
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(org.bukkit.Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return failure(getBalance(player), "Cannot deposit negative amounts");
        }
        boolean success = provider.depositPlayer(player, amount);
        return success
            ? success(amount, getBalance(player))
            : failure(getBalance(player), "Deposit failed");
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(org.bukkit.Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public List<String> getBanks() {
        return null;
    }

    private EconomyResponse notImplemented() {
        return new EconomyResponse(
            0, 0,
            net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "Bank support not implemented");
    }
}
