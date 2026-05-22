package serverrest.db;

import serverrest.OperazioneRecord;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interfaccia Repository per la persistenza delle operazioni della calcolatrice.
 * Definisce il contratto CRUD senza esporre dettagli di implementazione (JDBC, SQL, ecc.).
 *
 * @author delfo
 */
public interface OperazioneRepository {

    /**
     * Salva una nuova operazione nel database e restituisce il record persistito
     * (comprensivo di id e timestamp generati dal DB).
     */
    OperazioneRecord salva(double operando1, double operando2,
                           String operatore, double risultato) throws SQLException;

    /**
     * Restituisce tutte le operazioni ordinate dalla più recente.
     */
    List<OperazioneRecord> trovaTutte() throws SQLException;

    /**
     * Cerca un'operazione per id primario.
     * Restituisce Optional.empty() se non trovata.
     */
    Optional<OperazioneRecord> trovaPerID(int id) throws SQLException;
}
