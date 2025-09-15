package com.atm;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;

public class DatabaseManager {
    private static HikariDataSource dataSource;
    
    static {
        initializeDatabase();
    }
    
    private static void initializeDatabase() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/atm_db");
        config.setUsername("atm_user");
        config.setPassword("AtmUser@123");
        config.setMaximumPoolSize(10);
        
        dataSource = new HikariDataSource(config);
        
        createTables();
    }
    
    private static void createTables() {
        String[] createTableSQL = {
            "CREATE TABLE IF NOT EXISTS users (" +
            "user_id VARCHAR(10) PRIMARY KEY," +
            "pin_hash VARCHAR(64) NOT NULL" +
            ")",
            
            "CREATE TABLE IF NOT EXISTS accounts (" +
            "account_id INT AUTO_INCREMENT PRIMARY KEY," +
            "user_id VARCHAR(10)," +
            "account_type VARCHAR(10)," +
            "balance DECIMAL(10,2)," +
            "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
            ")",
            
            "CREATE TABLE IF NOT EXISTS transactions (" +
            "transaction_id INT AUTO_INCREMENT PRIMARY KEY," +
            "user_id VARCHAR(10)," +
            "transaction_type VARCHAR(50)," +
            "amount DECIMAL(10,2)," +
            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
            ")",
            
            "CREATE TABLE IF NOT EXISTS pin_activity (" +
            "activity_id INT AUTO_INCREMENT PRIMARY KEY," +
            "user_id VARCHAR(10)," +
            "activity_desc VARCHAR(100)," +
            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
            ")"
        };
        
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            for (String sql : createTableSQL) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database tables", e);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public boolean authenticateUser(String userId, String pinHash) {
        String sql = "SELECT pin_hash FROM users WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("pin_hash").equals(pinHash);
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Authentication failed", e);
        }
    }
    
    public void updatePIN(String userId, String newPinHash) {
        String sql = "UPDATE users SET pin_hash = ? WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPinHash);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update PIN", e);
        }
    }
    
    public Map<String, Double> getAccounts(String userId) {
        Map<String, Double> accounts = new HashMap<>();
        String sql = "SELECT account_type, balance FROM accounts WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                accounts.put(rs.getString("account_type"), rs.getDouble("balance"));
            }
            return accounts;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get accounts", e);
        }
    }
    
    public void updateBalance(String userId, String accountType, double newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE user_id = ? AND account_type = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newBalance);
            stmt.setString(2, userId);
            stmt.setString(3, accountType);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update balance", e);
        }
    }
    
    public void logTransaction(String userId, String type, double amount) {
        String sql = "INSERT INTO transactions (user_id, transaction_type, amount) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, type);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to log transaction", e);
        }
    }
    
    public void logPINActivity(String userId, String activity) {
        String sql = "INSERT INTO pin_activity (user_id, activity_desc) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, activity);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to log PIN activity", e);
        }
    }
    
    // Method to initialize demo data
    public void initializeDemoData() {
        try (Connection conn = getConnection()) {
            // First, clear existing data
            String[] clearData = {
                "DELETE FROM pin_activity",
                "DELETE FROM transactions",
                "DELETE FROM accounts",
                "DELETE FROM users"
            };
            
            Statement stmt = conn.createStatement();
            for (String sql : clearData) {
                stmt.execute(sql);
            }
            
            // Insert demo users
            String insertUser = "INSERT INTO users (user_id, pin_hash) VALUES (?, ?)";
            PreparedStatement userStmt = conn.prepareStatement(insertUser);
            
            // USER001 with default PIN 1234
            userStmt.setString(1, "USER001");
            userStmt.setString(2, hashPIN("1234"));
            userStmt.executeUpdate();
            
            // USER002 with default PIN 1234
            userStmt.setString(1, "USER002");
            userStmt.setString(2, hashPIN("1234"));
            userStmt.executeUpdate();
            
            // Insert demo accounts
            String insertAccount = "INSERT INTO accounts (user_id, account_type, balance) VALUES (?, ?, ?)";
            PreparedStatement accountStmt = conn.prepareStatement(insertAccount);
            
            // Accounts for USER001
            accountStmt.setString(1, "USER001");
            accountStmt.setString(2, "SAVINGS");
            accountStmt.setDouble(3, 1000.0);
            accountStmt.executeUpdate();
            
            accountStmt.setString(1, "USER001");
            accountStmt.setString(2, "CHECKING");
            accountStmt.setDouble(3, 500.0);
            accountStmt.executeUpdate();
            
            // Accounts for USER002
            accountStmt.setString(1, "USER002");
            accountStmt.setString(2, "SAVINGS");
            accountStmt.setDouble(3, 2000.0);
            accountStmt.executeUpdate();
            
            accountStmt.setString(1, "USER002");
            accountStmt.setString(2, "CHECKING");
            accountStmt.setDouble(3, 1000.0);
            accountStmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize demo data", e);
        }
    }
    
    private String hashPIN(String pin) {
        // Using the same hashing method as ATM class
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

    public List<String> getTransactionHistory(String userId) {
        List<String> history = new ArrayList<>();
        String sql = "SELECT transaction_type, amount, timestamp FROM transactions WHERE user_id = ? ORDER BY timestamp DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String transaction = String.format("%s - %s: %.2f",
                    rs.getTimestamp("timestamp").toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    rs.getString("transaction_type"),
                    rs.getDouble("amount"));
                history.add(transaction);
            }
            return history;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get transaction history", e);
        }
    }

    public List<String> getPINActivityHistory(String userId) {
        List<String> history = new ArrayList<>();
        String sql = "SELECT activity_desc, timestamp FROM pin_activity WHERE user_id = ? ORDER BY timestamp DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String activity = String.format("%s - %s",
                    rs.getTimestamp("timestamp").toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    rs.getString("activity_desc"));
                history.add(activity);
            }
            return history;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get PIN activity history", e);
        }
    }

    public boolean userExists(String userId) {
        String sql = "SELECT 1 FROM users WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check user existence", e);
        }
    }
}