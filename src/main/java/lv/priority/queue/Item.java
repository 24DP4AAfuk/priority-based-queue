package lv.priority.queue;

import java.util.HashMap;
import java.util.Map;

// Represents one queue entity and its assigned attribute values.
public class Item {
    private String name;
    private Map<String, Double> attributes; // attribute name -> value

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

    public boolean hasAttribute(String attrName) {
        return attributes.containsKey(attrName);
    }

    public double getAttributeValue(String attrName) {
        return attributes.getOrDefault(attrName, 0.0);
    }

    // Compute a combined score given attribute importance weights and rules.
    // For each attribute, score contribution depends on rule: ASC (higher value better), DESC (lower value better)
    public double computeScore(Map<String, Attribute> attributeDefs) {
        double score = 0.0;
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            String attr = entry.getKey();
            double value = entry.getValue();
            Attribute def = attributeDefs.get(attr);
            if (def != null) {
                double weight = def.getWeight();
                if ("DESC".equals(def.getRule())) {
                    // For DESC, invert the value (assuming values are 0-1, lower is better)
                    value = 1.0 - value;
                }
                score += value * weight;
            } else {
                // Default: ASC with weight 1.0
                score += value * 1.0;
            }
        }
        return score;
    }

    @Override
    // Helpful for debugging and logs.
    public String toString() {
        return "Item{name='" + name + "', attributes=" + attributes + "}";
    }
}
