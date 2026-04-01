package lv.priority.queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Comparator;

public class Queue {
    // attribute name -> Attribute object
    private Map<String, Attribute> attributes = new HashMap<>();

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

    public void setAttribute(Attribute attr) {
        attributes.put(attr.getName(), attr);
        rebuildQueue();
    }

    public void setAttributes(Map<String, Attribute> attrs) {
        attributes.clear();
        attributes.putAll(attrs);
        rebuildQueue();
    }

    public void addItem(Item item) {
        itemsByName.put(item.getName(), item);
        pq.add(new ItemWrapper(item, item.computeScore(attributes)));
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

    public void updateItemAttribute(String itemName, String attribute, double value) {
        Item it = itemsByName.get(itemName);
        if (it != null) {
            it.setAttribute(attribute, value);
            rebuildQueue();
        }
    }

    // Rebuild the priority queue from current items and attributes
    private void rebuildQueue() {
        pq.clear();
        for (Item item : itemsByName.values()) {
            pq.add(new ItemWrapper(item, item.computeScore(attributes)));
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

    // Expose a copy of current attributes
    public Map<String, Attribute> getAttributes() {
        return new HashMap<>(attributes);
    }

    // Compute score for a named item (or NaN if not present)
    public double getScoreForItem(String name) {
        Item it = itemsByName.get(name);
        if (it == null) return Double.NaN;
        return it.computeScore(attributes);
    }
}
