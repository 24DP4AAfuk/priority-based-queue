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

            stmt.executeUpdate("DROP TABLE IF EXISTS attributes");
            stmt.executeUpdate("DROP TABLE IF EXISTS items");
            stmt.executeUpdate("DROP TABLE IF EXISTS importances");

            // ATRIBUTS
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS atributs (" +
                "atributaID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nosaukums VARCHAR(50) NOT NULL, " +
                "koeficients FLOAT NOT NULL CHECK(koeficients >= 0 AND koeficients <= 1)" +
                ")"
            );

            // LIETOTAJS
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS lietotajs (" +
                "lietotajaID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "lietotajvards VARCHAR(30) NOT NULL UNIQUE, " +
                "parole VARCHAR(64) NOT NULL, " +
                "loma VARCHAR(20) NOT NULL" +
                ")"
            );

            // OBJEKTS
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS objekts (" +
                "objektaID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nosaukums VARCHAR(100) NOT NULL, " +
                "pievienots_laiks DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "prioritates_svars FLOAT, " +
                "statuss VARCHAR(20)" +
                ")"
            );

            // OBJEKTA_VERTIBA
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS objekta_vertiba (" +
                "vertibasID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "objektaID INTEGER NOT NULL, " +
                "atributaID INTEGER NOT NULL, " +
                "vertiba FLOAT NOT NULL, " +
                "FOREIGN KEY(objektaID) REFERENCES objekts(objektaID) ON DELETE CASCADE, " +
                "FOREIGN KEY(atributaID) REFERENCES atributs(atributaID) ON DELETE CASCADE" +
                ")"
            );

            conn.commit();
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