package lv.priority.queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Comparator;

public class Queue {
    // attribute name -> importance weight (0.0 - 1.0)
    private Map<String, Double> importanceWeights = new HashMap<>();

    // map of items for quick lookup
    private Map<String, Item> itemsByName = new HashMap<>();

    // priority queue ordered by computed score (highest first)
    private PriorityQueue<ItemWrapper> pq = new PriorityQueue<>(
        Comparator.comparingDouble(ItemWrapper::getScore).reversed()
    );

    private static class ItemWrapper {
        Item item;
        double score;

        ItemWrapper(Item item, double score) {
            this.item = item;
            this.score = score;
        }

        double getScore() {
            return score;
        }
    }

    public Queue() {
    }

    public void setImportance(String attribute, double weight) {
        if (weight < 0.0) weight = 0.0;
        if (weight > 1.0) weight = 1.0;
        importanceWeights.put(attribute, weight);
        rebuildQueue();
    }

    public void setImportances(Map<String, Double> weights) {
        importanceWeights.clear();
        importanceWeights.putAll(weights);
        rebuildQueue();
    }

    public void addItem(Item item) {
        itemsByName.put(item.getName(), item);
        pq.add(new ItemWrapper(item, item.computeScore(importanceWeights)));
    }

    public Item poll() {
        ItemWrapper w = pq.poll();
        if (w == null) return null;
        itemsByName.remove(w.item.getName());
        return w.item;
    }

    public Item peek() {
        ItemWrapper w = pq.peek();
        return w == null ? null : w.item;
    }

    public Item getItem(String name) {
        return itemsByName.get(name);
    }

    public void removeItem(String name) {
        Item it = itemsByName.remove(name);
        if (it != null) rebuildQueue();
    }

    public void updateItemAttribute(String itemName, String attribute) {
        Item it = itemsByName.get(itemName);
        if (it != null) {
            it.setAttribute(attribute);
            rebuildQueue();
        }
    }

    // Rebuild the priority queue from current items and importance weights
    private void rebuildQueue() {
        pq.clear();
        for (Item item : itemsByName.values()) {
            pq.add(new ItemWrapper(item, item.computeScore(importanceWeights)));
        }
    }

    // Return ordered snapshot (highest score first)
    public List<Item> orderedSnapshot() {
        List<ItemWrapper> list = new ArrayList<>(pq);
        list.sort(Comparator.comparingDouble(ItemWrapper::getScore).reversed());
        List<Item> out = new ArrayList<>();
        for (ItemWrapper w : list) out.add(w.item);
        return out;
    }

    // Expose a copy of current importance weights
    public Map<String, Double> getImportanceWeights() {
        return new HashMap<>(importanceWeights);
    }

    // Compute score for a named item (or NaN if not present)
    public double getScoreForItem(String name) {
        Item it = itemsByName.get(name);
        if (it == null) return Double.NaN;
        return it.computeScore(importanceWeights);
    }
}
