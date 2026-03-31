package lv.priority.queue;

public class Main {
    public static void main(String[] args) {
        Queue queue = new Queue();
        if (args.length > 0 && "demo".equalsIgnoreCase(args[0])) {
            // simple demo
            queue.setImportance("experience", 0.7);
            queue.setImportance("communication", 0.3);

            Item alice = new Item("Alice");
            // presence-only attributes
            alice.setAttribute("experience");
            alice.setAttribute("communication");

            Item bob = new Item("Bob");
            bob.setAttribute("communication");

            queue.addItem(alice);
            queue.addItem(bob);

            System.out.println("Demo ordering:");
            for (Item it : queue.orderedSnapshot()) {
                System.out.println(it.getName() + " -> " + it.computeScore(queue.getImportanceWeights()));
            }
            return;
        }
        CLI cli = new CLI(queue);
        cli.run();
    }
}
