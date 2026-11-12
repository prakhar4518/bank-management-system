package bank;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/** Business logic. Every change is written to the file database immediately. */
public class Bank {
    private final FileDatabase db;
    private final Map<Long, Account> accounts;

    public Bank(FileDatabase db) throws IOException {
        this.db = db;
        this.accounts = db.loadAll();
    }

    public Collection<Account> all() { return accounts.values(); }

    public Account get(long accNo) { return accounts.get(accNo); }

    public Account create(String name, String phone, String type, String pin, double opening) {
        Random r = new Random();
        long no;
        do { no = 10000000L + r.nextInt(90000000); } while (accounts.containsKey(no));

        String created = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        Account a = new Account(no, FileDatabase.clean(name), FileDatabase.clean(phone), type,
                hash(pin), 0, created);
        accounts.put(no, a);
        Transaction t = opening > 0 ? a.credit(opening, "DEPOSIT", "Opening deposit") : null;
        persist(a, t);
        return a;
    }

    public Account authenticate(long accNo, String pin) {
        Account a = accounts.get(accNo);
        return (a != null && a.pinHash.equals(hash(pin))) ? a : null;
    }

    public void deposit(Account a, double amount) {
        persist(a, a.credit(amount, "DEPOSIT", ""));
    }

    public boolean withdraw(Account a, double amount) {
        Transaction t = a.debit(amount, "WITHDRAW", "");
        if (t == null) return false;
        persist(a, t);
        return true;
    }

    public boolean transfer(Account from, long toNo, double amount) {
        Account to = accounts.get(toNo);
        if (to == null || to == from) return false;
        Transaction out = from.debit(amount, "TRANSFER_OUT", "To " + toNo);
        if (out == null) return false;
        Transaction in = to.credit(amount, "TRANSFER_IN", "From " + from.accNo);
        try {
            db.saveAccounts(accounts.values());      // both balances in one write
            db.appendTransaction(from.accNo, out);
            db.appendTransaction(to.accNo, in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return true;
    }

    public boolean changePin(Account a, String oldPin, String newPin) {
        if (!a.pinHash.equals(hash(oldPin))) return false;
        a.pinHash = hash(newPin);
        persist(a, null);
        return true;
    }

    public boolean delete(long accNo) {
        if (accounts.remove(accNo) == null) return false;
        try {
            db.saveAccounts(accounts.values());
            db.saveTransactions(accounts.values());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return true;
    }

    public List<Account> search(String query) {
        String q = query.toLowerCase();
        List<Account> out = new ArrayList<>();
        for (Account a : accounts.values()) {
            if (a.name.toLowerCase().contains(q) || String.valueOf(a.accNo).equals(q)) out.add(a);
        }
        return out;
    }

    private void persist(Account a, Transaction t) {
        try {
            db.saveAccounts(accounts.values());
            if (t != null) db.appendTransaction(a.accNo, t);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String hash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
