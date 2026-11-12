package bank;

import java.util.ArrayList;
import java.util.List;

/** A bank account held in memory; persisted by {@link FileDatabase}. */
public class Account {
    public final long accNo;
    public final String name;
    public final String phone;
    public final String type;      // Savings or Current
    public final String created;
    String pinHash;
    double balance;
    final List<Transaction> history = new ArrayList<>();

    public Account(long accNo, String name, String phone, String type,
                   String pinHash, double balance, String created) {
        this.accNo = accNo;
        this.name = name;
        this.phone = phone;
        this.type = type;
        this.pinHash = pinHash;
        this.balance = balance;
        this.created = created;
    }

    public double getBalance() { return balance; }

    public List<Transaction> getHistory() { return history; }

    /** Adds money and records the transaction. */
    Transaction credit(double amount, String type, String note) {
        balance = Math.round((balance + amount) * 100) / 100.0;
        Transaction t = new Transaction(type, amount, balance, note);
        history.add(t);
        return t;
    }

    /** Removes money and records the transaction; returns null if funds are insufficient. */
    Transaction debit(double amount, String type, String note) {
        if (amount > balance) return null;
        balance = Math.round((balance - amount) * 100) / 100.0;
        Transaction t = new Transaction(type, amount, balance, note);
        history.add(t);
        return t;
    }
}
