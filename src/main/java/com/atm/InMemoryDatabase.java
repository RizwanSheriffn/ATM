package com.atm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * In-memory database for testing purposes
 */
public class InMemoryDatabase {
    private final Map<String, String> users = new HashMap<>();
    private final Map<String, Map<String, Double>> accounts = new HashMap<>();
    private final Map<String, List<String>> transactions = new HashMap<>();
    private final Map<String, List<String>> pinActivities = new HashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public InMemoryDatabase() {
        initializeDemoData();
    }

    public boolean authenticateUser(String userId, String pinHash) {
        return users.containsKey(userId) && users.get(userId).equals(pinHash);
    }

    public void updatePIN(String userId, String newPinHash) {
        if (users.containsKey(userId)) {
            users.put(userId, newPinHash);
        }
    }

    public Map<String, Double> getAccounts(String userId) {
        return accounts.getOrDefault(userId, new HashMap<>());
    }

    public void updateBalance(String userId, String accountType, double newBalance) {
        if (accounts.containsKey(userId)) {
            accounts.get(userId).put(accountType, newBalance);
        }
    }

    public void logTransaction(String userId, String type, double amount) {
        transactions.computeIfAbsent(userId, k -> new ArrayList<>())
            .add(String.format("%s - %s: %.2f",
                LocalDateTime.now().format(formatter),
                type,
                amount));
    }

    public void logPINActivity(String userId, String activity) {
        pinActivities.computeIfAbsent(userId, k -> new ArrayList<>())
            .add(String.format("%s - %s",
                LocalDateTime.now().format(formatter),
                activity));
    }

    public List<String> getTransactionHistory(String userId) {
        return new ArrayList<>(transactions.getOrDefault(userId, new ArrayList<>()));
    }

    public List<String> getPINActivityHistory(String userId) {
        return new ArrayList<>(pinActivities.getOrDefault(userId, new ArrayList<>()));
    }

    public boolean userExists(String userId) {
        return users.containsKey(userId);
    }

    private void initializeDemoData() {
        // Create users
        users.put("USER001", hashPIN("1234"));
        users.put("USER002", hashPIN("1234"));

        // Create accounts
        Map<String, Double> user001Accounts = new HashMap<>();
        user001Accounts.put("SAVINGS", 1000.0);
        user001Accounts.put("CHECKING", 500.0);
        accounts.put("USER001", user001Accounts);

        Map<String, Double> user002Accounts = new HashMap<>();
        user002Accounts.put("SAVINGS", 2000.0);
        user002Accounts.put("CHECKING", 1000.0);
        accounts.put("USER002", user002Accounts);
    }

    private String hashPIN(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
