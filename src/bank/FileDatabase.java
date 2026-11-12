package bank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Uses plain text files as the database.
 *
 *   data/accounts.txt      accNo|name|phone|type|pinHash|balance|created
 *   data/transactions.txt  accNo|time|type|amount|balanceAfter|note
 *
 * Fields are separated by '|'. Lines starting with '#' are comments.
 * Accounts are rewritten safely (temp file, then move); transactions are appended.
 */
public class FileDatabase {
    private static final String ACC_HEADER = "# accNo|name|phone|type|pinHash|balance|created";
    private static final String TX_HEADER = "# accNo|time|type|amount|balanceAfter|note";

    private final Path accountsFile;
    private final Path transactionsFile;

    public FileDatabase(String folder) throws IOException {
        Path dir = Paths.get(folder);
        Files.createDirectories(dir);
        accountsFile = dir.resolve("accounts.txt");
        transactionsFile = dir.resolve("transactions.txt");
        if (!Files.exists(accountsFile)) writeAtomically(accountsFile, Collections.singletonList(ACC_HEADER));
        if (!Files.exists(transactionsFile)) writeAtomically(transactionsFile, Collections.singletonList(TX_HEADER));
    }

    /** Removes characters that would break the file format. */
    public static String clean(String s) {
        return s.replace("|", " ").replace("\r", " ").replace("\n", " ").trim();
    }

    // ---------- Load ----------
    public Map<Long, Account> loadAll() throws IOException {
        Map<Long, Account> accounts = new LinkedHashMap<>();

        for (String line : Files.readAllLines(accountsFile, StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\\|", -1);
            try {
                if (f.length != 7) throw new IllegalArgumentException("expected 7 fields");
                long no = Long.parseLong(f[0]);
                accounts.put(no, new Account(no, f[1], f[2], f[3], f[4], Double.parseDouble(f[5]), f[6]));
            } catch (RuntimeException e) {
                System.out.println("Skipping bad account line: " + line);
            }
        }

        for (String line : Files.readAllLines(transactionsFile, StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\\|", -1);
            try {
                if (f.length != 6) throw new IllegalArgumentException("expected 6 fields");
                Account a = accounts.get(Long.parseLong(f[0]));
                if (a != null) {
                    a.history.add(new Transaction(f[1], f[2], Double.parseDouble(f[3]),
                            Double.parseDouble(f[4]), f[5]));
                }
            } catch (RuntimeException e) {
                System.out.println("Skipping bad transaction line: " + line);
            }
        }
        return accounts;
    }

    // ---------- Save ----------
    /** Rewrites accounts.txt with the current state of all accounts. */
    public void saveAccounts(Collection<Account> accounts) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(ACC_HEADER);
        for (Account a : accounts) {
            lines.add(a.accNo + "|" + a.name + "|" + a.phone + "|" + a.type + "|" + a.pinHash + "|"
                    + String.format(Locale.ROOT, "%.2f", a.balance) + "|" + a.created);
        }
        writeAtomically(accountsFile, lines);
    }

    /** Adds one transaction to the end of transactions.txt. */
    public void appendTransaction(long accNo, Transaction t) throws IOException {
        Files.write(transactionsFile, Collections.singletonList(txLine(accNo, t)),
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    /** Rewrites transactions.txt from every account's history (used after deleting an account). */
    public void saveTransactions(Collection<Account> accounts) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(TX_HEADER);
        for (Account a : accounts) {
            for (Transaction t : a.history) lines.add(txLine(a.accNo, t));
        }
        writeAtomically(transactionsFile, lines);
    }

    private static String txLine(long accNo, Transaction t) {
        return accNo + "|" + t.time + "|" + t.type + "|"
                + String.format(Locale.ROOT, "%.2f", t.amount) + "|"
                + String.format(Locale.ROOT, "%.2f", t.balanceAfter) + "|" + t.note;
    }

    private static void writeAtomically(Path target, List<String> lines) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
