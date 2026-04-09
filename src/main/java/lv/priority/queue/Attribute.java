package lv.priority.queue;

// Defines one scoring attribute with weight and ordering rule.
public class Attribute {
    private String name;
    private double weight; // importance from 0.0 to 1.0
    private String rule; // "ASC" or "DESC"

    public Attribute(String name, double weight, String rule) {
        this.name = name;
        this.weight = weight;
        this.rule = rule != null ? rule : "ASC";
    }

    public String getName() {
        return name;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getRule() {
        return rule;
    }

    // Accepts expected values ASC/DESC; caller validation happens in CLI/DAO paths.
    public void setRule(String rule) {
        this.rule = rule;
    }
}
