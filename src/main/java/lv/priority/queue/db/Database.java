package lv.priority.queue.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
     * Initialize database schema according to diagram:
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

            // Migration: Add rule column to atributs if not exists
            try {
                stmt.executeUpdate("ALTER TABLE atributs ADD COLUMN rule TEXT DEFAULT 'ASC'");
            } catch (SQLException e) {
                // Ignore if column already exists
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }
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