package serverrest.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestisce la connessione JDBC al database.
 * Implementa il pattern Singleton: esiste una sola connessione per tutta l'applicazione.
 * Alla prima istanziazione crea automaticamente lo schema (tabella operazioni).
 *
 * Supporta due database:
 *   - SQLite  (embedded, nessun server richiesto)
 *   - MySQL   (server esterno)
 *
 * Per cambiare database basta modificare il campo DB_SELEZIONATO.
 *
 * @author delfo
 */
public class DatabaseManager {

    // ══════════════════════════════════════════════════════════════
    //  CONFIGURAZIONE — variabili d'ambiente con fallback ai default
    // ══════════════════════════════════════════════════════════════

    /** Legge DB_TYPE dall'ambiente (SQLITE o MYSQL). Default: SQLITE. */
    private static final DbType DB_SELEZIONATO = resolveDbType();

    // -- Parametri SQLite ------------------------------------------------
    private static final String SQLITE_URL = "jdbc:sqlite:calcolatrice.db";

    // -- Parametri MySQL — sovrascrivibili via variabili d'ambiente ------
    private static final String MYSQL_HOST     = envOr("MYSQL_HOST",     "localhost");
    private static final int    MYSQL_PORT     = Integer.parseInt(envOr("MYSQL_PORT", "3306"));
    private static final String MYSQL_DATABASE = envOr("MYSQL_DATABASE", "calcolatrice_db");
    private static final String MYSQL_USER     = envOr("MYSQL_USER",     "serverrest");
    private static final String MYSQL_PASSWORD = envOr("MYSQL_PASSWORD", "calcolatrice");

    /** URL JDBC completo per MySQL */
    private static final String MYSQL_URL =
        "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/" + MYSQL_DATABASE
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    // ══════════════════════════════════════════════════════════════
    //  Enum per la selezione del database
    // ══════════════════════════════════════════════════════════════

    public enum DbType { SQLITE, MYSQL }

    // ══════════════════════════════════════════════════════════════
    //  Implementazione Singleton
    // ══════════════════════════════════════════════════════════════

    private static DatabaseManager instance;
    private Connection connection;

    /** Costruttore privato: apre la connessione e inizializza lo schema */
    private DatabaseManager() throws SQLException {
        connection = creaConnessione();
        initSchema();
        System.out.println("[DB] Connessione aperta (" + DB_SELEZIONATO + ")");
    }

    /**
     * Restituisce l'unica istanza del DatabaseManager.
     * La crea al primo accesso o se la connessione risulta chiusa.
     * "synchronized" garantisce la sicurezza in ambienti multi-thread.
     */
    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /** Restituisce la connessione JDBC attiva */
    public Connection getConnection() {
        return connection;
    }

    /** Chiude la connessione al database */
    public void chiudi() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connessione chiusa.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore nella chiusura: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Metodi privati
    // ══════════════════════════════════════════════════════════════

    private static DbType resolveDbType() {
        return "MYSQL".equalsIgnoreCase(System.getenv("DB_TYPE")) ? DbType.MYSQL : DbType.SQLITE;
    }

    private static String envOr(String name, String def) {
        String v = System.getenv(name);
        return (v != null && !v.isBlank()) ? v : def;
    }

    /** Crea la connessione al database selezionato */
    private static Connection creaConnessione() throws SQLException {
        switch (DB_SELEZIONATO) {
            case MYSQL:
                return DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
            case SQLITE:
            default:
                return DriverManager.getConnection(SQLITE_URL);
        }
    }

    /**
     * Crea la tabella se non esiste ancora (istruzione idempotente).
     * Usa la sintassi DDL appropriata per il database selezionato.
     */
    private void initSchema() throws SQLException {
        String sql;
        switch (DB_SELEZIONATO) {
            case MYSQL:
                sql = "CREATE TABLE IF NOT EXISTS operazioni ("
                    + "  id        INT         PRIMARY KEY AUTO_INCREMENT,"
                    + "  operando1 DOUBLE      NOT NULL,"
                    + "  operando2 DOUBLE      NOT NULL,"
                    + "  operatore VARCHAR(20) NOT NULL,"
                    + "  risultato DOUBLE      NOT NULL,"
                    + "  timestamp DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                break;
            case SQLITE:
            default:
                sql = "CREATE TABLE IF NOT EXISTS operazioni ("
                    + "  id        INTEGER     PRIMARY KEY AUTOINCREMENT,"
                    + "  operando1 REAL        NOT NULL,"
                    + "  operando2 REAL        NOT NULL,"
                    + "  operatore VARCHAR(20) NOT NULL,"
                    + "  risultato REAL        NOT NULL,"
                    + "  timestamp DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP"
                    + ")";
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
}
