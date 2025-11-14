package com.atm;

import java.util.Map;
import java.util.List;

/**
 * DatabaseManager that delegates to InMemoryDatabase
 * Can be extended to support real MySQL in production
 */
public class DatabaseManager {
    private static final InMemoryDatabase db;

    static {
        db = new InMemoryDatabase();
    }
    
    public boolean authenticateUser(String userId, String pinHash) {
        return db.authenticateUser(userId, pinHash);
    }

    public void updatePIN(String userId, String newPinHash) {
        db.updatePIN(userId, newPinHash);
    }

    public Map<String, Double> getAccounts(String userId) {
        return db.getAccounts(userId);
    }

    public void updateBalance(String userId, String accountType, double newBalance) {
        db.updateBalance(userId, accountType, newBalance);
    }

    public void logTransaction(String userId, String type, double amount) {
        db.logTransaction(userId, type, amount);
    }

    public void logPINActivity(String userId, String activity) {
        db.logPINActivity(userId, activity);
    }

    public void initializeDemoData() {
        // In-memory database already initialized with demo data
    }

    public List<String> getTransactionHistory(String userId) {
        return db.getTransactionHistory(userId);
    }

    public List<String> getPINActivityHistory(String userId) {
        return db.getPINActivityHistory(userId);
    }

    public boolean userExists(String userId) {
        return db.userExists(userId);
    }
}