# Bank Management System (Java)

A console-based bank management system in plain Java (Java 8+).
It uses the **file system as its database**: all data is stored in human-readable
text files inside a `data/` folder. No database server and no external libraries.

## Features

**Customer**
- Open a Savings or Current account with a 4-digit PIN (stored as a SHA-256 hash)
- Check balance, deposit, withdraw, transfer to another account
- Mini statement (last 10 transactions)
- Change PIN, close account

**Admin**
- List all accounts and total deposits
- Search accounts by name or account number
- View any account's transaction history
- Delete an account

## Project structure

```
bank-management-system/
├── src/bank/
│   ├── Main.java            # console menus (UI)
│   ├── Bank.java            # business logic
│   ├── Account.java         # account model
│   ├── Transaction.java     # transaction model
│   └── FileDatabase.java    # reads/writes the text files
├── data/                    # created automatically at first run (git-ignored)
│   ├── accounts.txt
│   └── transactions.txt
├── .github/workflows/build.yml
├── run.sh / run.bat
├── .gitignore
├── .gitattributes
├── LICENSE
└── README.md
```

## How the file database works

Two pipe-separated (`|`) text files, created automatically in `data/`:

`accounts.txt`
```
# accNo|name|phone|type|pinHash|balance|created
87850660|Alice Smith|9990001111|Savings|03ac6742...|350.00|2026-11-12 03:52
```

`transactions.txt`
```
# accNo|time|type|amount|balanceAfter|note
87850660|2026-11-12 03:52:21|TRANSFER_OUT|200.00|350.00|To 86472276
```

- Everything is loaded into memory at startup and saved after every change.
- `accounts.txt` is rewritten through a temporary file and then moved into place, so a crash
  mid-write cannot leave a half-written file.
- `transactions.txt` is append-only (rewritten only when an account is deleted).
- The `|` character and line breaks are stripped from names and phone numbers so they cannot corrupt the format.
- Malformed lines are skipped with a warning instead of crashing the program.
- To reset the system, delete the `data/` folder.

## Requirements

- JDK 8 or newer (`javac -version` to check)

## Build and run

Linux / macOS:
```bash
./run.sh
```

Windows:
```bat
run.bat
```

Or manually:
```bash
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out bank.Main
```

## Default admin login

| Username | Password   |
|----------|------------|
| `admin`  | `admin123` |

Change `ADMIN_USER` and `ADMIN_PASS` at the top of `src/bank/Main.java` before real use.

## Notes

- `data/` holds all account data and is git-ignored. Do not commit it.
- In an IDE console, PIN and password input is visible because hidden input is not supported there. Use a real terminal for hidden input.
- Learning project, not production banking software: hard-coded admin credentials, no file locking (run only one instance at a time), no concurrency control.

## Possible improvements

- Switch to SQLite/MySQL through JDBC
- Swing or JavaFX GUI
- Interest calculation, loans, account lockout after failed logins
- JUnit tests

## License

MIT, see [LICENSE](LICENSE).
