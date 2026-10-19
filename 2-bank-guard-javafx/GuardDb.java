/* Fraud Management System (Part 2, JavaFX) - B NIKITHA, 25BCE2517 */
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * All the JDBC work for the Bank Guard app: reading the tables / views / procedure
 * and the actions (new transaction with rule checks, alert review, customer changes).
 * The rule logic is the same as in GuardServer.java from part 1.
 */
public class GuardDb {
    static final String URL  = env("FRAUD_DB_URL", "jdbc:mysql://localhost:3306/fraud_management");
    static final String USER = env("FRAUD_DB_USER", "root");
    static final String PASS = env("FRAUD_DB_PASS", "");
    static String env(String k, String d) { String v = System.getenv(k); return v == null ? d : v; }

    /** Result of a SELECT: column names + rows of text. */
    public record Rows(List<String> columns, List<List<String>> data) {
        public String get(int row, String column) { return data.get(row).get(columns.indexOf(column)); }
        public int size() { return data.size(); }
    }

    static final String SUMMARY =
        "SELECT (SELECT COUNT(*) FROM customers) AS customers, (SELECT COUNT(*) FROM bank_accounts) AS accounts, "
      + "(SELECT COUNT(*) FROM payment_transactions) AS transactions, "
      + "(SELECT COUNT(*) FROM payment_transactions WHERE flagged='Yes') AS flagged_transactions, "
      + "(SELECT COUNT(*) FROM fraud_alerts WHERE alert_status='Open') AS open_alerts";

    // one query per tab (same queries as the part 1 web version)
    static final Map<String, String> TAB_QUERIES = new LinkedHashMap<>();
    static {
        TAB_QUERIES.put("customers",    "SELECT * FROM customers ORDER BY customer_number");
        TAB_QUERIES.put("accounts",     "SELECT * FROM bank_accounts ORDER BY account_number");
        TAB_QUERIES.put("devices",      "SELECT * FROM devices ORDER BY device_ref");
        TAB_QUERIES.put("merchants",    "SELECT * FROM merchants ORDER BY merchant_ref");
        TAB_QUERIES.put("rules",        "SELECT * FROM detection_rules ORDER BY rule_ref");
        TAB_QUERIES.put("transactions", "SELECT * FROM payment_transactions ORDER BY txn_datetime DESC");
        TAB_QUERIES.put("logs",         "SELECT * FROM activity_log ORDER BY event_time DESC");
        TAB_QUERIES.put("risk",         "SELECT * FROM customer_risk_summary ORDER BY open_alerts DESC, flagged_transactions DESC, customer_number"); // view
        TAB_QUERIES.put("riskscores",   "SELECT * FROM transaction_risk_scores ORDER BY risk_score DESC, txn_ref");  // view
        TAB_QUERIES.put("statements",   "CALL monthly_statement(NULL)");                                             // stored procedure
        TAB_QUERIES.put("alerts",       "SELECT a.alert_ref, a.txn_ref, r.rule_name, a.severity, a.alert_status, c.full_name AS reviewed_by, a.reviewer_notes "
                                      + "FROM fraud_alerts a JOIN detection_rules r ON a.rule_ref = r.rule_ref "
                                      + "LEFT JOIN customers c ON a.reviewed_by = c.customer_number ORDER BY a.alert_ref DESC");
    }

    static Connection connect() throws SQLException { return DriverManager.getConnection(URL, USER, PASS); }

    /** Run a SELECT (or CALL) and return every row as text. */
    static Rows query(String sql) throws SQLException {
        try (Connection c = connect(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            List<String> cols = new ArrayList<>();
            for (int i = 1; i <= md.getColumnCount(); i++) cols.add(md.getColumnLabel(i));
            List<List<String>> data = new ArrayList<>();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= cols.size(); i++) row.add(rs.getString(i));
                data.add(row);
            }
            return new Rows(cols, data);
        }
    }

    static Rows tab(String name) throws SQLException { return query(TAB_QUERIES.get(name)); }

    /**
     * Insert a transaction, run every enabled rule against it and raise alerts + an activity log row,
     * all inside one database transaction. Returns a short message for the screen.
     */
    static String checkTransaction(String acc, String dev, String mer, String kind,
                                   String amountText, String location, String hourText) throws Exception {
        BigDecimal amt;
        try { amt = new BigDecimal(amountText.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("Enter a valid amount"); }
        if (amt.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        String loc = (location == null || location.isBlank()) ? "Unknown" : location.trim();

        try (Connection c = connect()) {
            String owner   = scalar(c, "SELECT belongs_to FROM bank_accounts WHERE account_number=? AND status='Active'", acc);
            String trusted = scalar(c, "SELECT trusted FROM devices WHERE device_ref=?", dev);
            String risk    = scalar(c, "SELECT risk_level FROM merchants WHERE merchant_ref=?", mer);
            if (owner == null) throw new IllegalArgumentException("Account not found or not active");
            if (trusted == null || risk == null) throw new IllegalArgumentException("Unknown device or merchant");

            LocalDateTime dt = LocalDateTime.now();
            if (hourText != null && !hourText.isBlank()) {
                int hr = Integer.parseInt(hourText.trim());
                if (hr < 0 || hr > 23) throw new IllegalArgumentException("Hour must be 0-23");
                dt = dt.withHour(hr);
            }
            int h = dt.getHour();

            c.setAutoCommit(false);
            try {
                List<String> rules = new ArrayList<>();
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT rule_ref FROM detection_rules WHERE enabled='Yes'")) {
                    while (rs.next()) rules.add(rs.getString(1));
                }
                // which enabled rules does this transaction break? -> {rule, severity}
                List<String[]> ruleHits = new ArrayList<>();
                for (String r : rules) {
                    String sev = switch (r) {
                        case "RULE-6001" -> kind.equals("Withdrawal") && amt.compareTo(BigDecimal.valueOf(50000)) > 0 ? "High" : null;
                        case "RULE-6002" -> trusted.equals("No") ? "High" : null;
                        case "RULE-6003" -> (h >= 23 || h < 5) ? "Low" : null;
                        case "RULE-6004" -> risk.equals("High") ? "Medium" : null;
                        default -> null;
                    };
                    if (sev != null) ruleHits.add(new String[]{r, sev});
                }
                boolean clean = ruleHits.isEmpty();
                // only clean transactions take money out straight away
                if (clean && update(c, "UPDATE bank_accounts SET balance = balance - ? WHERE account_number=? AND balance >= ?", amt, acc, amt) == 0)
                    throw new IllegalArgumentException("Insufficient balance");

                String ref = nextId(c, "payment_transactions", "txn_ref", "TXN-", 3000);
                update(c, "INSERT INTO payment_transactions (txn_ref, from_account, device_ref, merchant_ref, kind, amount, currency, txn_datetime, location, status, flagged) "
                        + "VALUES (?,?,?,?,?,?,'INR',?,?,?,?)",
                       ref, acc, dev, mer, kind, amt, Timestamp.valueOf(dt), loc, clean ? "Completed" : "Under Review", clean ? "No" : "Yes");
                for (String[] hit : ruleHits)
                    update(c, "INSERT INTO fraud_alerts (alert_ref, txn_ref, rule_ref, severity, alert_status) VALUES (?,?,?,?,'Open')",
                           nextId(c, "fraud_alerts", "alert_ref", "ALRT-", 7000), ref, hit[0], hit[1]);
                String event = switch (kind) { case "Purchase" -> "Payment Made"; case "Transfer" -> "Transfer Sent"; default -> "Withdrawal Attempt"; };
                update(c, "INSERT INTO activity_log (log_ref, event_type, performed_by, account_number, txn_ref, event_time, details) VALUES (?,?,?,?,?,?,?)",
                       nextId(c, "activity_log", "log_ref", "LOG-", 8000), event, owner, acc, ref, Timestamp.valueOf(dt),
                       kind + " of INR " + amt + " to " + mer + " via " + dev);
                c.commit();
                return ref + ": " + (clean ? "Completed" : "Under Review") + ", " + ruleHits.size() + " alert(s) raised";
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }

    /** An administrator clears an alert or confirms it as fraud (confirmed fraud blocks the transaction). */
    static void reviewAlert(String alert, String status, String reviewer, String notes) throws SQLException {
        if (notes == null || notes.isBlank()) throw new SQLException("Notes are required");
        try (Connection c = connect()) {
            if (!"Administrator".equals(scalar(c, "SELECT access_level FROM customers WHERE customer_number=?", reviewer)))
                throw new SQLException("Reviewer must be an Administrator");
            String txn = scalar(c, "SELECT txn_ref FROM fraud_alerts WHERE alert_ref=?", alert);
            if (txn == null) throw new SQLException("Unknown alert");
            update(c, "UPDATE fraud_alerts SET alert_status=?, reviewed_by=?, reviewer_notes=? WHERE alert_ref=?", status, reviewer, notes, alert);
            if ("Confirmed Fraud".equals(status))
                update(c, "UPDATE payment_transactions SET status='Blocked' WHERE txn_ref=?", txn);
            else   // cleared: the transaction completes once no open alert is left on it
                update(c, "UPDATE payment_transactions SET status='Completed' WHERE txn_ref=? AND status='Under Review' "
                        + "AND NOT EXISTS (SELECT 1 FROM fraud_alerts WHERE txn_ref=? AND alert_status='Open')", txn, txn);
        }
    }

    static String addCustomer(String name, String email, String level) throws SQLException {
        if (name.isBlank() || !email.contains("@")) throw new SQLException("Enter a name and a valid email");
        try (Connection c = connect()) {
            String id = nextId(c, "customers", "customer_number", "CUST-", 1000);
            update(c, "INSERT INTO customers (customer_number, full_name, email, access_level, joined_on, standing) VALUES (?,?,?,?,CURDATE(),'Active')",
                   id, name.trim(), email.trim(), level);
            return id;
        }
    }

    static void setStanding(String customer, String standing) throws SQLException {
        try (Connection c = connect()) {
            update(c, "UPDATE customers SET standing=? WHERE customer_number=?", standing, customer);
        }
    }

    /** Fails with a foreign key error if the customer still owns accounts or devices. */
    static void deleteCustomer(String customer) throws SQLException {
        try (Connection c = connect()) {
            update(c, "DELETE FROM customers WHERE customer_number=?", customer);
        }
    }

    // ---------- helpers ----------
    static String nextId(Connection c, String table, String col, String prefix, int base) throws SQLException {
        return prefix + scalar(c, "SELECT COALESCE(MAX(CAST(SUBSTRING(" + col + "," + (prefix.length() + 1) + ") AS UNSIGNED)), " + base + ") + 1 FROM " + table);
    }
    static String scalar(Connection c, String sql, Object... a) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < a.length; i++) ps.setObject(i + 1, a[i]);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }
    static int update(Connection c, String sql, Object... a) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < a.length; i++) ps.setObject(i + 1, a[i]);
            return ps.executeUpdate();
        }
    }
}
