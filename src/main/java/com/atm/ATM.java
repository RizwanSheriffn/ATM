package com.atm;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ATM {
    private String currentUserId;
    private final Scanner scanner;
    private final NumberFormat currencyFormat;
    private final DatabaseManager dbManager;

    public ATM() {
        this.scanner = new Scanner(System.in);
        this.currencyFormat = NumberFormat.getCurrencyInstance();
        this.dbManager = new DatabaseManager();
        this.currentUserId = "USER001"; // Default user
        dbManager.initializeDemoData(); // Initialize demo data in database
    }

    // Constructor for testing
    public ATM(String initialPin) {
        this.scanner = new Scanner(System.in);
        this.currencyFormat = NumberFormat.getCurrencyInstance();
        this.dbManager = new DatabaseManager();
        this.currentUserId = "USER001";
        dbManager.initializeDemoData(); // Initialize fresh test data
    }

    public boolean authenticateWithPIN(String pin) {
        boolean isValid = dbManager.authenticateUser(currentUserId, hashPIN(pin));
        dbManager.logPINActivity(currentUserId, isValid ? "Successful PIN authentication" : "Failed PIN authentication attempt");
        return isValid;
    }

    public double getBalance(String userId, String accountType) {
        Map<String, Double> accounts = dbManager.getAccounts(userId);
        return accounts.getOrDefault(accountType, 0.0);
    }

    public void performTransfer(String sourceUserId, String sourceAccount, 
                              String destUserId, String destAccount, double amount) {
        if (validateWithdrawal(sourceAccount, amount)) {
            double sourceBalance = getBalance(sourceUserId, sourceAccount);
            double destBalance = getBalance(destUserId, destAccount);
            
            dbManager.updateBalance(sourceUserId, sourceAccount, sourceBalance - amount);
            dbManager.updateBalance(destUserId, destAccount, destBalance + amount);
            dbManager.logTransaction(sourceUserId, "Transfer from " + sourceAccount + " to " + destAccount, amount);
        }
    }

    public boolean changePIN(String currentPIN, String newPIN) {
        if (!dbManager.authenticateUser(currentUserId, hashPIN(currentPIN))) {
            dbManager.logPINActivity(currentUserId, "Failed PIN change - incorrect current PIN");
            return false;
        }

        if (!newPIN.matches("\\d{4}")) {
            dbManager.logPINActivity(currentUserId, "Failed PIN change - invalid format");
            return false;
        }

        dbManager.updatePIN(currentUserId, hashPIN(newPIN));
        dbManager.logPINActivity(currentUserId, "Successful PIN change");
        return true;
    }

    public void processCardlessTransaction(String type, String account, double amount, String code) {
        System.out.println("Processing " + type + "...");
        try {
            Thread.sleep(1500);
            if (type.equals("withdrawal") && !validateWithdrawal(account, amount)) {
                return;
            }

            double currentBalance = getBalance(currentUserId, account);
            double newBalance = type.equals("withdrawal") ? 
                              currentBalance - amount : 
                              currentBalance + amount;
            
            dbManager.updateBalance(currentUserId, account, newBalance);
            String confirmationCode = generateConfirmationCode();
            
            System.out.println("\nTransaction successful!");
            System.out.println("Confirmation code: " + confirmationCode);
            System.out.println(type.substring(0, 1).toUpperCase() + type.substring(1) + " code: " + code);
            System.out.println("New balance: " + currencyFormat.format(newBalance));
            
            dbManager.logTransaction(currentUserId, "Cardless " + type + " (" + code + ")", amount);
        } catch (InterruptedException e) {
            System.out.println("Process interrupted. Please try again.");
        }
    }

    private boolean validateWithdrawal(String account, double amount) {
        double balance = getBalance(currentUserId, account);
        if (balance < amount) {
            System.out.println("Insufficient funds.");
            return false;
        }
        return true;
    }

    // Keeping utility methods unchanged
    private String generateConfirmationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
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
            throw new RuntimeException("SHA-256 not available");
        }
    }

    private void displayMenu() {
        System.out.println("\n========= ATM Menu =========");
        System.out.println("1. Deposit Options");
        System.out.println("2. Withdrawal Options");
        System.out.println("3. Check Balance");
        System.out.println("4. Change PIN");
        System.out.println("5. Mini Statement (Last 5)");
        System.out.println("6. Transfer Options");
        System.out.println("7. View Transaction History");
        System.out.println("8. PIN Activity Statement");
        System.out.println("9. Exit");
        System.out.print("Enter your choice: ");
    }

    public void start() {
        if (authenticateUser()) {
            run();
        } else {
            System.out.println("Too many failed attempts. Exiting system.");
        }
    }

    public void run() {
        int choice;
        do {
            displayMenu();
            choice = getIntInput();
            switch (choice) {
                case 1: handleDepositOptions(); break;
                case 2: handleWithdrawalOptions(); break;
                case 3: showBalance(); break;
                case 4: changePIN(); break;
                case 5: showMiniStatement(); break;
                case 6: handleTransferOptions(); break;
                case 7: showTransactionHistory(); break;
                case 8: showPINActivityStatement(); break;
                case 9: System.out.println("Thank you for using the ATM. Goodbye!"); break;
                default: System.out.println("Invalid choice.");
            }
        } while (choice != 9);
    }

    private void handleDepositOptions() {
        int choice;
        do {
            displayDepositMenu();
            choice = getIntInput();
            switch (choice) {
                case 1: cashDeposit(); break;
                case 2: checkDeposit(); break;
                case 3: cardlessDeposit(); break;
                case 4: System.out.println("Returning to main menu..."); break;
                default: System.out.println("Invalid choice.");
            }
        } while (choice != 4);
    }

    private void handleWithdrawalOptions() {
        int choice;
        do {
            displayWithdrawalMenu();
            choice = getIntInput();
            switch (choice) {
                case 1: quickWithdraw(50); break;
                case 2: quickWithdraw(100); break;
                case 3: quickWithdraw(200); break;
                case 4: customWithdraw(); break;
                case 5: cardlessWithdraw(); break;
                case 6: System.out.println("Returning to main menu..."); break;
                default: System.out.println("Invalid choice.");
            }
        } while (choice != 6);
    }

    private void handleTransferOptions() {
        int choice;
        do {
            displayTransferMenu();
            choice = getIntInput();
            switch (choice) {
                case 1: transferBetweenAccounts(); break;
                case 2: transferToAnotherUser(); break;
                case 3: System.out.println("Returning to main menu..."); break;
                default: System.out.println("Invalid choice.");
            }
        } while (choice != 3);
    }

    private void cashDeposit() {
        String account = selectAccount("deposit to");
        if (account == null) return;
        
        double amount = getPositiveDoubleInput("Enter amount to deposit: ");
        double currentBalance = getBalance(currentUserId, account);
        dbManager.updateBalance(currentUserId, account, currentBalance + amount);
        
        System.out.println("Deposit successful!");
        System.out.println("New balance: " + currencyFormat.format(currentBalance + amount));
        dbManager.logTransaction(currentUserId, "Cash Deposit to " + account, amount);
    }

    private void checkDeposit() {
        String account = selectAccount("deposit to");
        if (account == null) return;
        
        double amount = getPositiveDoubleInput("Enter check amount: ");
        System.out.println("Processing check...");
        try {
            Thread.sleep(2000);
            double currentBalance = getBalance(currentUserId, account);
            dbManager.updateBalance(currentUserId, account, currentBalance + amount);
            System.out.println("Check deposit successful!");
            System.out.println("New balance: " + currencyFormat.format(currentBalance + amount));
            dbManager.logTransaction(currentUserId, "Check Deposit to " + account, amount);
        } catch (InterruptedException e) {
            System.out.println("Process interrupted. Please try again.");
        }
    }

    private void cardlessDeposit() {
        System.out.println("\n=== Cardless Deposit ===");
        String depositCode = getAndValidateCode("deposit");
        if (depositCode == null) return;

        String account = selectAccount("deposit to");
        if (account == null) return;

        double amount = getPositiveDoubleInput("Enter amount to deposit: ");
        processCardlessTransaction("deposit", account, amount, depositCode);
    }

    private void quickWithdraw(double amount) {
        String account = selectAccount("withdraw from");
        if (account == null) return;
        
        if (validateWithdrawal(account, amount)) {
            performWithdrawal(account, amount);
        }
    }

    private void customWithdraw() {
        String account = selectAccount("withdraw from");
        if (account == null) return;
        
        double amount = getPositiveDoubleInput("Enter withdrawal amount: ");
        if (validateWithdrawal(account, amount)) {
            performWithdrawal(account, amount);
        }
    }

    private void cardlessWithdraw() {
        System.out.println("\n=== Cardless Withdrawal ===");
        String withdrawalCode = getAndValidateCode("withdrawal");
        if (withdrawalCode == null) return;

        String account = selectAccount("withdraw from");
        if (account == null) return;

        double amount = getPositiveDoubleInput("Enter amount to withdraw: ");
        if (validateWithdrawal(account, amount)) {
            processCardlessTransaction("withdrawal", account, amount, withdrawalCode);
        }
    }

    private void transferBetweenAccounts() {
        System.out.println("\nAvailable accounts:");
        Map<String, Double> accounts = dbManager.getAccounts(currentUserId);
        for (Map.Entry<String, Double> entry : accounts.entrySet()) {
            System.out.println(entry.getKey() + ": " + currencyFormat.format(entry.getValue()));
        }

        String sourceAccount = selectAccount("transfer from");
        if (sourceAccount == null) return;

        String destAccount = selectAccount("transfer to");
        if (destAccount == null) return;

        if (sourceAccount.equals(destAccount)) {
            System.out.println("Cannot transfer to the same account.");
            return;
        }

        double amount = getPositiveDoubleInput("Enter amount to transfer: ");
        if (validateWithdrawal(sourceAccount, amount)) {
            double sourceBalance = getBalance(currentUserId, sourceAccount);
            double destBalance = getBalance(currentUserId, destAccount);
            dbManager.updateBalance(currentUserId, sourceAccount, sourceBalance - amount);
            dbManager.updateBalance(currentUserId, destAccount, destBalance + amount);
            System.out.println("Transfer successful!");
            dbManager.logTransaction(currentUserId, "Transfer from " + sourceAccount + " to " + destAccount, amount);
        }
    }

    private void transferToAnotherUser() {
        System.out.println("\nYour accounts:");
        Map<String, Double> sourceAccounts = dbManager.getAccounts(currentUserId);
        for (Map.Entry<String, Double> entry : sourceAccounts.entrySet()) {
            System.out.println(entry.getKey() + ": " + currencyFormat.format(entry.getValue()));
        }

        String sourceAccount = selectAccount("transfer from");
        if (sourceAccount == null) return;

        System.out.print("Enter recipient's User ID: ");
        String recipientId = scanner.nextLine().toUpperCase();
        if (!dbManager.userExists(recipientId)) {
            System.out.println("Recipient not found.");
            return;
        }

        String destAccount = selectAccount("transfer to", recipientId);
        if (destAccount == null) return;

        double amount = getPositiveDoubleInput("Enter amount to transfer: ");
        if (validateWithdrawal(sourceAccount, amount)) {
            double sourceBalance = getBalance(currentUserId, sourceAccount);
            double destBalance = getBalance(recipientId, destAccount);
            dbManager.updateBalance(currentUserId, sourceAccount, sourceBalance - amount);
            dbManager.updateBalance(recipientId, destAccount, destBalance + amount);
            System.out.println("Transfer successful!");
            dbManager.logTransaction(currentUserId, "Transfer to " + recipientId + "'s " + destAccount, amount);
        }
    }

    private void showTransactionHistory() {
        System.out.println("\n=== Transaction History ===");
        List<String> transactionHistory = dbManager.getTransactionHistory(currentUserId);
        if (transactionHistory.isEmpty()) {
            System.out.println("No transactions to show.");
        } else {
            for (String transaction : transactionHistory) {
                System.out.println(transaction);
            }
        }
    }

    private void showMiniStatement() {
        System.out.println("\n=== Mini Statement (Last 5) ===");
        List<String> transactionHistory = dbManager.getTransactionHistory(currentUserId);
        if (transactionHistory.isEmpty()) {
            System.out.println("No transactions to show.");
        } else {
            int start = Math.max(transactionHistory.size() - 5, 0);
            for (int i = start; i < transactionHistory.size(); i++) {
                System.out.println(transactionHistory.get(i));
            }
        }
    }

    private void showPINActivityStatement() {
        System.out.println("\n=== PIN Activity Statement ===");
        List<String> pinActivityHistory = dbManager.getPINActivityHistory(currentUserId);
        if (pinActivityHistory.isEmpty()) {
            System.out.println("No PIN activities to show.");
        } else {
            for (String activity : pinActivityHistory) {
                System.out.println(activity);
            }
        }
    }

    public List<String> getTransactionHistory() {
        return dbManager.getTransactionHistory(currentUserId);
    }

    public List<String> getMiniStatement() {
        List<String> history = dbManager.getTransactionHistory(currentUserId);
        if (history.size() <= 5) {
            return history;
        }
        return history.subList(history.size() - 5, history.size());
    }

    public List<String> getPINActivityHistory() {
        return dbManager.getPINActivityHistory(currentUserId);
    }

    private String selectAccount(String action) {
        return selectAccount(action, currentUserId);
    }

    private String selectAccount(String action, String userId) {
        Map<String, Double> accounts = dbManager.getAccounts(userId);
        System.out.println("\nAvailable accounts:");
        for (String accountType : accounts.keySet()) {
            System.out.println(accountType);
        }
        
        System.out.print("Select account to " + action + " (SAVINGS/CHECKING): ");
        String account = scanner.nextLine().toUpperCase();
        if (!accounts.containsKey(account)) {
            System.out.println("Invalid account.");
            return null;
        }
        return account;
    }

    private int getIntInput() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a number: ");
            }
        }
    }

    private double getPositiveDoubleInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double amount = Double.parseDouble(scanner.nextLine().trim());
                if (amount <= 0) {
                    System.out.println("Amount must be positive.");
                    continue;
                }
                return amount;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format.");
            }
        }
    }

    private void displayDepositMenu() {
        System.out.println("\n=== Deposit Options ===");
        System.out.println("1. Cash Deposit");
        System.out.println("2. Check Deposit");
        System.out.println("3. Cardless Deposit");
        System.out.println("4. Back to Main Menu");
    }

    private void displayWithdrawalMenu() {
        System.out.println("\n=== Withdrawal Options ===");
        System.out.println("1. Quick Cash ($50)");
        System.out.println("2. Quick Cash ($100)");
        System.out.println("3. Quick Cash ($200)");
        System.out.println("4. Custom Amount");
        System.out.println("5. Cardless Withdrawal");
        System.out.println("6. Back to Main Menu");
    }

    private void displayTransferMenu() {
        System.out.println("\n=== Transfer Options ===");
        System.out.println("1. Transfer Between My Accounts");
        System.out.println("2. Transfer to Another User");
        System.out.println("3. Back to Main Menu");
    }

    private String getAndValidateCode(String type) {
        System.out.print("Enter 6-digit " + type + " code: ");
        String code = scanner.nextLine();
        if (!code.matches("\\d{6}")) {
            System.out.println("Invalid code format. Must be 6 digits.");
            return null;
        }
        return code;
    }

    private void performWithdrawal(String account, double amount) {
        double currentBalance = getBalance(currentUserId, account);
        dbManager.updateBalance(currentUserId, account, currentBalance - amount);
        System.out.println("Withdrawal successful!");
        System.out.println("Remaining balance: " + currencyFormat.format(currentBalance - amount));
        dbManager.logTransaction(currentUserId, "Withdrawal from " + account, amount);
    }

    private boolean authenticateUser() {
        int attempts = 0;
        while (attempts < 3) {
            System.out.print("Enter your 4-digit PIN: ");
            String enteredPIN = scanner.nextLine();
            if (authenticateWithPIN(enteredPIN)) {
                System.out.println("Login successful.\n");
                return true;
            } else {
                System.out.println("Incorrect PIN. Try again.");
                attempts++;
            }
        }
        return false;
    }

    private void showBalance() {
        System.out.println("\n=== Account Balances ===");
        Map<String, Double> accounts = dbManager.getAccounts(currentUserId);
        for (Map.Entry<String, Double> account : accounts.entrySet()) {
            System.out.println(account.getKey() + ": " + currencyFormat.format(account.getValue()));
        }
    }

    private void changePIN() {
        System.out.print("Enter current PIN: ");
        String currentPIN = scanner.nextLine();
        
        System.out.print("Enter new 4-digit PIN: ");
        String newPIN = scanner.nextLine();
        
        if (changePIN(currentPIN, newPIN)) {
            System.out.println("PIN changed successfully.");
        } else {
            System.out.println("Failed to change PIN.");
        }
    }

    public static void main(String[] args) {
        ATM atm = new ATM();
        atm.start();
    }
}