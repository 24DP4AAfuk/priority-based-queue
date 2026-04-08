package lv.priority.queue;

import java.util.List;
import java.util.Locale;
import java.util.Map;

// Application entry point that runs either demo mode or interactive CLI.
public class Main {
    public static void main(String[] args) {
        Queue queue = new Queue();
        if (args.length > 0 && "demo".equalsIgnoreCase(args[0])) {
            runDemo(queue);
            return;
        }
        CLI cli = new CLI(queue);
        cli.run();
    }

    private static void runDemo(Queue queue) {
        System.out.println("============================================");
        System.out.println(" Priority Queue Demo: Support Ticket Triage ");
        System.out.println("============================================");

        queue.setAttribute(new Attribute("urgency", 0.50, "ASC"));
        queue.setAttribute(new Attribute("impact", 0.30, "ASC"));
        queue.setAttribute(new Attribute("wait_time", 0.20, "DESC"));

        Item paymentOutage = new Item("Payment outage");
        paymentOutage.setAttribute("urgency", 1.00);
        paymentOutage.setAttribute("impact", 0.95);
        paymentOutage.setAttribute("wait_time", 0.10);

        Item onboardingIssue = new Item("Onboarding issue");
        onboardingIssue.setAttribute("urgency", 0.55);
        onboardingIssue.setAttribute("impact", 0.50);
        onboardingIssue.setAttribute("wait_time", 0.80);

        Item mobileCrash = new Item("Mobile app crash");
        mobileCrash.setAttribute("urgency", 0.90);
        mobileCrash.setAttribute("impact", 0.85);
        mobileCrash.setAttribute("wait_time", 0.35);

        Item invoiceQuestion = new Item("Invoice question");
        invoiceQuestion.setAttribute("urgency", 0.35);
        invoiceQuestion.setAttribute("impact", 0.25);
        invoiceQuestion.setAttribute("wait_time", 0.95);

        queue.addItem(paymentOutage);
        queue.addItem(onboardingIssue);
        queue.addItem(mobileCrash);
        queue.addItem(invoiceQuestion);

        printRules(queue.getAttributes());
        printRanking(queue);
        printProcessingSimulation(queue);
    }

    private static void printRules(Map<String, Attribute> attrs) {
        System.out.println();
        System.out.println("Scoring Rules");
        System.out.println("- ASC: higher value increases priority");
        System.out.println("- DESC: lower value increases priority (uses 1 - value)");
        System.out.println();
        System.out.printf(Locale.US, "%-12s | %-6s | %-6s%n", "Attribute", "Weight", "Rule");
        System.out.println("-------------|--------|------");
        for (Attribute attr : attrs.values()) {
            System.out.printf(Locale.US, "%-12s | %-6.2f | %-6s%n", attr.getName(), attr.getWeight(), attr.getRule());
        }
    }

    private static void printRanking(Queue queue) {
        Map<String, Attribute> attrs = queue.getAttributes();
        List<Item> ordered = queue.orderedSnapshot();

        System.out.println();
        System.out.println("Initial Queue Ranking (highest score first)");
        System.out.printf(Locale.US, "%-4s | %-20s | %-8s | %-8s | %-9s | %-7s%n",
                "Pos", "Ticket", "Urgency", "Impact", "WaitTime", "Score");
        System.out.println("-----|----------------------|----------|----------|-----------|--------");

        int pos = 1;
        for (Item item : ordered) {
                System.out.printf(Locale.US, "%-4d | %-20s | %-8.2f | %-8.2f | %-9.2f | %-7.4f%n",
                    pos++,
                    item.getName(),
                    item.getAttributeValue("urgency"),
                    item.getAttributeValue("impact"),
                    item.getAttributeValue("wait_time"),
                    item.computeScore(attrs));
        }
    }

    private static void printProcessingSimulation(Queue queue) {
        System.out.println();
        System.out.println("Processing Simulation");
        int step = 1;
        Item next;
        while ((next = queue.poll()) != null) {
                System.out.printf(Locale.US, "%d. Processing %-20s (score %.4f)%n",
                    step++,
                    next.getName(),
                    next.computeScore(queue.getAttributes()));
        }
        System.out.println("Queue is now empty.");
    }
}
