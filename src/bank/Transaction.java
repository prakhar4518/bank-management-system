package bank;

import java.text.SimpleDateFormat;
import java.util.Date;

/** One entry in an account's history. */
public class Transaction {
    public final String time;
    public final String type;      // DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT
    public final double amount;
    public final double balanceAfter;
    public final String note;

    public Transaction(String type, double amount, double balanceAfter, String note) {
        this(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                type, amount, balanceAfter, note);
    }

    public Transaction(String time, String type, double amount, double balanceAfter, String note) {
        this.time = time;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.note = note;
    }
}
