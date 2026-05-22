package serverrest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler per POST /api/calcola/post
 * Riceve un body JSON con operando1, operando2, operatore; restituisce il record
 * persistito (inclusivo di id e timestamp).
 *
 * @author delfo
 */
public class PostHandler implements HttpHandler {

    private final CalcolatriceService service;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public PostHandler(CalcolatriceService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            inviaErrore(exchange, 405, "Metodo non consentito. Usa POST");
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)
            );
            OperazioneRequest request = gson.fromJson(reader, OperazioneRequest.class);
            reader.close();

            if (request == null) {
                inviaErrore(exchange, 400, "Body della richiesta vuoto o non valido");
                return;
            }
            if (request.getOperatore() == null || request.getOperatore().trim().isEmpty()) {
                inviaErrore(exchange, 400, "Operatore mancante o vuoto");
                return;
            }

            OperazioneRecord record = service.calcola(
                request.getOperando1(),
                request.getOperando2(),
                request.getOperatore()
            );

            inviaRisposta(exchange, 201, gson.toJson(record));

        } catch (JsonSyntaxException e) {
            inviaErrore(exchange, 400, "JSON non valido: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            inviaErrore(exchange, 400, e.getMessage());
        } catch (SQLException e) {
            inviaErrore(exchange, 500, "Errore database: " + e.getMessage());
        } catch (Exception e) {
            inviaErrore(exchange, 500, "Errore interno del server: " + e.getMessage());
        }
    }

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
