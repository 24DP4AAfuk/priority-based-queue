package lv.priority.queue.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String DEFAULT_URL = "jdbc:sqlite:priority.db";
    private final String url;

    public Database() {
        this(DEFAULT_URL);
    }

    public Database(String url) {
        this.url = url;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    /**
     * Initialize database schema according to provided diagram:
     * - atributs (with rule)
     * - lietotajs
     * - objekts
     * - objekta_vertiba (with value)
     * - history
     * - system_stats
     */
    public void init() throws SQLException {
    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
        conn.setAutoCommit(false);

        // Enable FK support in SQLite
        stmt.execute("PRAGMA foreign_keys = ON");

        try {
            // Create tables if they do not exist. Do NOT drop existing tables so persisted
            // data remains intact across restarts. Schema migrations should be handled
            // separately when needed.

            // ATRIBUTS (updated with rule)
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS atributs (" +
                "atributaID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nosaukums TEXT NOT NULL UNIQUE, " +
                "koeficients REAL NOT NULL CHECK(koeficients >= 0 AND koeficients <= 1), " +
                "rule TEXT NOT NULL DEFAULT 'ASC' CHECK(rule IN ('ASC', 'DESC'))" +
                ")"
            );

            // LIETOTAJS
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS lietotajs (" +
                "lietotajaID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "lietotajvards TEXT NOT NULL UNIQUE, " +
                "parole TEXT NOT NULL, " +
                "loma TEXT NOT NULL CHECK(loma IN ('Client', 'Admin', 'Worker'))" +
                ")"
            );

            // Insert default users if not exist
            stmt.executeUpdate(
                "INSERT OR IGNORE INTO lietotajs(lietotajvards, parole, loma) VALUES('admin', 'admin', 'Admin')"
            );
            stmt.executeUpdate(
                "INSERT OR IGNORE INTO lietotajs(lietotajvards, parole, loma) VALUES('worker', 'worker', 'Worker')"
            );
            stmt.executeUpdate(
                "INSERT OR IGNORE INTO lietotajs(lietotajvards, parole, loma) VALUES('client', 'client', 'Client')"
            );

            // OBJEKTS
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS objekts (" +
                "objektaID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nosaukums TEXT NOT NULL UNIQUE, " +
                "pievienots_laiks TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "prioritates_svars REAL, " +
                "statuss TEXT DEFAULT 'pending', " +
                "FK_lietotajsID INTEGER, " +
                "FOREIGN KEY(FK_lietotajsID) REFERENCES lietotajs(lietotajaID) ON DELETE SET NULL" +
                ")"
            );

            // OBJEKTA_VERTIBA (with value)
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS objekta_vertiba (" +
                "vertibasID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "FK_objektaID INTEGER NOT NULL, " +
                "FK_atributaID INTEGER NOT NULL, " +
                "vertiba REAL NOT NULL CHECK(vertiba >= 0 AND vertiba <= 1), " +
                "FOREIGN KEY(FK_objektaID) REFERENCES objekts(objektaID) ON DELETE CASCADE, " +
                "FOREIGN KEY(FK_atributaID) REFERENCES atributs(atributaID) ON DELETE CASCADE, " +
                "UNIQUE(FK_objektaID, FK_atributaID)" +
                ")"
            );

            // HISTORY
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS history (" +
                "historyID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "objekta_nosaukums TEXT NOT NULL, " +
                "processed_time TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "processed_by TEXT" +
                ")"
            );

            // SYSTEM_STATS
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS system_stats (" +
                "statID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uptime REAL, " +
                "avg_reorder_time REAL, " +
                "last_updated TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // LAST_PROCESSED (for last 10)
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS last_processed (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "objekta_nosaukums TEXT NOT NULL, " +
                "processed_time TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Optional indexes (recommended)
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_objekta_vertiba_objekts ON objekta_vertiba(FK_objektaID)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_objekta_vertiba_atributs ON objekta_vertiba(FK_atributaID)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_history_time ON history(processed_time)"
            );

            // Migration: old DB versions could store object-attribute value in a different column.
            // Ensure objekta_vertiba always has the expected 'vertiba' column used by DAO queries.
            migrateObjektaVertibaTable(conn);

            // Migration: Add rule column to atributs if not exists
            try {
                stmt.executeUpdate("ALTER TABLE atributs ADD COLUMN rule TEXT DEFAULT 'ASC'");
            } catch (SQLException e) {
                // Ignore if column already exists
            }

            // Keep persisted scores aligned with current attribute values and rules.
            recomputeAllPriorities(conn);

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }
}

    private void migrateObjektaVertibaTable(Connection conn) throws SQLException {
        boolean hasVertiba = hasColumn(conn, "objekta_vertiba", "vertiba");
        if (!hasVertiba) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE objekta_vertiba ADD COLUMN vertiba REAL");
            }
        }

        // Compatibility with older schemas that used a different value column name.
        List<String> legacyValueColumns = findLegacyValueColumns(conn);
        for (String legacyColumn : legacyValueColumns) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "UPDATE objekta_vertiba SET vertiba = " + legacyColumn + " " +
                    "WHERE (vertiba IS NULL OR vertiba = 0) AND " + legacyColumn + " IS NOT NULL"
                );
            }
        }
    }

    private List<String> findLegacyValueColumns(Connection conn) throws SQLException {
        List<String> candidates = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(objekta_vertiba)")) {
            while (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");
                if (name == null) {
                    continue;
                }

                String normalized = name.toLowerCase();
                if ("vertiba".equals(normalized)
                        || "vertibasid".equals(normalized)
                        || "fk_objektaid".equals(normalized)
                        || "fk_atributaid".equals(normalized)) {
                    continue;
                }

                String normalizedType = type == null ? "" : type.toUpperCase();
                boolean numeric = normalizedType.contains("REAL")
                        || normalizedType.contains("NUM")
                        || normalizedType.contains("DOUB")
                        || normalizedType.contains("FLOA")
                        || normalizedType.contains("INT");

                if (numeric && (normalized.contains("vertib") || "value".equals(normalized))) {
                    candidates.add(name);
                }
            }
        }
        return candidates;
    }

    private void recomputeAllPriorities(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "UPDATE objekts SET prioritates_svars = COALESCE((" +
                "SELECT SUM(CASE WHEN a.rule = 'DESC' THEN (1.0 - ov.vertiba) ELSE ov.vertiba END * a.koeficients) " +
                "FROM objekta_vertiba ov JOIN atributs a ON a.atributaID = ov.FK_atributaID " +
                "WHERE ov.FK_objektaID = objekts.objektaID), 0)"
            );
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    // Simple demo to create tables
    public static void main(String[] args) {
        Database db = new Database();
        try {
            db.init();
            System.out.println("Database initialized (priority.db)");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}