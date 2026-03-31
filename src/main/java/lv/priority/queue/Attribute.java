package lv.priority.queue;

public class Attribute {
    private String name;
    private double weight; // importance from 0.0 to 1.0

    public Attribute(String name, double weight) {
        this.name = name;
        this.weight = weight;
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
}
