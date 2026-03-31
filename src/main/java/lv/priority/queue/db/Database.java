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
     * Initialize database schema according to provided diagram:
     * - atributs
     * - lietotajs
     * - objekts
     * - objekta_vertiba
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

            // ATRIBUTS
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS atributs (" +
                "atributaID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nosaukums TEXT NOT NULL, " +
                "koeficients REAL NOT NULL CHECK(koeficients >= 0 AND koeficients <= 1)" +
                ")"
            );

            // LIETOTAJS
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS lietotajs (" +
                "lietotajaID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "lietotajvards TEXT NOT NULL UNIQUE, " +
                "parole TEXT NOT NULL, " +
                "loma TEXT NOT NULL" +
                ")"
            );

            // OBJEKTS
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS objekts (" +
                "objektaID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nosaukums TEXT NOT NULL, " +
                "pievienots_laiks TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "prioritates_svars REAL, " +
                "statuss TEXT, " +
                "FK_lietotajsID INTEGER, " +
                "FOREIGN KEY(FK_lietotajsID) REFERENCES lietotajs(lietotajaID) ON DELETE SET NULL" +
                ")"
            );

            // OBJEKTA_VERTIBA (presence-only mapping: FK_objektaID <-> FK_atributaID)
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS objekta_vertiba (" +
                "vertibasID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "FK_objektaID INTEGER NOT NULL, " +
                "FK_atributaID INTEGER NOT NULL, " +
                "FOREIGN KEY(FK_objektaID) REFERENCES objekts(objektaID) ON DELETE CASCADE, " +
                "FOREIGN KEY(FK_atributaID) REFERENCES atributs(atributaID) ON DELETE CASCADE" +
                ")"
            );

            // Optional indexes (recommended)
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_objekta_vertiba_objekts ON objekta_vertiba(FK_objektaID)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_objekta_vertiba_atributs ON objekta_vertiba(FK_atributaID)"
            );

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