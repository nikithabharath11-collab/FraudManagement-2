/* Fraud Management System (Part 2, JavaFX) - B NIKITHA, 25BCE2517 */
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

/**
 * Bank Guard - desktop version of the Fraud Management System.
 * One window, one tab per part of the system; every table is filled over JDBC (see GuardDb).
 * The whole user interface is built in Java code (no HTML, no CSS files).
 */
public class GuardApp extends Application {
    static final String OWNER = "B NIKITHA  |  Reg. No. 25BCE2517  |  VIT Vellore  |  B.Tech CSE  |  DBMS Project Part 2 (JavaFX)";

    // colours (same palette as the part 1 web page)
    static final Color ACCENT = Color.web("#1f4e8c"), PAGE = Color.web("#f5f6fa"), CARD = Color.WHITE,
                       LINE = Color.web("#e3e6ee"), MUTED = Color.web("#667085"), INK = Color.web("#1d2433");

    // tab key -> tab title, in the same order as the web version
    static final Map<String, String> TABS = new LinkedHashMap<>();
    static {
        TABS.put("dashboard", "Dashboard");       TABS.put("transactions", "Transactions");
        TABS.put("alerts", "Fraud Alerts");       TABS.put("customers", "Customers");
        TABS.put("accounts", "Accounts");         TABS.put("devices", "Devices");
        TABS.put("merchants", "Merchants");       TABS.put("rules", "Rules");
        TABS.put("logs", "Activity Log");         TABS.put("risk", "Risk Summary");
        TABS.put("riskscores", "Risk Scores");    TABS.put("statements", "Monthly Statements");
    }

    // status words that get a coloured tag in the tables: red / amber / green
    static final Set<String> TAG_COLUMNS = Set.of("severity", "alert_status", "status", "standing", "risk_level", "risk_band");
    static final Set<String> RED   = Set.of("High", "Blocked", "Confirmed Fraud");
    static final Set<String> AMBER = Set.of("Medium", "Open", "Under Review", "Suspended", "Frozen");
    static final Set<String> GREEN = Set.of("Low", "Cleared", "Completed", "Active");

    private final TabPane tabs = new TabPane();
    private final Label status = new Label("Connecting...");
    private final Label message = new Label();

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        Label title = new Label("Fraud Management System  -  Bank Guard");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);
        status.setPadding(new Insets(3, 12, 3, 12));
        status.setTextFill(Color.WHITE);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> show(tabs.getSelectionModel().getSelectedItem()));
        HBox header = new HBox(12, title, gap, status, refresh);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setBackground(fill(ACCENT, 0));

        message.setPadding(new Insets(8, 14, 8, 14));
        message.setMaxWidth(Double.MAX_VALUE);
        message.setVisible(false);
        message.setManaged(false);

        for (Map.Entry<String, String> t : TABS.entrySet()) {
            Tab tab = new Tab(t.getValue());
            tab.setUserData(t.getKey());
            tab.setClosable(false);
            tabs.getTabs().add(tab);
        }
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, tab) -> show(tab));

        Label footer = new Label(OWNER);
        footer.setTextFill(MUTED);
        footer.setPadding(new Insets(8, 20, 8, 20));
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.setBackground(fill(CARD, 0));
        footer.setBorder(new Border(new BorderStroke(LINE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1, 0, 0, 0))));

        BorderPane root = new BorderPane(tabs, new VBox(header, message), null, footer, null);
        root.setBackground(fill(PAGE, 0));

        stage.setTitle("Fraud Management System (Bank Guard) - B NIKITHA, 25BCE2517");
        stage.setScene(new Scene(root, 1280, 800));
        stage.show();
        show(tabs.getTabs().get(0));
    }

    /** Builds the content of a tab fresh from the database every time it is opened. */
    private void show(Tab tab) {
        String key = (String) tab.getUserData();
        VBox box = new VBox(10);
        box.setPadding(new Insets(14, 20, 14, 20));
        try {
            GuardDb.Rows summary = GuardDb.query(GuardDb.SUMMARY);
            setConnected(true);
            switch (key) {
                case "dashboard" -> {
                    box.getChildren().addAll(cards(summary), heading("Fraud alerts"));
                    addTable(box, GuardDb.tab("alerts"));
                }
                case "transactions" -> transactionsTab(box);
                case "alerts" -> alertsTab(box);
                case "customers" -> customersTab(box);
                default -> {
                    box.getChildren().add(heading(TABS.get(key)));
                    addTable(box, GuardDb.tab(key));
                }
            }
        } catch (Exception e) {
            setConnected(false);
            box.getChildren().addAll(heading("Database unreachable"), new Label(e.getMessage()),
                new Label("Check that MySQL is running, database/guard_schema.sql is loaded and FRAUD_DB_PASS is correct."));
        }
        tab.setContent(box);
    }

    // ---------- tabs with forms ----------

    private void transactionsTab(VBox box) throws Exception {
        ComboBox<String> account  = pick(GuardDb.tab("accounts"), "account_number", "account_label");
        ComboBox<String> device   = pick(GuardDb.tab("devices"), "device_ref", "device_name");
        ComboBox<String> merchant = pick(GuardDb.tab("merchants"), "merchant_ref", "merchant_name");
        ComboBox<String> kind     = choice("Purchase", "Transfer", "Withdrawal");
        TextField amount   = field("e.g. 2500");
        TextField location = field("Chennai, IN");
        TextField hour     = field("e.g. 2");
        Button submit = button("Submit", () -> GuardDb.checkTransaction(id(account), id(device), id(merchant),
                kind.getValue(), amount.getText(), location.getText(), hour.getText()));
        box.getChildren().addAll(heading("New transaction (rules run automatically)"),
            form(submit, "From account", account, "Device", device, "Merchant", merchant, "Kind", kind,
                 "Amount (INR)", amount, "Location", location, "Simulate hour (0-23, optional)", hour),
            heading("Transactions"));
        addTable(box, GuardDb.tab("transactions"));
    }

    private void alertsTab(VBox box) throws Exception {
        GuardDb.Rows alerts = GuardDb.tab("alerts");
        ComboBox<String> alert = new ComboBox<>();
        for (int i = 0; i < alerts.size(); i++)
            if ("Open".equals(alerts.get(i, "alert_status")))
                alert.getItems().add(alerts.get(i, "alert_ref") + " - " + alerts.get(i, "txn_ref") + " · " + alerts.get(i, "rule_name"));
        alert.getSelectionModel().selectFirst();
        ComboBox<String> decision = choice("Cleared", "Confirmed Fraud");
        ComboBox<String> reviewer = new ComboBox<>();
        GuardDb.Rows customers = GuardDb.tab("customers");
        for (int i = 0; i < customers.size(); i++)
            if ("Administrator".equals(customers.get(i, "access_level")))
                reviewer.getItems().add(customers.get(i, "customer_number") + " - " + customers.get(i, "full_name"));
        reviewer.getSelectionModel().selectFirst();
        TextField notes = field("What did you check?");
        Button save = button("Save review", () -> {
            GuardDb.reviewAlert(id(alert), decision.getValue(), id(reviewer), notes.getText());
            return "Review saved";
        });
        box.getChildren().addAll(heading("Review an alert"),
            form(save, "Open alert", alert, "Decision", decision, "Reviewer (admins)", reviewer, "Notes", notes),
            heading("All alerts"));
        addTable(box, alerts);
    }

    private void customersTab(VBox box) throws Exception {
        GuardDb.Rows customers = GuardDb.tab("customers");
        TextField name = field("Full name"), email = field("name@example.com");
        ComboBox<String> level = choice("Standard", "Administrator");
        Button add = button("Add", () -> "Customer " + GuardDb.addCustomer(name.getText(), email.getText(), level.getValue()) + " added");

        ComboBox<String> who = pick(customers, "customer_number", "full_name");
        ComboBox<String> standing = choice("Active", "Suspended");
        Button change = button("Update", () -> { GuardDb.setStanding(id(who), standing.getValue()); return "Standing updated"; });

        ComboBox<String> gone = pick(customers, "customer_number", "full_name");
        Button delete = button("Delete", () -> { GuardDb.deleteCustomer(id(gone)); return "Customer deleted"; });

        HBox forms = new HBox(10,
            titled("Add customer", form(add, "Name", name, "Email", email, "Access level", level)),
            titled("Update standing", form(change, "Customer", who, "Standing", standing)),
            titled("Delete customer", form(delete, "Customer", gone)));
        box.getChildren().addAll(forms, heading("Customers"));
        addTable(box, customers);
    }

    // ---------- small building blocks ----------

    interface Action { String run() throws Exception; }

    /** A button that runs a database action, shows the result and reloads the tab. */
    private Button button(String text, Action action) {
        Button b = new Button(text);
        b.setTextFill(Color.WHITE);
        b.setBackground(fill(ACCENT, 6));
        b.setPadding(new Insets(7, 16, 7, 16));
        b.setOnAction(e -> {
            try {
                String result = action.run();
                show(tabs.getSelectionModel().getSelectedItem());
                toast(result, false);
            } catch (Exception ex) {
                toast(ex.getMessage(), true);
            }
        });
        return b;
    }

    /** Fields side by side (label above each control), then the button. */
    private Node form(Button submit, Object... labelsAndControls) {
        FlowPane pane = new FlowPane(12, 10);
        pane.setAlignment(Pos.BOTTOM_LEFT);
        for (int i = 0; i < labelsAndControls.length; i += 2) {
            Label l = new Label((String) labelsAndControls[i]);
            l.setTextFill(MUTED);
            Control c = (Control) labelsAndControls[i + 1];
            c.setPrefWidth(200);
            pane.getChildren().add(new VBox(3, l, c));
        }
        pane.getChildren().add(submit);
        pane.setPadding(new Insets(12));
        pane.setBackground(fill(CARD, 10));
        pane.setBorder(line());
        return pane;
    }

    private Node titled(String title, Node form) {
        VBox v = new VBox(6, heading(title), form);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    private void addTable(VBox box, GuardDb.Rows rows) {
        TableView<List<String>> table = table(rows);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.getChildren().add(table);
    }

    /** A TableView with one column per SQL column. */
    static TableView<List<String>> table(GuardDb.Rows rows) {
        TableView<List<String>> table = new TableView<>();
        for (int i = 0; i < rows.columns().size(); i++) {
            final int index = i;
            String name = rows.columns().get(i);
            TableColumn<List<String>, String> col = new TableColumn<>(name.replace('_', ' ').toUpperCase());
            col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().get(index) == null ? "–" : cell.getValue().get(index)));
            if (TAG_COLUMNS.contains(name)) col.setCellFactory(c -> new TagCell());
            int longest = name.length();
            for (List<String> row : rows.data())
                if (row.get(i) != null) longest = Math.max(longest, Math.min(row.get(i).length(), 45));
            col.setPrefWidth(longest * 7.5 + 30);
            table.getColumns().add(col);
        }
        table.getItems().setAll(rows.data());
        table.setPlaceholder(new Label("No rows."));
        table.setMinHeight(160);
        return table;
    }

    /** Table cell that shows a status as a small coloured tag. */
    static class TagCell extends TableCell<List<String>, String> {
        @Override protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(null);
            if (empty || item == null) return;
            Label tag = new Label(item);
            tag.setPadding(new Insets(1, 9, 1, 9));
            if (RED.contains(item))        { tag.setBackground(fill(Color.web("#fde8e6"), 10)); tag.setTextFill(Color.web("#c0392b")); }
            else if (AMBER.contains(item)) { tag.setBackground(fill(Color.web("#fdf1d8"), 10)); tag.setTextFill(Color.web("#9a6a12")); }
            else if (GREEN.contains(item)) { tag.setBackground(fill(Color.web("#e1f3e8"), 10)); tag.setTextFill(Color.web("#2e7d4f")); }
            setGraphic(tag);
        }
    }

    /** Dashboard number cards from the summary query. */
    private Node cards(GuardDb.Rows summary) {
        HBox row = new HBox(12);
        for (String col : summary.columns()) {
            Label value = new Label(summary.get(0, col));
            value.setFont(Font.font("System", FontWeight.BOLD, 26));
            value.setTextFill(INK);
            Label name = new Label(col.replace('_', ' '));
            name.setTextFill(MUTED);
            VBox card = new VBox(2, value, name);
            card.setPadding(new Insets(14));
            card.setBackground(fill(CARD, 10));
            card.setBorder(line());
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().add(card);
        }
        return row;
    }

    /** Drop-down of "id - label" built from a query result. */
    static ComboBox<String> pick(GuardDb.Rows rows, String idCol, String labelCol) {
        ComboBox<String> box = new ComboBox<>();
        for (int i = 0; i < rows.size(); i++) box.getItems().add(rows.get(i, idCol) + " - " + rows.get(i, labelCol));
        box.getSelectionModel().selectFirst();
        return box;
    }

    static ComboBox<String> choice(String... values) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll(values);
        box.getSelectionModel().selectFirst();
        return box;
    }

    /** The id part of an "id - label" drop-down value. */
    static String id(ComboBox<String> box) {
        if (box.getValue() == null) throw new IllegalArgumentException("Nothing to choose from in one of the lists");
        return box.getValue().split(" - ")[0];
    }

    static TextField field(String hint) {
        TextField f = new TextField();
        f.setPromptText(hint);
        return f;
    }

    static Label heading(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 15));
        l.setTextFill(INK);
        return l;
    }

    private void setConnected(boolean ok) {
        status.setText(ok ? "JDBC connected · MySQL" : "Database unreachable");
        status.setBackground(fill(ok ? Color.web("#2e7d4f") : Color.web("#c0392b"), 12));
    }

    private void toast(String text, boolean bad) {
        message.setText(text);
        message.setTextFill(bad ? Color.web("#c0392b") : Color.web("#2e7d4f"));
        message.setBackground(fill(bad ? Color.web("#fde8e6") : Color.web("#e1f3e8"), 0));
        message.setVisible(true);
        message.setManaged(true);
        PauseTransition hide = new PauseTransition(Duration.seconds(6));
        hide.setOnFinished(e -> { if (message.getText().equals(text)) { message.setVisible(false); message.setManaged(false); } });
        hide.play();
    }

    static Background fill(Color color, double radius) {
        return new Background(new BackgroundFill(color, new CornerRadii(radius), Insets.EMPTY));
    }

    static Border line() {
        return new Border(new BorderStroke(LINE, BorderStrokeStyle.SOLID, new CornerRadii(10), BorderWidths.DEFAULT));
    }
}
