package lv.priority.queue;

import lv.priority.queue.db.Database;
import lv.priority.queue.db.DatabaseDAO;

import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.time.Instant;

// Interactive command-line shell for queue and DB operations.
public class CLI {
    private final Queue queue;
    private final Scanner in = new Scanner(System.in);
    private final Database database;
    private final DatabaseDAO dao;
    private String currentUser;
    private String currentRole;
    private long startTime = System.currentTimeMillis();
    private double avgReorderTime = 0.0;

    public CLI(Queue queue) {
        this.queue = queue;
        this.database = new Database();
        this.dao = new DatabaseDAO(database);
        try {
            // ensure schema exists and load stored data
            database.init();
            dao.loadAll(queue);
        } catch (SQLException e) {
            System.err.println("Warning: failed to load DB: " + e.getMessage());
        }
    }

    // Reads and executes commands until user exits.
    public void run() {
        if (!authenticate()) {
            System.out.println("Authentication failed. Exiting.");
            return;
        }
        System.out.println("Priority Queue CLI. Logged in as " + currentUser + " (" + currentRole + "). Type 'help' for commands.");
        while (true) {
            System.out.print("> ");
            String line;
            try {
                // Read one command line from stdin.
                line = in.nextLine();
            } catch (Exception e) {
                System.out.println("Goodbye");
                return;
            }
            if (line == null) return;
            line = line.trim();
            if (line.isEmpty()) continue;
            // First token is command name, remaining tokens are arguments.
            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();
            try {
                switch (cmd) {
                    case "help":
                        printHelp();
                        break;
                    case "exit":
                    case "quit":
                        System.out.println("Bye");
                        return;
                    case "adduser":
                        if (!"Admin".equals(currentRole)) {
                            System.out.println("Access denied. Admin required.");
                            break;
                        }
                        // adduser <name>
                        if (parts.length < 2) {
                            System.out.println("Usage: adduser <name>");
                            break;
                        }
                        {
                            String name = parts[1];
                            try {
                                dao.addItem(name);
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                            queue.addItem(new Item(name));
                            System.out.println("Added user: " + name);
                        }
                        break;
                    case "setattr":
                        // setattr <name> <attr> <value>
                        if (parts.length < 4) {
                            System.out.println("Usage: setattr <name> <attr> <value>");
                            break;
                        }
                        {
                            String name = parts[1];
                            String attr = parts[2];
                            double val = Double.parseDouble(parts[3]);
                            if (val < 0 || val > 1) {
                                System.out.println("Value must be between 0 and 1");
                                break;
                            }
                            long start = System.nanoTime();
                            queue.updateItemAttribute(name, attr, val);
                            long end = System.nanoTime();
                            avgReorderTime = (avgReorderTime + (end - start) / 1e6) / 2; // average in ms
                            try {
                                dao.saveAttribute(name, attr, val);
                                dao.updateStats((System.currentTimeMillis() - startTime) / 1000.0, avgReorderTime);
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                            System.out.println("Assigned attribute '" + attr + "' = " + val + " to " + name);
                        }
                        break;
                    case "setimp":
                        if (!"Admin".equals(currentRole)) {
                            System.out.println("Access denied. Admin required.");
                            break;
                        }
                        // setimp <attr> <weight> <rule>
                        if (parts.length < 4) {
                            System.out.println("Usage: setimp <attr> <weight> <rule>");
                            break;
                        }
                        {
                            String attr = parts[1];
                            double w = Double.parseDouble(parts[2]);
                            String rule = parts[3].toUpperCase();
                            if (!"ASC".equals(rule) && !"DESC".equals(rule)) {
                                System.out.println("Rule must be ASC or DESC");
                                break;
                            }
                            queue.setAttribute(new Attribute(attr, w, rule));
                            try {
                                dao.setAttribute(attr, w, rule);
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                            System.out.println("Set attribute '" + attr + "' weight=" + w + " rule=" + rule);
                        }
                        break;
                    case "createattr":
                        if (!"Admin".equals(currentRole)) {
                            System.out.println("Access denied. Admin required.");
                            break;
                        }
                        // createattr <name> <coef> <rule>
                        if (parts.length < 4) {
                            System.out.println("Usage: createattr <name> <coef> <rule>");
                            break;
                        }
                        {
                            String attr = parts[1];
                            double w = Double.parseDouble(parts[2]);
                            String rule = parts[3].toUpperCase();
                            try {
                                dao.setAttribute(attr, w, rule);
                                queue.setAttribute(new Attribute(attr, w, rule));
                                System.out.println("Created/updated attribute: " + attr + "=" + w + " " + rule);
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                        }
                        break;
                    case "listattrs":
                        try {
                            Map<String, Attribute> attrs = dao.listAttributes();
                            System.out.println("Attributes (name = coefficient rule):");
                            for (Map.Entry<String, Attribute> e : attrs.entrySet()) {
                                Attribute a = e.getValue();
                                System.out.printf("- %s = %.4f %s\n", e.getKey(), a.getWeight(), a.getRule());
                            }
                        } catch (SQLException ex) {
                            System.out.println("DB error: " + ex.getMessage());
                        }
                        break;
                    case "list":
                        // list current ordering - fetch fresh from DB
                        try {
                            // Pull persisted scores so output reflects DB source of truth.
                            Map<String, Attribute> imps = dao.listAttributes();
                            Map<String, Item> items = dao.getAllItems();
                            Map<String, Double> scores = dao.getStoredScores();
                            List<Item> ordered = new ArrayList<>(items.values());
                            ordered.sort((a, b) -> Double.compare(
                                    scores.getOrDefault(b.getName(), b.computeScore(imps)),
                                    scores.getOrDefault(a.getName(), a.computeScore(imps))));

                            System.out.println("CURRENT PRIORITY QUEUE");
                            System.out.println("Position | Name          | Score     | Attributes");
                            System.out.println("---------|---------------|-----------|-----------");
                            int pos = 1;
                            for (Item it : ordered) {
                                double s = scores.getOrDefault(it.getName(), it.computeScore(imps));
                                StringBuilder as = new StringBuilder();
                                for (Map.Entry<String, Double> entry : it.getAttributes().entrySet()) {
                                    if (as.length() > 0) as.append(", ");
                                    as.append(entry.getKey()).append("=").append(String.format("%.2f", entry.getValue()));
                                }
                                System.out.printf("%-8d | %-13s | %-9.4f | %s\n", pos++, it.getName(), s, as.toString());
                            }
                        } catch (SQLException ex) {
                            System.out.println("DB error: " + ex.getMessage());
                        }
                        break;
                    case "show":
                        // show <name>
                        if (parts.length < 2) {
                            System.out.println("Usage: show <name>");
                            break;
                        }
                        {
                            String name = parts[1];
                            try {
                                Map<String, Attribute> imps = dao.listAttributes();
                                Item it = dao.getItem(name);
                                if (it == null) {
                                    System.out.println("No such item: " + name);
                                } else {
                                    double s = it.computeScore(imps);
                                    System.out.println("User: " + it.getName());
                                    System.out.printf("Score: %.4f\n", s);
                                    System.out.println("Attributes:");
                                    if (it.getAttributes().isEmpty()) {
                                        System.out.println("  (none)");
                                    } else {
                                        for (Map.Entry<String, Double> entry : it.getAttributes().entrySet()) {
                                            System.out.printf("  - %s = %.2f\n", entry.getKey(), entry.getValue());
                                        }
                                    }
                                }
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                        }
                        break;
                    case "remove":
                        if (!"Admin".equals(currentRole)) {
                            System.out.println("Access denied. Admin required.");
                            break;
                        }
                        // remove <name>
                        if (parts.length < 2) {
                            System.out.println("Usage: remove <name>");
                            break;
                        }
                        {
                            String name = parts[1];
                            queue.removeItem(name);
                            try {
                                dao.removeItem(name);
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                            System.out.println("Removed " + name);
                        }
                        break;
                    case "poll":
                        if (!"Worker".equals(currentRole)) {
                            System.out.println("Access denied. Worker required.");
                            break;
                        }
                        // Poll from in-memory queue; then mirror removal + audit data in DB.
                        Item p = queue.poll();
                        if (p == null) {
                            System.out.println("Queue empty");
                        } else {
                            System.out.println("Processing item: " + p.getName());
                            try {
                                dao.removeItem(p.getName());
                                dao.addToHistory(p.getName(), currentUser);
                                dao.updateStats((System.currentTimeMillis() - startTime) / 1000.0, avgReorderTime);
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                        }
                        break;
                    case "search":
                        // search <query>
                        if (parts.length < 2) {
                            System.out.println("Usage: search <query>");
                            break;
                        }
                        {
                            String query = parts[1];
                            try {
                                List<String> results = dao.searchItems(query);
                                if (results.isEmpty()) {
                                    System.out.println("No items found");
                                } else {
                                    System.out.println("Search results:");
                                    for (String r : results) {
                                        System.out.println("  " + r);
                                    }
                                }
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                        }
                        break;
                    case "history":
                        try {
                            List<String> hist = dao.getHistory();
                            System.out.println("Processing History:");
                            for (String h : hist) {
                                System.out.println("  " + h);
                            }
                        } catch (SQLException ex) {
                            System.out.println("DB error: " + ex.getMessage());
                        }
                        break;
                    case "stats":
                        try {
                            double[] stats = dao.getStats();
                            List<String> last = dao.getLastProcessed();
                            System.out.println("System Statistics:");
                            System.out.printf("Uptime: %.2f seconds\n", stats[0]);
                            System.out.printf("Average reordering time: %.2f ms\n", stats[1]);
                            System.out.println("Last 10 processed items:");
                            for (String l : last) {
                                System.out.println("  " + l);
                            }
                        } catch (SQLException ex) {
                            System.out.println("DB error: " + ex.getMessage());
                        }
                        break;
                    default:
                        System.out.println("Unknown command: " + cmd + ". Type 'help'.");
                }
            } catch (Exception e) {
                // Keep CLI loop alive on bad input (e.g., number parsing errors).
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private boolean authenticate() {
        // Simple username/password login against the lietotajs table.
        System.out.println("Priority Queue System Login");
        System.out.print("Username: ");
        String username = in.nextLine().trim();
        System.out.print("Password: ");
        String password = in.nextLine().trim();
        try {
            currentRole = dao.authenticate(username, password);
            if (currentRole != null) {
                currentUser = username;
                return true;
            }
        } catch (SQLException e) {
            System.out.println("DB error: " + e.getMessage());
        }
        return false;
    }

    private void printHelp() {
        // Keep this output aligned with switch-case command handlers.
        System.out.println("Commands:");
        System.out.println("  help                Show this help");
        System.out.println("  adduser <name>      Add a new item/user (Admin)");
        System.out.println("  setattr <name> <attr> <value>  Assign attribute value to item");
        System.out.println("  setimp <attr> <weight> <rule>  Set attribute importance and rule (Admin)");
        System.out.println("  createattr <name> <coef> <rule> Create/update attribute definition (Admin)");
        System.out.println("  listattrs           List defined attributes and rules");
        System.out.println("  list                List items ordered by score");
        System.out.println("  show <name>         Show item details and score");
        System.out.println("  remove <name>       Remove an item (Admin)");
        System.out.println("  poll                Process highest-priority item (Worker)");
        System.out.println("  search <query>      Search items by ID or name");
        System.out.println("  history             Show processing history");
        System.out.println("  stats               Show system statistics");
        System.out.println("  exit                Quit");
    }
}
