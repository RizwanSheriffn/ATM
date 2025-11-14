package com.atm;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ATMTest {
    private ATM atm;
    
    @Before
    public void setUp() {
        // Initialize ATM with memory-only storage for testing
        atm = new ATM("1234"); // Pass initial PIN directly
    }

    @Test
    public void testAuthentication() {
        assertTrue("Authentication should succeed with default PIN", atm.authenticateWithPIN("1234"));
        assertFalse("Authentication should fail with wrong PIN", atm.authenticateWithPIN("0000"));
    }

    @Test
    public void testCardlessDeposit() {
        double initialBalance = atm.getBalance("USER001", "SAVINGS");
        double depositAmount = 500.0;
        
        atm.processCardlessTransaction("deposit", "SAVINGS", depositAmount, "123456");
        
        assertEquals("Balance should increase after cardless deposit",
            initialBalance + depositAmount, 
            atm.getBalance("USER001", "SAVINGS"),
            0.01);
    }

    @Test
    public void testCardlessWithdrawal() {
        double initialBalance = atm.getBalance("USER001", "SAVINGS");
        double withdrawAmount = 200.0;
        
        atm.processCardlessTransaction("withdrawal", "SAVINGS", withdrawAmount, "123456");
        
        assertEquals("Balance should decrease after cardless withdrawal",
            initialBalance - withdrawAmount, 
            atm.getBalance("USER001", "SAVINGS"),
            0.01);
    }

    @Test
    public void testTransferBetweenAccounts() {
        String userId = "USER001";
        double savingsInitial = atm.getBalance(userId, "SAVINGS");
        double checkingInitial = atm.getBalance(userId, "CHECKING");
        double transferAmount = 100.0;
        
        atm.performTransfer(userId, "SAVINGS", userId, "CHECKING", transferAmount);
        
        assertEquals("Savings should decrease by transfer amount",
            savingsInitial - transferAmount,
            atm.getBalance(userId, "SAVINGS"),
            0.01);
        assertEquals("Checking should increase by transfer amount",
            checkingInitial + transferAmount,
            atm.getBalance(userId, "CHECKING"),
            0.01);
    }

    @Test
    public void testTransferToAnotherUser() {
        String sourceUserId = "USER001";
        String destUserId = "USER002";
        double sourceInitial = atm.getBalance(sourceUserId, "SAVINGS");
        double destInitial = atm.getBalance(destUserId, "SAVINGS");
        double transferAmount = 100.0;
        
        atm.performTransfer(sourceUserId, "SAVINGS", destUserId, "SAVINGS", transferAmount);
        
        assertEquals("Source account should decrease by transfer amount",
            sourceInitial - transferAmount,
            atm.getBalance(sourceUserId, "SAVINGS"),
            0.01);
        assertEquals("Destination account should increase by transfer amount",
            destInitial + transferAmount,
            atm.getBalance(destUserId, "SAVINGS"),
            0.01);
    }

    @Test
    public void testInsufficientFundsTransfer() {
        String userId = "USER001";
        double initialBalance = atm.getBalance(userId, "SAVINGS");
        double transferAmount = initialBalance + 1000.0; // More than available
        
        atm.performTransfer(userId, "SAVINGS", "USER002", "SAVINGS", transferAmount);
        
        assertEquals("Balance should remain unchanged on insufficient funds",
            initialBalance,
            atm.getBalance(userId, "SAVINGS"),
            0.01);
    }

    @Test
    public void testPINChange() {
        assertTrue("Should successfully change PIN",
            atm.changePIN("1234", "5678")); // 1234 is default PIN
        assertTrue("Should authenticate with new PIN",
            atm.authenticateWithPIN("5678"));
    }

    @Test
    public void testTransactionHistory() {
        atm.processCardlessTransaction("deposit", "SAVINGS", 100.0, "123456");
        atm.processCardlessTransaction("withdrawal", "CHECKING", 50.0, "654321");
        
        assertTrue("Transaction history should contain at least 2 entries",
            atm.getTransactionHistory().size() >= 2);
    }

    @Test
    public void testMiniStatement() {
        // Perform 6 transactions
        for (int i = 1; i <= 6; i++) {
            atm.processCardlessTransaction("deposit", "SAVINGS", i * 100.0, "123456");
        }
        
        assertEquals("Mini statement should show only last 5 transactions",
            5,
            atm.getMiniStatement().size());
    }

    @Test
    public void testPINActivityLogging() {
        atm.authenticateWithPIN("1234"); // Correct PIN
        atm.authenticateWithPIN("9999"); // Wrong PIN
        atm.changePIN("1234", "5678"); // Change PIN
        
        assertTrue("PIN activity log should contain at least 3 entries",
            atm.getPINActivityHistory().size() >= 3);
    }
}