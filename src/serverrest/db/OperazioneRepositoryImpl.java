package serverrest.db;

import serverrest.OperazioneRecord;
import serverrest.OperazioneResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementazione JDBC del repository delle operazioni.
 * Tutta la logica SQL è confinata in questa classe; il resto dell'applicazione
 * dipende solo dall'interfaccia {@link OperazioneRepository}.
 *
 * @author delfo
 */
public class OperazioneRepositoryImpl implements OperazioneRepository {

    private final DatabaseManager dbManager;

    public OperazioneRepositoryImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // ------------------------------------------------------------------
    // salva
    // ------------------------------------------------------------------

    @Override
    public OperazioneRecord salva(double operando1, double operando2,
                                  String operatore, double risultato) throws SQLException {

        String sql = "INSERT INTO operazioni (operando1, operando2, operatore, risultato) " +
                     "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = dbManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setDouble(1, operando1);
            stmt.setDouble(2, operando2);
            stmt.setString(3, operatore);
            stmt.setDouble(4, risultato);
            stmt.executeUpdate();

            // Recupera l'id auto-generato e rilege il record completo (con timestamp)
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int nuovoId = keys.getInt(1);
                    return trovaPerID(nuovoId)
                           .orElseThrow(() -> new SQLException("Salvataggio fallito: record non trovato dopo INSERT"));
                }
                throw new SQLException("Salvataggio fallito: nessun ID generato");
            }
        }
    }

    // ------------------------------------------------------------------
    // trovaTutte
    // ------------------------------------------------------------------

    @Override
    public List<OperazioneRecord> trovaTutte() throws SQLException {

        String sql = "SELECT id, operando1, operando2, operatore, risultato, timestamp " +
                     "FROM operazioni ORDER BY id DESC";

        List<OperazioneRecord> lista = new ArrayList<>();

        try (Statement stmt   = dbManager.getConnection().createStatement();
             ResultSet rs     = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(mapRow(rs));
            }
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // trovaPerID
    // ------------------------------------------------------------------

    @Override
    public Optional<OperazioneRecord> trovaPerID(int id) throws SQLException {

        String sql = "SELECT id, operando1, operando2, operatore, risultato, timestamp " +
                     "FROM operazioni WHERE id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // Helper: mappa una riga del ResultSet in OperazioneRecord
    // ------------------------------------------------------------------

    private OperazioneRecord mapRow(ResultSet rs) throws SQLException {
        OperazioneResponse risposta = new OperazioneResponse(
            rs.getDouble("operando1"),
            rs.getDouble("operando2"),
            rs.getString("operatore"),
            rs.getDouble("risultato")
        );
        return new OperazioneRecord(
            rs.getInt("id"),
            rs.getString("timestamp"),
            risposta
        );
    }
}
