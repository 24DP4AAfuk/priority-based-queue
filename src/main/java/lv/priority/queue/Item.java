package lv.priority.queue;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;

public class Item {
    private String name;
    private Set<String> attributes; // attribute presence set

    public Item(String name) {
        this.name = name;
        this.attributes = new HashSet<>();
    }

    public Item(String name, Set<String> attributes) {
        this.name = name;
        this.attributes = new HashSet<>(attributes);
    }

    public String getName() {
        return name;
    }

    public Set<String> getAttributes() {
        return attributes;
    }

    public void setAttribute(String attrName) {
        attributes.add(attrName);
    }

    public boolean hasAttribute(String attrName) {
        return attributes.contains(attrName);
    }

    // Compute a combined score given attribute importance weights.
    // If an importance weight for an attribute is not provided, treat it as 1.0
    // so attribute values still contribute by default.
    public double computeScore(Map<String, Double> importanceWeights) {
        double score = 0.0;
        for (String attr : attributes) {
            double weight = 1.0;
            if (importanceWeights != null) {
                weight = importanceWeights.getOrDefault(attr, 1.0);
            }
            score += weight; // presence contributes weight (binary)
        }
        return score;
    }

    @Override
    public String toString() {
        return "Item{name='" + name + "', attributes=" + attributes + "}";
    }
}
