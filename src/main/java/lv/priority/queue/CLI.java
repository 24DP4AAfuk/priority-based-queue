package lv.priority.queue;

import java.util.Map;
import java.util.Scanner;

public class CLI {
    private final Queue queue;
    private final Scanner in = new Scanner(System.in);

    public CLI(Queue queue) {
        this.queue = queue;
    }

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
                        queue.addItem(new Item(parts[1]));
                        System.out.println("Added user: " + parts[1]);
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
                            double value = Double.parseDouble(parts[3]);
                            queue.updateItemAttribute(name, attr, value);
                            System.out.println("Set attribute '" + attr + "' for " + name + " = " + value);
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
                            System.out.println("Set importance '" + attr + "' = " + w);
                        }
                        break;
                    case "list":
                        // list current ordering
                        Map<String, Double> imps = queue.getImportanceWeights();
                        System.out.println("Current ordering (highest score first):");
                        System.out.printf("%-20s %-10s %s\n", "Name", "Score", "Attributes");
                        System.out.println("---------------------------------------------------------------");
                        for (Item it : queue.orderedSnapshot()) {
                            double s = it.computeScore(imps);
                            // build attribute string
                            StringBuilder as = new StringBuilder();
                            for (Map.Entry<String, Double> e : it.getAttributes().entrySet()) {
                                if (as.length() > 0) as.append(", ");
                                as.append(e.getKey()).append("=").append(String.format("%.2f", e.getValue()));
                            }
                            System.out.printf("%-20s %-10.4f %s\n", it.getName(), s, as.toString());
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
                            Item it = queue.getItem(name);
                            if (it == null) {
                                System.out.println("No such item: " + name);
                            } else {
                                double s = queue.getScoreForItem(name);
                                System.out.println("User: " + it.getName());
                                System.out.printf("Score: %.4f\n", s);
                                System.out.println("Attributes:");
                                if (it.getAttributes().isEmpty()) {
                                    System.out.println("  (none)");
                                } else {
                                    for (Map.Entry<String, Double> e : it.getAttributes().entrySet()) {
                                        System.out.printf("  - %-16s : %.4f\n", e.getKey(), e.getValue());
                                    }
                                }
                            }
                        }
                        break;
                    case "remove":
                        // remove <name>
                        if (parts.length < 2) {
                            System.out.println("Usage: remove <name>");
                            break;
                        }
                        queue.removeItem(parts[1]);
                        System.out.println("Removed " + parts[1]);
                        break;
                    case "poll":
                        Item p = queue.poll();
                        if (p == null) System.out.println("Queue empty"); else System.out.println("Polled: " + p.getName());
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
        System.out.println("  setattr <name> <attr> <value>   Set attribute value (0.0-1.0)");
        System.out.println("  setimp <attr> <weight>           Set importance weight (0.0-1.0)");
        System.out.println("  list                List items ordered by score");
        System.out.println("  show <name>         Show item details and score");
        System.out.println("  remove <name>       Remove an item");
        System.out.println("  poll                Pop highest-priority item");
        System.out.println("  imp                 Show importance weights");
        System.out.println("  exit                Quit");
    }
}
