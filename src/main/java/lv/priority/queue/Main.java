package lv.priority.queue;

public class Main {
    public static void main(String[] args) {
        Queue queue = new Queue();
        if (args.length > 0 && "demo".equalsIgnoreCase(args[0])) {
            // simple demo
            queue.setAttribute(new Attribute("experience", 0.7, "ASC"));
            queue.setAttribute(new Attribute("communication", 0.3, "ASC"));

            Item alice = new Item("Alice");
            // attributes with values
            alice.setAttribute("experience", 0.8);
            alice.setAttribute("communication", 0.9);

            Item bob = new Item("Bob");
            bob.setAttribute("communication", 0.6);

            queue.addItem(alice);
            queue.addItem(bob);

            System.out.println("Demo ordering:");
            for (Item it : queue.orderedSnapshot()) {
                System.out.println(it.getName() + " -> " + it.computeScore(queue.getAttributes()));
            }
            return;
        }
        CLI cli = new CLI(queue);
        cli.run();
    }
}
