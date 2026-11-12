package bank;

import java.io.Console;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Scanner;

/** Console user interface. */
public class Main {
    static final String ADMIN_USER = "admin";
    static final String ADMIN_PASS = "admin123";
    static final String DATA_FOLDER = "data";
    static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        Bank bank;
        try {
            bank = new Bank(new FileDatabase(DATA_FOLDER));
        } catch (IOException e) {
            System.out.println("Cannot open data folder: " + e.getMessage());
            return;
        }
        while (true) {
            System.out.println("\n===== BANK MANAGEMENT SYSTEM =====");
            System.out.println("1. Open account\n2. Customer login\n3. Admin login\n4. Exit");
            try {
                if (!in.hasNextLine()) return;
                switch (prompt("Choose: ")) {
                    case "1": openAccount(bank); break;
                    case "2": customerLogin(bank); break;
                    case "3": adminLogin(bank); break;
                    case "4": System.out.println("Goodbye!"); return;
                    default: System.out.println("Invalid choice.");
                }
            } catch (UncheckedIOException e) {
                System.out.println("File error: " + e.getMessage());
            }
        }
    }

    // ---------- Input helpers ----------
    static String prompt(String msg) {
        System.out.print(msg);
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    static String secret(String msg) {
        Console c = System.console();
        if (c != null) return new String(c.readPassword(msg));
        return prompt(msg); // IDE fallback: input is visible
    }

    static double readAmount(String msg) {
        try {
            double v = Double.parseDouble(prompt(msg));
            if (v > 0) return Math.round(v * 100) / 100.0;
        } catch (NumberFormatException ignored) { }
        System.out.println("Invalid amount.");
        return -1;
    }

    static long readLong(String msg) {
        try { return Long.parseLong(prompt(msg)); }
        catch (NumberFormatException e) { System.out.println("Invalid number."); return -1; }
    }

    static String readPin(String msg) {
        String p = secret(msg);
        if (p.matches("\\d{4}")) return p;
        System.out.println("PIN must be exactly 4 digits.");
        return null;
    }

    // ---------- Open account ----------
    static void openAccount(Bank bank) {
        String name = prompt("Full name: ");
        if (name.isEmpty()) { System.out.println("Name is required."); return; }
        String phone = prompt("Phone: ");
        String type = prompt("Type (1=Savings, 2=Current): ").equals("1") ? "Savings" : "Current";
        String pin = readPin("Set 4-digit PIN: ");
        if (pin == null) return;
        double opening = 0;
        if (prompt("Opening deposit? (y/n): ").equalsIgnoreCase("y")) {
            opening = Math.max(0, readAmount("Amount: "));
        }
        Account a = bank.create(name, phone, type, pin, opening);
        System.out.println("\nAccount created! Account number: " + a.accNo);
    }

    // ---------- Customer ----------
    static void customerLogin(Bank bank) {
        long no = readLong("Account number: ");
        String pin = readPin("PIN: ");
        Account a = (no < 0 || pin == null) ? null : bank.authenticate(no, pin);
        if (a == null) { System.out.println("Wrong account number or PIN."); return; }
        System.out.println("\nWelcome, " + a.name + "!");

        while (true) {
            System.out.println("\n--- Customer Menu ---");
            System.out.println("1.Balance 2.Deposit 3.Withdraw 4.Transfer");
            System.out.println("5.Statement 6.Change PIN 7.Close account 8.Logout");
            try {
                switch (prompt("Choose: ")) {
                    case "1":
                        System.out.printf("Current balance: %.2f%n", a.getBalance());
                        break;
                    case "2": {
                        double amt = readAmount("Deposit amount: ");
                        if (amt > 0) {
                            bank.deposit(a, amt);
                            System.out.printf("Deposited %.2f. New balance: %.2f%n", amt, a.getBalance());
                        }
                        break;
                    }
                    case "3": {
                        double amt = readAmount("Withdraw amount: ");
                        if (amt > 0) {
                            if (bank.withdraw(a, amt))
                                System.out.printf("Withdrew %.2f. New balance: %.2f%n", amt, a.getBalance());
                            else System.out.println("Insufficient funds.");
                        }
                        break;
                    }
                    case "4": {
                        long to = readLong("Recipient account number: ");
                        double amt = readAmount("Transfer amount: ");
                        if (to > 0 && amt > 0) {
                            if (bank.transfer(a, to, amt))
                                System.out.printf("Transferred %.2f to %d. New balance: %.2f%n", amt, to, a.getBalance());
                            else System.out.println("Transfer failed (check account number and funds).");
                        }
                        break;
                    }
                    case "5": printStatement(a, 10); break;
                    case "6": {
                        String old = readPin("Current PIN: ");
                        String np = old == null ? null : readPin("New 4-digit PIN: ");
                        if (old != null && np != null) {
                            System.out.println(bank.changePin(a, old, np) ? "PIN changed." : "Incorrect PIN.");
                        }
                        break;
                    }
                    case "7":
                        if (prompt("Type YES to permanently close your account: ").equals("YES")) {
                            bank.delete(a.accNo);
                            System.out.println("Account closed.");
                            return;
                        }
                        break;
                    case "8": return;
                    default: System.out.println("Invalid choice.");
                }
            } catch (UncheckedIOException e) {
                System.out.println("File error: " + e.getMessage());
            }
        }
    }

    static void printStatement(Account a, int limit) {
        List<Transaction> h = a.getHistory();
        if (h.isEmpty()) { System.out.println("No transactions yet."); return; }
        System.out.printf("%n%-20s%-14s%10s%12s  %s%n", "Date", "Type", "Amount", "Balance", "Note");
        System.out.println("----------------------------------------------------------------------");
        for (int i = h.size() - 1; i >= Math.max(0, h.size() - limit); i--) {
            Transaction t = h.get(i);
            System.out.printf("%-20s%-14s%10.2f%12.2f  %s%n", t.time, t.type, t.amount, t.balanceAfter, t.note);
        }
    }

    // ---------- Admin ----------
    static void adminLogin(Bank bank) {
        if (!(prompt("Username: ").equals(ADMIN_USER) && secret("Password: ").equals(ADMIN_PASS))) {
            System.out.println("Invalid admin credentials.");
            return;
        }
        while (true) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1.List accounts 2.Search 3.View transactions 4.Delete account 5.Logout");
            try {
                switch (prompt("Choose: ")) {
                    case "1": {
                        System.out.printf("%n%-12s%-22s%-10s%12s%n", "Acc No", "Name", "Type", "Balance");
                        double total = 0;
                        for (Account a : bank.all()) {
                            System.out.printf("%-12d%-22s%-10s%12.2f%n", a.accNo, a.name, a.type, a.getBalance());
                            total += a.getBalance();
                        }
                        System.out.printf("Total accounts: %d | Total deposits: %.2f%n", bank.all().size(), total);
                        break;
                    }
                    case "2": {
                        List<Account> found = bank.search(prompt("Name or account number: "));
                        for (Account a : found) {
                            System.out.printf("%d | %s | %s | %s | %.2f | opened %s%n",
                                    a.accNo, a.name, a.phone, a.type, a.getBalance(), a.created);
                        }
                        if (found.isEmpty()) System.out.println("No match.");
                        break;
                    }
                    case "3": {
                        Account a = bank.get(readLong("Account number: "));
                        if (a == null) System.out.println("Account not found.");
                        else printStatement(a, 50);
                        break;
                    }
                    case "4": {
                        long no = readLong("Account number to delete: ");
                        System.out.println(bank.delete(no) ? "Deleted." : "Account not found.");
                        break;
                    }
                    case "5": return;
                    default: System.out.println("Invalid choice.");
                }
            } catch (UncheckedIOException e) {
                System.out.println("File error: " + e.getMessage());
            }
        }
    }
}
