package me.crazyg.everything.utils.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class EcoProvider implements Economy {

    private final EcoStorage storage;

    public EcoProvider(EcoStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean isEnabled() {
        return true;
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
        return String.format("$%.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return "dollars";
    }

    @Override
    public String currencyNameSingular() {
        return "dollar";
    }

    // ---------------------------------------------------------
    // Account Handling
    // ---------------------------------------------------------

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return storage.hasAccount(player.getUniqueId());
    }

    @Override
    public boolean hasAccount(String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
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
        return storage.createAccount(player.getUniqueId());
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    // ---------------------------------------------------------
    // Balance
    // ---------------------------------------------------------

    @Override
    public double getBalance(OfflinePlayer player) {
        return storage.getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
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
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    // ---------------------------------------------------------
    // Withdraw
    // ---------------------------------------------------------

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        double balance = getBalance(player);

        if (amount < 0) {
            return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE,
                    "Cannot withdraw negative amounts");
        }

        if (balance < amount) {
            return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE,
                    "Insufficient funds");
        }

        storage.setBalance(player.getUniqueId(), balance - amount);
        return new EconomyResponse(amount, balance - amount, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    // ---------------------------------------------------------
    // Deposit
    // ---------------------------------------------------------

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE,
                    "Cannot deposit negative amounts");
        }

        double balance = getBalance(player);
        storage.setBalance(player.getUniqueId(), balance + amount);

        return new EconomyResponse(amount, balance + amount, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    // ---------------------------------------------------------
    // Bank Support (disabled)
    // ---------------------------------------------------------

    @Override
    public EconomyResponse createBank(String name, String player) {
        return notSupported();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return notSupported();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return notSupported();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return notSupported();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return notSupported();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return notSupported();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return notSupported();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return notSupported();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return notSupported();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return notSupported();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return notSupported();
    }

    private EconomyResponse notSupported() {
        return new EconomyResponse(0, 0,
                EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank support not implemented");
    }

    // ---------------------------------------------------------
    // Misc
    // ---------------------------------------------------------

    @Override
    public List<String> getBanks() {
        return null;
    }
}
