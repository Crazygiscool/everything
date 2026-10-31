package me.crazyg.everything.utils.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class EcoProvider {

    private final EcoStorage storage;

    public EcoProvider(EcoStorage storage) {
        this.storage = storage;
    }

        public boolean isEnabled() {
        return true;
    }

        public String getName() {
        return "EverythingEconomy";
    }

        public boolean hasBankSupport() {
        return false;
    }

        public int fractionalDigits() {
        return 2;
    }

        public String format(double amount) {
        return String.format("$%.2f", amount);
    }

        public String currencyNamePlural() {
        return "dollars";
    }

        public String currencyNameSingular() {
        return "dollar";
    }

    // ---------------------------------------------------------
    // Account Handling
    // ---------------------------------------------------------

        public boolean hasAccount(OfflinePlayer player) {
        return storage.hasAccount(player.getUniqueId());
    }

        public boolean hasAccount(String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

        public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

        public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

        public boolean createPlayerAccount(OfflinePlayer player) {
        return storage.createAccount(player.getUniqueId());
    }

        public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

        public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

        public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    // ---------------------------------------------------------
    // Balance
    // ---------------------------------------------------------

        public double getBalance(OfflinePlayer player) {
        return storage.getBalance(player.getUniqueId());
    }

        public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

        public double getBalance(OfflinePlayer player, String worldName) {
        return getBalance(player);
    }

        public double getBalance(String playerName, String worldName) {
        return getBalance(playerName);
    }

        public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

        public boolean has(String playerName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

        public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

        public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    // ---------------------------------------------------------
    // Withdraw
    // ---------------------------------------------------------

        public boolean withdrawPlayer(OfflinePlayer player, double amount) {
        double balance = getBalance(player);

        if (amount < 0) return false;
        if (balance < amount) return false;

        storage.setBalance(player.getUniqueId(), balance - amount);
        return true;
    }

        public boolean withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

        public boolean withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

        public boolean withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    // ---------------------------------------------------------
    // Deposit
    // ---------------------------------------------------------

        public boolean depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) return false;

        double balance = getBalance(player);
        storage.setBalance(player.getUniqueId(), balance + amount);
        return true;
    }

        public boolean depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

        public boolean depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

        public boolean depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    // ---------------------------------------------------------
    // Bank Support (disabled)
    // ---------------------------------------------------------

        public boolean createBank(String name, String player) {
        return notSupported();
    }

        public boolean createBank(String name, OfflinePlayer player) {
        return notSupported();
    }

        public boolean deleteBank(String name) {
        return notSupported();
    }

        public boolean bankBalance(String name) {
        return notSupported();
    }

        public boolean bankHas(String name, double amount) {
        return notSupported();
    }

        public boolean bankWithdraw(String name, double amount) {
        return notSupported();
    }

        public boolean bankDeposit(String name, double amount) {
        return notSupported();
    }

        public boolean isBankOwner(String name, String playerName) {
        return notSupported();
    }

        public boolean isBankOwner(String name, OfflinePlayer player) {
        return notSupported();
    }

        public boolean isBankMember(String name, String playerName) {
        return notSupported();
    }

        public boolean isBankMember(String name, OfflinePlayer player) {
        return notSupported();
    }

    private boolean notSupported() {
        return false;
    }

    // ---------------------------------------------------------
    // Misc
    // ---------------------------------------------------------

        public List<String> getBanks() {
        return null;
    }
}
