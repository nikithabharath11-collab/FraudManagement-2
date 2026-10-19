# DBMS PROJECT – PART 2 (JavaFX)
## FRAUD MANAGEMENT SYSTEM – "Bank Guard" desktop app
**Name:** B NIKITHA (Nikitha Bharath)  **Reg. No.:** 25BCE2517  **Course:** B.Tech CSE, 3rd semester, VIT Vellore
**Database:** `fraud_management` (8 tables + 2 views + 1 trigger + 1 stored procedure + indexes, MySQL 8)

**Live website (web version of the same app, pure Java with Vaadin):** https://nikithabharath11-collab.github.io/fraud-management/  
**Download this project (zip):** https://fraudmgmt.onrender.com/fx/downloads/FraudManagement-2.zip  
**GitHub repository:** https://github.com/nikithabharath11-collab/FraudManagement-2

---

## 1. What changed from part 1
Part 1 had a Java backend (`GuardServer.java`) and an HTML/JavaScript page (`dashboard/index.html`).
In part 2 the whole user interface is written **in Java with JavaFX**: no HTML, no CSS files, no web server.
The window talks to MySQL directly over **JDBC**.

| Part 1 (web)                        | Part 2 (JavaFX)                                       |
|-------------------------------------|-------------------------------------------------------|
| `dashboard/index.html` (HTML + JS)  | `GuardApp.java` – one window, `TabPane` with 12 tabs  |
| `GuardServer.java` (HTTP + JDBC)    | `GuardDb.java` – the same queries and rule logic      |
| HTML tables                         | `TableView` filled from the `ResultSet`               |
| HTML forms                          | `ComboBox` / `TextField` / `Button` forms             |

The SQL file `database/guard_schema.sql` is the same as in part 1.

## 2. Requirements (Windows 11)
1. **MySQL 8.0** (MySQL Server + `mysql` command in PATH, or MySQL Workbench)
2. **JDK 17 or newer** (e.g. JDK 21 or JDK 25) – check with `java -version` and `javac -version`
3. **JavaFX SDK** for Windows, x64 (e.g. 21.0.6 LTS) from https://gluonhq.com/products/javafx/ –
   unzip it to `C:\javafx-sdk-21.0.6` so that `C:\javafx-sdk-21.0.6\lib\javafx.controls.jar` exists
4. **MySQL Connector/J** jar (e.g. `mysql-connector-j-9.1.0.jar`) copied into the `lib` folder
5. VS Code (optional, for editing)

## 3. Project structure
```
FraudManagement-2
├── GuardApp.java              ← JavaFX window: header, 12 tabs, forms, footer with my name
├── GuardDb.java               ← all JDBC code: queries, new transaction + rule checks, reviews, customers
├── run.bat                    ← compiles and starts the app on Windows
├── database
│   └── guard_schema.sql       ← tables, sample data, views, trigger, procedure, indexes
├── lib
│   └── PUT_JAR_HERE.txt       ← put mysql-connector-j-x.x.x.jar here
├── web                        ← web version for the live link (Vaadin, pure Java)
│   ├── pom.xml                ← Maven build (Vaadin, embedded Jetty, MySQL driver)
│   ├── run_web.bat            ← builds and starts it on http://localhost:8080
│   └── src/main/java/guard/     ← GuardView.java (screens), GuardDb.java (same JDBC code), Main.java
├── screenshots                ← one picture of every tab
└── REPORT.md
```

## 4. How to run (Windows 11)
**Step 1 – create the database** (only once):
```
mysql -u root -p < database\guard_schema.sql
```
Check it: `USE fraud_management; SHOW TABLES; SELECT * FROM customers;`

**Step 2 – start the app:** double-click **`run.bat`**.
It asks for the MySQL root password (and can load the schema for you), compiles the two Java files and opens the window.
If the JavaFX SDK is in another folder, edit the line `set "JAVAFX_LIB=C:\javafx-sdk-21.0.6\lib"` in `run.bat`.

**Same thing by hand** (Command Prompt in the project folder):
```
set FRAUD_DB_PASS=your_mysql_password
javac -d out --module-path "C:\javafx-sdk-21.0.6\lib" --add-modules javafx.controls GuardDb.java GuardApp.java
java --module-path "C:\javafx-sdk-21.0.6\lib" --add-modules javafx.controls -cp "out;lib\*" GuardApp
```
Optional settings (environment variables): `FRAUD_DB_URL` (default `jdbc:mysql://localhost:3306/fraud_management`),
`FRAUD_DB_USER` (default `root`), `FRAUD_DB_PASS` (default empty).

## 5. The window
* **Header:** title and a badge that turns green ("JDBC connected · MySQL") or red ("Database unreachable"), plus a Refresh button.
* **Tabs** (same as the web version): Dashboard, Transactions, Fraud Alerts, Customers, Accounts, Devices, Merchants,
  Rules, Activity Log, Risk Summary, Risk Scores, Monthly Statements. Every time a tab is opened its data is read again from MySQL.
* **Footer:** `B NIKITHA | Reg. No. 25BCE2517 | VIT Vellore | B.Tech CSE | DBMS Project Part 2 (JavaFX)`; the window title also has my name and reg. no.
* Status columns (severity, alert status, standing, risk level ...) are shown as small red / amber / green tags.

| Tab | What it shows (SQL) | Actions |
|-----|---------------------|---------|
| Dashboard | 5 counts with sub-queries (`COUNT(*)`), all fraud alerts (JOIN of 3 tables) | – |
| Transactions | `payment_transactions` | **New transaction** – rules run automatically |
| Fraud Alerts | alerts JOIN rules LEFT JOIN customers | **Review an alert** (Cleared / Confirmed Fraud) |
| Customers | `customers` | **Add**, **update standing**, **delete** customer |
| Accounts, Devices, Merchants, Rules, Activity Log | the tables | – |
| Risk Summary | view `customer_risk_summary` | – |
| Risk Scores | view `transaction_risk_scores` | – |
| Monthly Statements | stored procedure `CALL monthly_statement(NULL)` | – |

## 6. Business logic (GuardDb.java)
**New transaction** (`checkTransaction`) – everything inside one database transaction (`setAutoCommit(false)` → `commit()` / `rollback()`):
1. The account must be Active; device and merchant must exist.
2. Every **enabled** rule from `detection_rules` is checked:
   * RULE-6001 Large Withdrawal Watch – withdrawal above INR 50,000 → High
   * RULE-6002 New Device Check – untrusted device → High
   * RULE-6003 Odd Hours Activity – between 23:00 and 05:00 (the "Simulate hour" box lets you test this) → Low
   * RULE-6004 High Risk Merchant – merchant risk = High → Medium (disabled in the sample data)
3. No rule hit → balance is reduced (`UPDATE ... WHERE balance >= amount`, so it cannot go negative) and the status is *Completed*.
   Any hit → status *Under Review*, flagged = Yes, one row in `fraud_alerts` per rule.
4. A row is written to `activity_log`. The trigger `trg_balance_history` records the balance change.

**Review an alert** (`reviewAlert`) – only an *Administrator* may review. *Confirmed Fraud* blocks the transaction;
*Cleared* completes it once no other alert on it is still open.

**Customers** – add (next id `CUST-10xx` is worked out with `MAX(...)+1`), change standing, delete
(MySQL refuses with a foreign key error if the customer still has accounts, devices or log rows – the message is shown on screen).

All statements that use user input are `PreparedStatement`s with `?` parameters.

## 7. DBMS concepts used
Primary / foreign keys, JOIN and LEFT JOIN, sub-queries, aggregate functions, 2 views, 1 trigger,
1 stored procedure called from Java, indexes, transactions with commit / rollback, prepared statements.

## 8. Screenshots
`screenshots/01_dashboard.png` … `12_monthly_statements.png` – one picture per tab (the footer with my name is visible on every one).

## 9. Live website (web version, folder `web/`)
The JavaFX desktop app above is the main project. For the live link I wrote a web version of the same screens in Java
with **Vaadin Flow** (free, Apache 2.0): `web/src/main/java/guard/GuardView.java` builds the same tabs, forms, tables,
colour tags and name footer with Vaadin components instead of JavaFX controls, and `GuardDb.java` is the same JDBC class
as the desktop app (only a `package` line added). There are no HTML or CSS files of my own; Vaadin turns the Java
components into the page and sends every click back to the Java code on the server.

It runs on embedded Jetty at https://fraudmgmt.onrender.com/web/fraud (https://nikithabharath11-collab.github.io/fraud-management/ opens it) and uses its own copy of
the same schema, so visitors can try the forms freely. The free server sleeps when idle, so the first visit can take
up to a minute. To run it on your own PC: install JDK 21 and Maven 3.9, load the schema, then run `web\run_web.bat`
and open http://localhost:8080/fraud.

---
I used AI assistance for parts of the code and deployment, as allowed for this project.
