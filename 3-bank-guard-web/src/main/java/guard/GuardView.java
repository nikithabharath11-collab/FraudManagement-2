/* Fraud Management System (Part 2, web version) - B NIKITHA, 25BCE2517 */
package guard;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.*;

/**
 * Bank Guard - web version of the Fraud Management System, written only in Java with Vaadin Flow.
 * It has the same tabs, forms and colours as the JavaFX app (GuardApp.java) and uses the same
 * JDBC class (GuardDb), so every table is still read from MySQL with plain SQL, views and the procedure.
 * Vaadin turns these Java components into a web page and sends each click back to this class.
 */
@Route("fraud")
@PageTitle("Fraud Management System - Bank Guard (B NIKITHA)")
public class GuardView extends VerticalLayout {
    static final String OWNER = "B NIKITHA  |  Reg. No. 25BCE2517  |  VIT Vellore  |  B.Tech CSE  |  DBMS Project Part 2 (web version, Vaadin)";

    // same palette as the JavaFX app and the part 1 page
    static final String ACCENT = "#1f4e8c", PAGE = "#f5f6fa", CARD = "#ffffff", LINE = "#e3e6ee", MUTED = "#667085", INK = "#1d2433";

    static final Map<String, String> TABS = new LinkedHashMap<>();
    static {
        TABS.put("dashboard", "Dashboard");       TABS.put("transactions", "Transactions");
        TABS.put("alerts", "Fraud Alerts");       TABS.put("customers", "Customers");
        TABS.put("accounts", "Accounts");         TABS.put("devices", "Devices");
        TABS.put("merchants", "Merchants");       TABS.put("rules", "Rules");
        TABS.put("logs", "Activity Log");         TABS.put("risk", "Risk Summary");
        TABS.put("riskscores", "Risk Scores");    TABS.put("statements", "Monthly Statements");
    }

    // status words shown as coloured tags: red / amber / green
    static final Set<String> TAG_COLUMNS = Set.of("severity", "alert_status", "status", "standing", "risk_level", "risk_band");
    static final Set<String> RED   = Set.of("High", "Blocked", "Confirmed Fraud");
    static final Set<String> AMBER = Set.of("Medium", "Open", "Under Review", "Suspended", "Frozen");
    static final Set<String> GREEN = Set.of("Low", "Cleared", "Completed", "Active");

    private final Tabs tabs = new Tabs();
    private final Map<Tab, String> keys = new HashMap<>();
    private final VerticalLayout content = new VerticalLayout();
    private final Span status = new Span("Connecting...");
    private final Div message = new Div();

    public GuardView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", PAGE).set("color", INK);
        // Lumo theme colours for this app (also used by drop-down lists, which open outside the view)
        addAttachListener(e -> theme(e.getUI()));

        Span title = new Span("Fraud Management System  -  Bank Guard");
        title.getStyle().set("font-size", "1.25rem").set("font-weight", "700").set("color", "white").set("white-space", "pre");
        status.getStyle().set("color", "white").set("padding", "3px 12px").set("border-radius", "12px").set("font-size", "0.85rem");
        Button refresh = new Button("Refresh", e -> show());
        refresh.getStyle().set("background", "white").set("color", ACCENT);
        HorizontalLayout header = new HorizontalLayout(title, status, refresh);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.getStyle().set("background", ACCENT).set("padding", "12px 20px").set("box-sizing", "border-box").set("flex-wrap", "wrap");

        message.setVisible(false);
        message.setWidthFull();
        message.getStyle().set("padding", "8px 20px").set("box-sizing", "border-box");

        for (Map.Entry<String, String> t : TABS.entrySet()) {
            Tab tab = new Tab(t.getValue());
            keys.put(tab, t.getKey());
            tabs.add(tab);
        }
        tabs.setWidthFull();
        tabs.getStyle().set("background", CARD).set("border-bottom", "1px solid " + LINE);
        tabs.addSelectedChangeListener(e -> show());

        content.setSizeFull();
        content.setPadding(true);
        content.getStyle().set("overflow", "auto");

        Span footer = new Span(OWNER);
        footer.setWidthFull();
        footer.getStyle().set("color", MUTED).set("background", CARD).set("padding", "8px 20px").set("box-sizing", "border-box")
              .set("border-top", "1px solid " + LINE).set("font-size", "0.85rem").set("white-space", "pre-wrap");

        add(header, message, tabs, content, footer);
        expand(content);
        show();
    }

    static void theme(UI ui) {
        ui.getElement().getStyle().set("--lumo-primary-color", ACCENT).set("--lumo-primary-text-color", ACCENT)
          .set("--lumo-primary-color-50pct", ACCENT + "80").set("--lumo-primary-color-10pct", ACCENT + "1a")
          .set("--lumo-base-color", CARD).set("background", PAGE);
    }

    /** Builds the selected tab fresh from the database every time it is opened (like the JavaFX app). */
    private void show() {
        String key = keys.get(tabs.getSelectedTab());
        content.removeAll();
        try {
            GuardDb.Rows summary = GuardDb.query(GuardDb.SUMMARY);
            setConnected(true);
            switch (key) {
                case "dashboard" -> {
                    content.add(cards(summary), heading("Fraud alerts"));
                    addTable(GuardDb.tab("alerts"));
                }
                case "transactions" -> transactionsTab();
                case "alerts" -> alertsTab();
                case "customers" -> customersTab();
                default -> {
                    content.add(heading(TABS.get(key)));
                    addTable(GuardDb.tab(key));
                }
            }
        } catch (Exception e) {
            setConnected(false);
            content.add(heading("Database unreachable"), new Span(String.valueOf(e.getMessage())),
                new Span("Check that MySQL is running, database/guard_schema.sql is loaded and FRAUD_DB_PASS is correct."));
        }
    }

    // ---------- tabs with forms ----------

    private void transactionsTab() throws Exception {
        ComboBox<String> account  = pick(GuardDb.tab("accounts"), "account_number", "account_label");
        ComboBox<String> device   = pick(GuardDb.tab("devices"), "device_ref", "device_name");
        ComboBox<String> merchant = pick(GuardDb.tab("merchants"), "merchant_ref", "merchant_name");
        ComboBox<String> kind     = choice("Purchase", "Transfer", "Withdrawal");
        TextField amount   = field("e.g. 2500");
        TextField location = field("Chennai, IN");
        TextField hour     = field("e.g. 2");
        Button submit = button("Submit", () -> GuardDb.checkTransaction(id(account), id(device), id(merchant),
                kind.getValue(), amount.getValue(), location.getValue(), hour.getValue()));
        content.add(heading("New transaction (rules run automatically)"),
            form(submit, "From account", account, "Device", device, "Merchant", merchant, "Kind", kind,
                 "Amount (INR)", amount, "Location", location, "Simulate hour (0-23, optional)", hour),
            heading("Transactions"));
        addTable(GuardDb.tab("transactions"));
    }

    private void alertsTab() throws Exception {
        GuardDb.Rows alerts = GuardDb.tab("alerts");
        List<String> open = new ArrayList<>();
        for (int i = 0; i < alerts.size(); i++)
            if ("Open".equals(alerts.get(i, "alert_status")))
                open.add(alerts.get(i, "alert_ref") + " - " + alerts.get(i, "txn_ref") + " · " + alerts.get(i, "rule_name"));
        ComboBox<String> alert = choice(open.toArray(new String[0]));
        ComboBox<String> decision = choice("Cleared", "Confirmed Fraud");
        GuardDb.Rows customers = GuardDb.tab("customers");
        List<String> admins = new ArrayList<>();
        for (int i = 0; i < customers.size(); i++)
            if ("Administrator".equals(customers.get(i, "access_level")))
                admins.add(customers.get(i, "customer_number") + " - " + customers.get(i, "full_name"));
        ComboBox<String> reviewer = choice(admins.toArray(new String[0]));
        TextField notes = field("What did you check?");
        Button save = button("Save review", () -> {
            GuardDb.reviewAlert(id(alert), decision.getValue(), id(reviewer), notes.getValue());
            return "Review saved";
        });
        content.add(heading("Review an alert"),
            form(save, "Open alert", alert, "Decision", decision, "Reviewer (admins)", reviewer, "Notes", notes),
            heading("All alerts"));
        addTable(alerts);
    }

    private void customersTab() throws Exception {
        GuardDb.Rows customers = GuardDb.tab("customers");
        TextField name = field("Full name"), email = field("name@example.com");
        ComboBox<String> level = choice("Standard", "Administrator");
        Button add = button("Add", () -> "Customer " + GuardDb.addCustomer(name.getValue(), email.getValue(), level.getValue()) + " added");

        ComboBox<String> who = pick(customers, "customer_number", "full_name");
        ComboBox<String> standing = choice("Active", "Suspended");
        Button change = button("Update", () -> { GuardDb.setStanding(id(who), standing.getValue()); return "Standing updated"; });

        ComboBox<String> gone = pick(customers, "customer_number", "full_name");
        Button delete = button("Delete", () -> { GuardDb.deleteCustomer(id(gone)); return "Customer deleted"; });

        FlexLayout forms = row(
            titled("Add customer", form(add, "Name", name, "Email", email, "Access level", level)),
            titled("Update standing", form(change, "Customer", who, "Standing", standing)),
            titled("Delete customer", form(delete, "Customer", gone)));
        content.add(forms, heading("Customers"));
        addTable(customers);
    }

    // ---------- small building blocks ----------

    interface Action { String run() throws Exception; }

    /** A button that runs a database action, shows the result and reloads the tab. */
    private Button button(String text, Action action) {
        Button b = new Button(text);
        b.getStyle().set("background", ACCENT).set("color", "white");
        b.addClickListener(e -> {
            try {
                String result = action.run();
                show();
                toast(result, false);
            } catch (Exception ex) {
                toast(ex.getMessage(), true);
            }
        });
        return b;
    }

    /** Fields side by side (label above each control), then the button, on a white card. */
    private Component form(Button submit, Object... labelsAndControls) {
        FlexLayout pane = new FlexLayout();
        pane.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        pane.setAlignItems(FlexComponent.Alignment.END);
        pane.getStyle().set("gap", "10px 12px").set("padding", "12px").set("background", CARD)
            .set("border", "1px solid " + LINE).set("border-radius", "10px").set("box-sizing", "border-box");
        pane.setWidthFull();
        for (int i = 0; i < labelsAndControls.length; i += 2) {
            Component c = (Component) labelsAndControls[i + 1];
            if (c instanceof ComboBox<?> box) { box.setLabel((String) labelsAndControls[i]); box.setWidth("210px"); }
            if (c instanceof TextField f)     { f.setLabel((String) labelsAndControls[i]); f.setWidth("210px"); }
            pane.add(c);
        }
        pane.add(submit);
        return pane;
    }

    private Component titled(String title, Component form) {
        VerticalLayout v = new VerticalLayout(heading(title), form);
        v.setPadding(false);
        v.setSpacing(false);
        v.getStyle().set("flex", "1 1 300px").set("gap", "6px");
        return v;
    }

    private static FlexLayout row(Component... parts) {
        FlexLayout r = new FlexLayout(parts);
        r.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        r.setWidthFull();
        r.getStyle().set("gap", "10px");
        return r;
    }

    private void addTable(GuardDb.Rows rows) {
        Grid<List<String>> grid = table(rows);
        content.add(grid);
        content.setFlexGrow(1, grid);
    }

    /** A Grid with one column per SQL column (header = column name, like the JavaFX TableView). */
    static Grid<List<String>> table(GuardDb.Rows rows) {
        Grid<List<String>> grid = new Grid<>();
        for (int i = 0; i < rows.columns().size(); i++) {
            final int index = i;
            String name = rows.columns().get(i);
            Grid.Column<List<String>> col;
            if (TAG_COLUMNS.contains(name)) col = grid.addColumn(new ComponentRenderer<>(r -> tag(r.get(index))));
            else col = grid.addColumn(r -> r.get(index) == null ? "–" : r.get(index));
            col.setHeader(name.replace('_', ' ').toUpperCase()).setAutoWidth(true).setResizable(true).setSortable(true);
            if (!TAG_COLUMNS.contains(name)) col.setComparator(r -> r.get(index) == null ? "" : r.get(index));
            if (name.contains("amount") || name.contains("balance") || name.contains("score")) col.setTextAlign(ColumnTextAlign.END);
        }
        grid.setItems(rows.data());
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setWidthFull();
        grid.setMinHeight("260px");
        grid.setEmptyStateText("No rows.");
        return grid;
    }

    /** A status shown as a small coloured tag. */
    static Component tag(String item) {
        Span s = new Span(item == null ? "–" : item);
        if (item == null) return s;
        String bg = null, fg = null;
        if (RED.contains(item))        { bg = "#fde8e6"; fg = "#c0392b"; }
        else if (AMBER.contains(item)) { bg = "#fdf1d8"; fg = "#9a6a12"; }
        else if (GREEN.contains(item)) { bg = "#e1f3e8"; fg = "#2e7d4f"; }
        if (bg != null) s.getStyle().set("background", bg).set("color", fg).set("padding", "1px 9px").set("border-radius", "10px");
        return s;
    }

    /** Dashboard number cards from the summary query. */
    private Component cards(GuardDb.Rows summary) {
        FlexLayout r = row();
        for (String col : summary.columns()) {
            Span value = new Span(summary.get(0, col));
            value.getStyle().set("font-size", "1.8rem").set("font-weight", "700").set("color", INK);
            Span name = new Span(col.replace('_', ' '));
            name.getStyle().set("color", MUTED);
            VerticalLayout card = new VerticalLayout(value, name);
            card.setSpacing(false);
            card.getStyle().set("flex", "1 1 160px").set("background", CARD).set("border", "1px solid " + LINE)
                .set("border-radius", "10px").set("padding", "14px");
            r.add(card);
        }
        return r;
    }

    /** Drop-down of "id - label" built from a query result. */
    static ComboBox<String> pick(GuardDb.Rows rows, String idCol, String labelCol) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) items.add(rows.get(i, idCol) + " - " + rows.get(i, labelCol));
        return choice(items.toArray(new String[0]));
    }

    static ComboBox<String> choice(String... values) {
        ComboBox<String> box = new ComboBox<>();
        box.setItems(values);
        if (values.length > 0) box.setValue(values[0]);
        return box;
    }

    /** The id part of an "id - label" drop-down value. */
    static String id(ComboBox<String> box) {
        if (box.getValue() == null) throw new IllegalArgumentException("Nothing to choose from in one of the lists");
        return box.getValue().split(" - ")[0];
    }

    static TextField field(String hint) {
        TextField f = new TextField();
        f.setPlaceholder(hint);
        return f;
    }

    static Span heading(String text) {
        Span l = new Span(text);
        l.getStyle().set("font-size", "1.05rem").set("font-weight", "700").set("color", INK).set("margin-top", "4px");
        return l;
    }

    private void setConnected(boolean ok) {
        status.setText(ok ? "JDBC connected · MySQL" : "Database unreachable");
        status.getStyle().set("background", ok ? "#2e7d4f" : "#c0392b");
    }

    private void toast(String text, boolean bad) {
        message.setText(text);
        message.getStyle().set("color", bad ? "#c0392b" : "#2e7d4f").set("background", bad ? "#fde8e6" : "#e1f3e8");
        message.setVisible(true);
    }
}
