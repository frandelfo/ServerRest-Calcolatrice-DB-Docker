package serverrest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import serverrest.db.OperazioneRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handler per i due endpoint di consultazione dello storico:
 *
 *   GET /api/operazioni          → lista di tutte le operazioni (ordine cronologico inverso)
 *   GET /api/operazioni/{id}     → singola operazione per id
 *
 * Il routing tra i due casi viene effettuato analizzando il path della richiesta.
 *
 * @author delfo
 */
public class OperazioniHandler implements HttpHandler {

    private static final String BASE_PATH = "/api/operazioni";

    private final OperazioneRepository repository;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public OperazioniHandler(OperazioneRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            inviaErrore(exchange, 405, "Metodo non consentito. Usa GET");
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            if (path.equals(BASE_PATH) || path.equals(BASE_PATH + "/")) {
                // --- GET /api/operazioni ---
                gestisciLista(exchange);
            } else {
                // --- GET /api/operazioni/{id} ---
                gestisciPerID(exchange, path);
            }
        } catch (SQLException e) {
            inviaErrore(exchange, 500, "Errore database: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Lista tutte le operazioni
    // ------------------------------------------------------------------

    private void gestisciLista(HttpExchange exchange) throws IOException, SQLException {
        List<OperazioneRecord> lista = repository.trovaTutte();

        Map<String, Object> risposta = new HashMap<>();
        risposta.put("totale", lista.size());
        risposta.put("operazioni", lista);

        inviaRisposta(exchange, 200, gson.toJson(risposta));
    }

    // ------------------------------------------------------------------
    // Singola operazione per id
    // ------------------------------------------------------------------

    private void gestisciPerID(HttpExchange exchange, String path) throws IOException, SQLException {

        // Estrae l'id dal path: "/api/operazioni/42" → "42"
        String segmento = path.substring(BASE_PATH.length()).replaceAll("^/+", "");

        if (segmento.isEmpty()) {
            gestisciLista(exchange);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(segmento);
        } catch (NumberFormatException e) {
            inviaErrore(exchange, 400, "ID non valido: '" + segmento + "'. Deve essere un numero intero");
            return;
        }

        Optional<OperazioneRecord> risultato = repository.trovaPerID(id);

        if (risultato.isPresent()) {
            inviaRisposta(exchange, 200, gson.toJson(risultato.get()));
        } else {
            inviaErrore(exchange, 404, "Operazione con id=" + id + " non trovata");
        }
    }

    // ------------------------------------------------------------------
    // Helpers per risposta e errori
    // ------------------------------------------------------------------

    private void inviaRisposta(HttpExchange exchange, int codice, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(codice, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void inviaErrore(HttpExchange exchange, int codice, String messaggio) throws IOException {
        Map<String, Object> errore = new HashMap<>();
        errore.put("errore", messaggio);
        errore.put("status", codice);
        inviaRisposta(exchange, codice, gson.toJson(errore));
    }
}
