package lv.priority.queue.db;

import lv.priority.queue.Item;
import lv.priority.queue.Queue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseDAO {
    private final Database db;

    public DatabaseDAO(Database db) {
        this.db = db;
    }

    public void loadAll(Queue queue) throws SQLException {
        try (Connection conn = db.getConnection()) {
            // load attribute definitions (atributs: nosaukums -> koeficients)
            Map<String, Double> imps = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT nosaukums, koeficients FROM atributs");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    imps.put(rs.getString(1), rs.getDouble(2));
                }
            }
            queue.setImportances(imps);

            // load objekts and their attribute values
            try (PreparedStatement ps = conn.prepareStatement("SELECT objektaID, nosaukums FROM objekts");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    String name = rs.getString(2);
                    Item it = new Item(name);
                    try (PreparedStatement ps2 = conn.prepareStatement(
                                    "SELECT a.nosaukums FROM objekta_vertiba ov JOIN atributs a ON ov.FK_atributaID = a.atributaID WHERE ov.FK_objektaID = ?")) {
                        ps2.setInt(1, id);
                        try (ResultSet rs2 = ps2.executeQuery()) {
                            while (rs2.next()) {
                                        it.setAttribute(rs2.getString(1));
                            }
                        }
                    }
                    queue.addItem(it);
                }
            }
        }
    }

    public void addItem(String name) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO objekts(nosaukums, prioritates_svars) VALUES(?, 0)") ) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    public void saveAttribute(String itemName, String attrName) throws SQLException {
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

            // check existing value
            // if mapping already exists, nothing to update (presence-only)
            try (PreparedStatement ps = conn.prepareStatement("SELECT vertibasID FROM objekta_vertiba WHERE FK_objektaID = ? AND FK_atributaID = ?")) {
                ps.setInt(1, objId);
                ps.setInt(2, attrId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        conn.commit();
                        return;
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO objekta_vertiba(FK_objektaID, FK_atributaID) VALUES(?, ?)") ) {
                ps.setInt(1, objId);
                ps.setInt(2, attrId);
                ps.executeUpdate();
            }

            // recompute and persist priority for this object
            double newScore = 0.0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(a.koeficients) FROM objekta_vertiba ov JOIN atributs a ON a.atributaID = ov.FK_atributaID WHERE ov.FK_objektaID = ?")) {
                ps.setInt(1, objId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) newScore = rs.getDouble(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE objekts SET prioritates_svars = ? WHERE objektaID = ?")) {
                ps.setDouble(1, newScore);
                ps.setInt(2, objId);
                ps.executeUpdate();
            }

            conn.commit();
        }
    }

    public void setImportance(String attrName, double weight) throws SQLException {
        try (Connection conn = db.getConnection()) {
            Integer aid = getAttributeId(conn, attrName);
            if (aid == null) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO atributs(nosaukums, koeficients) VALUES(?, ?)") ) {
                    ps.setString(1, attrName);
                    ps.setDouble(2, weight);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE atributs SET koeficients = ? WHERE atributaID = ?")) {
                    ps.setDouble(1, weight);
                    ps.setInt(2, aid);
                    ps.executeUpdate();
                }
            }
            // Recompute priorities for all objects based on updated coefficients
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE objekts SET prioritates_svars = COALESCE((SELECT SUM(a.koeficients) FROM objekta_vertiba ov JOIN atributs a ON a.atributaID = ov.FK_atributaID WHERE ov.FK_objektaID = objekts.objektaID), 0)") ) {
                ps.executeUpdate();
            }
        }
    }

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

    private Integer getObjectId(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT objektaID FROM objekts WHERE nosaukums = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return null;
    }

    private Integer getAttributeId(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT atributaID FROM atributs WHERE nosaukums = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return null;
    }

    public Map<String, Double> listAttributes() throws SQLException {
        Map<String, Double> out = new HashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nosaukums, koeficients FROM atributs");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.put(rs.getString(1), rs.getDouble(2));
        }
        return out;
    }

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
                        "SELECT a.nosaukums FROM objekta_vertiba ov JOIN atributs a ON ov.FK_atributaID = a.atributaID WHERE ov.FK_objektaID = ?")) {
                    ps2.setInt(1, id);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        while (rs2.next()) {
                            it.setAttribute(rs2.getString(1));
                        }
                    }
                }
                out.put(name, it);
            }
        }
        return out;
    }

    public Item getItem(String name) throws SQLException {
        try (Connection conn = db.getConnection()) {
            Integer id = getObjectId(conn, name);
            if (id == null) return null;
            Item it = new Item(name);
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT a.nosaukums FROM objekta_vertiba ov JOIN atributs a ON ov.FK_atributaID = a.atributaID WHERE ov.FK_objektaID = ?")) {
                ps2.setInt(1, id);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        it.setAttribute(rs2.getString(1));
                    }
                }
            }
            return it;
        }
    }
}
