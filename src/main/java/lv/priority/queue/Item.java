package lv.priority.queue;

import java.util.HashMap;
import java.util.Map;

public class Item {
    private String name;
    private Map<String, Double> attributes; // attribute name -> value (0.0 - 1.0)

    public Item(String name) {
        this.name = name;
        this.attributes = new HashMap<>();
    }

    public Item(String name, Map<String, Double> attributes) {
        this.name = name;
        this.attributes = new HashMap<>(attributes);
    }

    public String getName() {
        return name;
    }

    public Map<String, Double> getAttributes() {
        return attributes;
    }

    public void setAttribute(String attrName, double value) {
        attributes.put(attrName, value);
    }

    public Double getAttributeValue(String attrName) {
        return attributes.getOrDefault(attrName, 0.0);
    }

    // Compute a combined score given attribute importance weights.
    // If an importance weight for an attribute is not provided, treat it as 1.0
    // so attribute values still contribute by default.
    public double computeScore(Map<String, Double> importanceWeights) {
        double score = 0.0;
        for (Map.Entry<String, Double> e : attributes.entrySet()) {
            String attr = e.getKey();
            double value = e.getValue();
            double weight = 1.0;
            if (importanceWeights != null) {
                weight = importanceWeights.getOrDefault(attr, 1.0);
            }
            score += value * weight;
        }
        return score;
    }

    @Override
    public String toString() {
        return "Item{name='" + name + "', attributes=" + attributes + "}";
    }
}
