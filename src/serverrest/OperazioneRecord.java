package serverrest;

/**
 * Record di persistenza di un'operazione della calcolatrice.
 *
 * Implementa la composizione con {@link OperazioneResponse}: invece di
 * duplicarne i campi, contiene un'istanza della classe come attributo.
 * Gli unici attributi propri di questa classe sono quelli legati alla
 * persistenza: {@code id} (chiave primaria del DB) e {@code timestamp}
 * (marca temporale generata automaticamente dal database).
 *
 * Il JSON prodotto da GSON rifletterà la struttura composta:
 * <pre>
 * {
 *   "id": 1,
 *   "timestamp": "2024-01-01 10:00:00",
 *   "risposta": {
 *     "operando1": 10.0,
 *     "operando2": 5.0,
 *     "operatore": "SOMMA",
 *     "risultato": 15.0,
 *     "operazione": "10,00 SOMMA 5,00 = 15,00"
 *   }
 * }
 * </pre>
 *
 * @author delfo
 */
public class OperazioneRecord {

    // --- Attributi propri (persistenza) ---
    private int    id;
    private String timestamp;

    // --- Composizione: dati di calcolo delegati a OperazioneResponse ---
    private OperazioneResponse risposta;

    // Costruttore vuoto richiesto da GSON
    public OperazioneRecord() { }

    /**
     * @param id        chiave primaria generata dal database
     * @param timestamp marca temporale generata dal database
     * @param risposta  oggetto di calcolo composto (operandi, operatore, risultato)
     */
    public OperazioneRecord(int id, String timestamp, OperazioneResponse risposta) {
        this.id        = id;
        this.timestamp = timestamp;
        this.risposta  = risposta;
    }

    // --- Getter/Setter degli attributi propri ---

    public int    getId()        { return id; }
    public void   setId(int id)  { this.id = id; }

    public String getTimestamp()          { return timestamp; }
    public void   setTimestamp(String v)  { this.timestamp = v; }

    // --- Getter/Setter dell'oggetto composto ---

    public OperazioneResponse getRisposta()                    { return risposta; }
    public void               setRisposta(OperazioneResponse r){ this.risposta = r; }

    @Override
    public String toString() {
        return "OperazioneRecord{id=" + id +
               ", timestamp='" + timestamp + '\'' +
               ", risposta=" + risposta + '}';
    }
}
