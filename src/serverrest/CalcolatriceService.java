package serverrest;

import serverrest.db.OperazioneRepository;
import java.sql.SQLException;

/**
 * Service della calcolatrice: esegue il calcolo matematico e delega
 * la persistenza al repository.
 * Separazione dei compiti: la logica di business (calcolo) è qui;
 * la logica di accesso ai dati è in {@link serverrest.db.OperazioneRepository}.
 *
 * @author delfo
 */
public class CalcolatriceService {

    private final OperazioneRepository repository;

    /**
     * @param repository implementazione del repository iniettata dall'esterno
     *                   (Dependency Injection manuale)
     */
    public CalcolatriceService(OperazioneRepository repository) {
        this.repository = repository;
    }

    /**
     * Esegue l'operazione, salva il risultato nel DB e restituisce il record
     * completo (con id e timestamp generati dal database).
     *
     * @param operando1 primo operando
     * @param operando2 secondo operando
     * @param operatore operatore (SOMMA, SOTTRAZIONE, MOLTIPLICAZIONE, DIVISIONE o simbolo)
     * @return il record persistito
     * @throws IllegalArgumentException se l'operatore non è valido o divisione per zero
     * @throws SQLException             se la persistenza fallisce
     */
    public OperazioneRecord calcola(double operando1, double operando2, String operatore)
            throws IllegalArgumentException, SQLException {

        double risultato = eseguiCalcolo(operando1, operando2, operatore);
        return repository.salva(operando1, operando2, operatore, risultato);
    }

    // ------------------------------------------------------------------
    // Logica matematica pura (private, testabile in isolamento)
    // ------------------------------------------------------------------

    private static double eseguiCalcolo(double operando1, double operando2, String operatore)
            throws IllegalArgumentException {

        if (operatore == null || operatore.trim().isEmpty()) {
            throw new IllegalArgumentException("Operatore non può essere vuoto");
        }

        String op = operatore.toUpperCase().trim();

        switch (op) {
            case "SOMMA":
            case "+":
                return operando1 + operando2;

            case "SOTTRAZIONE":
            case "-":
                return operando1 - operando2;

            case "MOLTIPLICAZIONE":
            case "*":
            case "X":
                return operando1 * operando2;

            case "DIVISIONE":
            case "/":
                if (operando2 == 0) {
                    throw new IllegalArgumentException("Divisione per zero non consentita");
                }
                return operando1 / operando2;

            default:
                throw new IllegalArgumentException(
                    "Operatore non valido: " + operatore +
                    ". Operatori consentiti: SOMMA, SOTTRAZIONE, MOLTIPLICAZIONE, DIVISIONE"
                );
        }
    }
}
