package lv.priority.queue.db;

import lv.priority.queue.Item;
import lv.priority.queue.Queue;
import lv.priority.queue.Attribute;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

// data access object that keeps queue state and SQLite persistence in sync.
public class DatabaseDAO {
    private final Database db;

    public DatabaseDAO(Database db) {
        this.db = db;
    }

    // Loads all attributes and objects from DB into the in-memory queue.
    public void loadAll(Queue queue) throws SQLException {
        try (Connection conn = db.getConnection()) {
            // load attribute definitions (atributs: nosaukums -> Attribute)
            Map<String, Attribute> attrs = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT nosaukums, koeficients, rule FROM atributs");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attrs.put(rs.getString(1), new Attribute(rs.getString(1), rs.getDouble(2), rs.getString(3)));
                }
            }
            queue.setAttributes(attrs);

            // load objekts and their attribute values
            try (PreparedStatement ps = conn.prepareStatement("SELECT objektaID, nosaukums FROM objekts");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    String name = rs.getString(2);
                    Item it = new Item(name);
                    try (PreparedStatement ps2 = conn.prepareStatement(
                                    "SELECT a.nosaukums, ov.vertiba FROM objekta_vertiba ov JOIN atributs a ON ov.FK_atributaID = a.atributaID WHERE ov.FK_objektaID = ?")) {
                        ps2.setInt(1, id);
                        try (ResultSet rs2 = ps2.executeQuery()) {
                            while (rs2.next()) {
                                it.setAttribute(rs2.getString(1), rs2.getDouble(2));
                            }
                        }
                    }
                    queue.addItem(it);
                }
            }
        }
    }

    // Creates the object row if it does not already exist.
    public void addItem(String name) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO objekts(nosaukums, prioritates_svars) VALUES(?, 0)") ) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    // Ensures item/attribute relationship exists with value and refreshes stored score.
    public void saveAttribute(String itemName, String attrName, double value) throws SQLException {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            Integer objId = getObjectId(conn, itemName);
            if (objId == null) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO objekts(nosaukums) VALUES(?)")) {
                    ps.setString(1, itemName);
                    ps.executeUpdate();
                }
                objId = getObjectId(conn, itemName);
            }

            Integer attrId = getAttributeId(conn, attrName);
            if (attrId == null) {
                throw new SQLException("Attribute not found: " + attrName);
            }

            // upsert the value
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO objekta_vertiba(FK_objektaID, FK_atributaID, vertiba) VALUES(?, ?, ?)")) {
                ps.setInt(1, objId);
                ps.setInt(2, attrId);
                ps.setDouble(3, value);
                ps.executeUpdate();
            }

            // recompute and persist priority for this object
            double newScore = computeScore(conn, objId);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE objekts SET prioritates_svars = ? WHERE objektaID = ?")) {
                ps.setDouble(1, newScore);
                ps.setInt(2, objId);
                ps.executeUpdate();
            }

            conn.commit();
        }
    }

    private double computeScore(Connection conn, int objId) throws SQLException {
        double score = 0.0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ov.vertiba, a.koeficients, a.rule FROM objekta_vertiba ov JOIN atributs a ON a.atributaID = ov.FK_atributaID WHERE ov.FK_objektaID = ?")) {
            ps.setInt(1, objId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double value = rs.getDouble(1);
                    double weight = rs.getDouble(2);
                    String rule = rs.getString(3);
                    if ("DESC".equals(rule)) {
                        value = 1.0 - value;
                    }
                    score += value * weight;
                }
            }
        }
        return score;
    }

    // Upserts attribute definition and recomputes all object priorities in one pass.
    public void setAttribute(String attrName, double weight, String rule) throws SQLException {
        try (Connection conn = db.getConnection()) {
            Integer aid = getAttributeId(conn, attrName);
            if (aid == null) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO atributs(nosaukums, koeficients, rule) VALUES(?, ?, ?)") ) {
                    ps.setString(1, attrName);
                    ps.setDouble(2, weight);
                    ps.setString(3, rule);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE atributs SET koeficients = ?, rule = ? WHERE atributaID = ?")) {
                    ps.setDouble(1, weight);
                    ps.setString(2, rule);
                    ps.setInt(3, aid);
                    ps.executeUpdate();
                }
            }
            // Recompute priorities for all objects based on updated attributes
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE objekts SET prioritates_svars = COALESCE((" +
                    "SELECT SUM(CASE WHEN a.rule = 'DESC' THEN (1.0 - ov.vertiba) ELSE ov.vertiba END * a.koeficients) " +
                    "FROM objekta_vertiba ov JOIN atributs a ON a.atributaID = ov.FK_atributaID " +
                    "WHERE ov.FK_objektaID = objekts.objektaID), 0)")) {
                ps.executeUpdate();
            }
        }
    }

    // Removes an object and its attribute mappings.
    public void removeItem(String name) throws SQLException {
        try (Connection conn = db.getConnection()) {
            Integer id = getObjectId(conn, name);
            if (id == null) return;
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM objekta_vertiba WHERE FK_objektaID = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM objekts WHERE objektaID = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    // Returns object PK by logical name, or null if missing.
    private Integer getObjectId(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT objektaID FROM objekts WHERE nosaukums = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return null;
    }

    // Returns attribute PK by logical name, or null if missing.
    private Integer getAttributeId(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT atributaID FROM atributs WHERE nosaukums = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return null;
    }

    // Lists all attribute definitions (name -> Attribute).
    public Map<String, Attribute> listAttributes() throws SQLException {
        Map<String, Attribute> out = new HashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nosaukums, koeficients, rule FROM atributs");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.put(rs.getString(1), new Attribute(rs.getString(1), rs.getDouble(2), rs.getString(3)));
        }
        return out;
    }

    // Returns all objects with their assigned attributes.
    public Map<String, Item> getAllItems() throws SQLException {
        Map<String, Item> out = new HashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT objektaID, nosaukums FROM objekts");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                Item it = new Item(name);
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "SELECT a.nosaukums, ov.vertiba FROM objekta_vertiba ov JOIN atributs a ON ov.FK_atributaID = a.atributaID WHERE ov.FK_objektaID = ?")) {
                    ps2.setInt(1, id);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        while (rs2.next()) {
                            it.setAttribute(rs2.getString(1), rs2.getDouble(2));
                        }
                    }
                }
                out.put(name, it);
            }
        }
        return out;
    }

    // Returns persisted scores keyed by object name.
    public Map<String, Double> getStoredScores() throws SQLException {
        Map<String, Double> out = new HashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nosaukums, COALESCE(prioritates_svars, 0) FROM objekts");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.put(rs.getString(1), rs.getDouble(2));
            }
        }
        return out;
    }

    // Returns a single object with attributes, or null when not found.
    public Item getItem(String name) throws SQLException {
        try (Connection conn = db.getConnection()) {
            Integer id = getObjectId(conn, name);
            if (id == null) return null;
            Item it = new Item(name);
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT a.nosaukums, ov.vertiba FROM objekta_vertiba ov JOIN atributs a ON ov.FK_atributaID = a.atributaID WHERE ov.FK_objektaID = ?")) {
                ps2.setInt(1, id);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        it.setAttribute(rs2.getString(1), rs2.getDouble(2));
                    }
                }
            }
            return it;
        }
    }

    // User authentication
    public String authenticate(String username, String password) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT loma FROM lietotajs WHERE lietotajvards = ? AND parole = ?")) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    // Add user
    public void addUser(String username, String password, String role) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO lietotajs(lietotajvards, parole, loma) VALUES(?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    // Add to history
    public void addToHistory(String itemName, String processedBy) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO history(objekta_nosaukums, processed_by) VALUES(?, ?)")) {
            ps.setString(1, itemName);
            ps.setString(2, processedBy);
            ps.executeUpdate();
        }
        // Also add to last_processed and keep only last 10
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO last_processed(objekta_nosaukums) VALUES(?)")) {
                ps.setString(1, itemName);
                ps.executeUpdate();
            }
            // Keep only last 10
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM last_processed WHERE id NOT IN (SELECT id FROM last_processed ORDER BY processed_time DESC LIMIT 10)")) {
                ps.executeUpdate();
            }
            conn.commit();
        }
    }

    // Get history
    public List<String> getHistory() throws SQLException {
        List<String> history = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT objekta_nosaukums, processed_time, processed_by FROM history ORDER BY processed_time DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                history.add(rs.getString(1) + " processed at " + rs.getString(2) + " by " + rs.getString(3));
            }
        }
        return history;
    }

    // Get last 10 processed
    public List<String> getLastProcessed() throws SQLException {
        List<String> last = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT objekta_nosaukums, processed_time FROM last_processed ORDER BY processed_time DESC LIMIT 10");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                last.add(rs.getString(1) + " at " + rs.getString(2));
            }
        }
        return last;
    }

    // Update system stats
    public void updateStats(double uptime, double avgReorderTime) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO system_stats(statID, uptime, avg_reorder_time) VALUES(1, ?, ?)")) {
            ps.setDouble(1, uptime);
            ps.setDouble(2, avgReorderTime);
            ps.executeUpdate();
        }
    }

    // Get system stats
    public double[] getStats() throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uptime, avg_reorder_time FROM system_stats WHERE statID = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new double[]{rs.getDouble(1), rs.getDouble(2)};
            }
        }
        return new double[]{0.0, 0.0};
    }

    // Search by ID or name
    public List<String> searchItems(String query) throws SQLException {
        List<String> results = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nosaukums FROM objekts WHERE objektaID = ? OR nosaukums LIKE ?")) {
            try {
                ps.setInt(1, Integer.parseInt(query));
            } catch (NumberFormatException e) {
                ps.setInt(1, -1);
            }
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getString(1));
                }
            }
        }
        return results;
    }
}
