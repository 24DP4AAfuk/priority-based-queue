package lv.priority.queue;

import lv.priority.queue.db.Database;
import lv.priority.queue.db.DatabaseDAO;

import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

// Interactive command-line shell for queue and DB operations.
public class CLI {
    private final Queue queue;
    private final Scanner in = new Scanner(System.in);
    private final Database database;
    private final DatabaseDAO dao;

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
        System.out.println("Priority Queue CLI. Type 'help' for commands.");
        while (true) {
            System.out.print("> ");
            String line;
            try {
                line = in.nextLine();
            } catch (Exception e) {
                System.out.println("Goodbye");
                return;
            }
            if (line == null) return;
            line = line.trim();
            if (line.isEmpty()) continue;
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
                        // setattr <name> <attr>
                        if (parts.length < 3) {
                            System.out.println("Usage: setattr <name> <attr>");
                            break;
                        }
                        {
                            String name = parts[1];
                            String attr = parts[2];
                            queue.updateItemAttribute(name, attr);
                            try {
                                dao.saveAttribute(name, attr);
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                            System.out.println("Assigned attribute '" + attr + "' to " + name);
                        }
                        break;
                    case "setimp":
                        // setimp <attr> <weight>
                        if (parts.length < 3) {
                            System.out.println("Usage: setimp <attr> <weight>");
                            break;
                        }
                        {
                            String attr = parts[1];
                            double w = Double.parseDouble(parts[2]);
                            queue.setImportance(attr, w);
                            try {
                                dao.setImportance(attr, w);
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                            System.out.println("Set importance '" + attr + "' = " + w);
                        }
                        break;
                    case "createattr":
                        // createattr <name> <coef>
                        if (parts.length < 3) {
                            System.out.println("Usage: createattr <name> <coef>");
                            break;
                        }
                        {
                            String attr = parts[1];
                            double w = Double.parseDouble(parts[2]);
                            try {
                                dao.setImportance(attr, w);
                                queue.setImportance(attr, w);
                                System.out.println("Created/updated attribute: " + attr + "=" + w);
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                        }
                        break;
                    case "listattrs":
                        try {
                            Map<String, Double> attrs = dao.listAttributes();
                            System.out.println("Attributes (name = coefficient):");
                            for (Map.Entry<String, Double> e : attrs.entrySet()) {
                                System.out.printf("- %s = %.4f\n", e.getKey(), e.getValue());
                            }
                        } catch (SQLException ex) {
                            System.out.println("DB error: " + ex.getMessage());
                        }
                        break;
                    case "list":
                        // list current ordering - fetch fresh from DB
                        try {
                            Map<String, Double> imps = dao.listAttributes();
                            Map<String, Item> items = dao.getAllItems();
                            List<Item> ordered = new ArrayList<>(items.values());
                            ordered.sort((a, b) -> Double.compare(b.computeScore(imps), a.computeScore(imps)));

                            System.out.println("Current ordering (highest score first):");
                            System.out.printf("%-20s %-10s %s\n", "Name", "Score", "Attributes");
                            System.out.println("---------------------------------------------------------------");
                            for (Item it : ordered) {
                                double s = it.computeScore(imps);
                                StringBuilder as = new StringBuilder();
                                for (String a : it.getAttributes()) {
                                    if (as.length() > 0) as.append(", ");
                                    as.append(a);
                                }
                                System.out.printf("%-20s %-10.4f %s\n", it.getName(), s, as.toString());
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
                                Map<String, Double> imps = dao.listAttributes();
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
                                        for (String a : it.getAttributes()) {
                                            System.out.printf("  - %s\n", a);
                                        }
                                    }
                                }
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                        }
                        break;
                    case "remove":
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
                        Item p = queue.poll();
                        if (p == null) {
                            System.out.println("Queue empty");
                        } else {
                            System.out.println("Polled: " + p.getName());
                            try {
                                dao.removeItem(p.getName());
                            } catch (SQLException ex) {
                                System.out.println("DB error: " + ex.getMessage());
                            }
                        }
                        break;
                    case "imp":
                        // show importances
                        System.out.println("Importance weights:");
                        for (Map.Entry<String, Double> e : queue.getImportanceWeights().entrySet()) {
                            System.out.printf("- %s = %.4f\n", e.getKey(), e.getValue());
                        }
                        break;
                    default:
                        System.out.println("Unknown command: " + cmd + ". Type 'help'.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("  help                Show this help");
        System.out.println("  adduser <name>      Add a user/item");
        System.out.println("  setattr <name> <attr>        Assign attribute to item (presence-only)");
        System.out.println("  setimp <attr> <weight>           Set importance weight (0.0-1.0)");
        System.out.println("  createattr <name> <coef>        Create/update attribute definition");
        System.out.println("  listattrs                         List defined attributes and coefficients");
        System.out.println("  list                List items ordered by score");
        System.out.println("  show <name>         Show item details and score");
        System.out.println("  remove <name>       Remove an item");
        System.out.println("  poll                Pop highest-priority item");
        System.out.println("  imp                 Show importance weights");
        System.out.println("  exit                Quit");
    }
}
