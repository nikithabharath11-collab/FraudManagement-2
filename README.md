# Fraud Management System – "Bank Guard" (DBMS project)
**B NIKITHA (Nikitha Bharath)** · Reg. No. **25BCE2517** · B.Tech CSE, 3rd semester · VIT Vellore

One MySQL database (`fraud_management`: 8 tables, 2 views, a trigger, a stored procedure, indexes) with three
front ends that I built one after another. Each folder is complete on its own.

| Folder | What it is | Main files |
|---|---|---|
| [`1-bank-guard-html`](1-bank-guard-html) | **Part 1** – HTML dashboard + small Java server (REST over JDBC) | `GuardServer.java`, `GuardConsole.java`, `dashboard/index.html`, `database/guard_schema.sql` |
| [`2-bank-guard-javafx`](2-bank-guard-javafx) | **Part 2** – desktop app in JavaFX + JDBC (the version I hand in) | `GuardApp.java`, `GuardDb.java`, `run.bat` |
| [`3-bank-guard-web`](3-bank-guard-web) | **Part 2 live website** – the same screens in pure Java with Vaadin Flow | `src/main/java/guard/GuardView.java`, `GuardDb.java`, `run_web.bat` |

**Live website:** https://nikithabharath11-collab.github.io/fraud-management/ (opens https://fraudmgmt.onrender.com/web/fraud)  
**Part 2 zip:** https://fraudmgmt.onrender.com/fx/downloads/FraudManagement-2.zip

## Before any of them (Windows 11)
1. Install **MySQL 8.0** and make sure `mysql` works in Command Prompt.
2. Download **MySQL Connector/J** (platform independent). Every folder says where its `.jar` goes.
3. Load the database once: `mysql -u root -p < 2-bank-guard-javafx\database\guard_schema.sql`
   (all three folders use the same schema; it also resets the sample data).

No password is stored in this repository. Each program reads it from the `FRAUD_DB_PASS` environment
variable, and the `.bat` files ask for it when they start.

## 1 – HTML version (part 1)
Needs a JDK. Put the connector jar in `1-bank-guard-html\jdbc_driver`, then in that folder:
```
set FRAUD_DB_PASS=your-mysql-password
javac -cp ".;jdbc_driver\*" GuardConsole.java GuardServer.java
java  -cp ".;jdbc_driver\*" GuardServer
```
Open http://localhost:8080 – the dashboard calls `/api/...` on GuardServer, which runs the SQL.
`java -cp ".;jdbc_driver\*" GuardConsole` runs the console JDBC demo. Details: `1-bank-guard-html/README.md`.

## 2 – JavaFX desktop version (part 2)
Needs JDK 21 and the JavaFX 21 SDK in `C:\javafx-sdk-21.0.6` (or change `JAVAFX_LIB` in `run.bat`).
Put the connector jar in `2-bank-guard-javafx\lib`, then double-click `2-bank-guard-javafx\run.bat`.
It asks for the MySQL password, can load the schema, compiles and opens the Bank Guard window.
Full report: `2-bank-guard-javafx/REPORT.md`.

## 3 – Web version in Vaadin (part 2 live link)
Needs JDK 21 and Maven 3.9 (the first build downloads Vaadin, Jetty and the MySQL driver).
Double-click `3-bank-guard-web\run_web.bat`, then open http://localhost:8080/fraud.
There is no HTML or CSS of my own: `GuardView.java` builds the page from Vaadin components and
`GuardDb.java` is the JavaFX version's JDBC class with only a `package` line added.

---
I used AI assistance for parts of the code and deployment, as allowed for this project.
