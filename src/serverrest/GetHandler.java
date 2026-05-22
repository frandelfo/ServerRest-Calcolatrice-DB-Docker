package serverrest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler per GET /api/calcola/get?operando1=X&operando2=Y&operatore=OP
 * Restituisce il record persistito (inclusivo di id e timestamp).
 *
 * @author delfo
 */
public class GetHandler implements HttpHandler {

    private final CalcolatriceService service;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public GetHandler(CalcolatriceService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            inviaErrore(exchange, 405, "Metodo non consentito. Usa GET");
            return;
        }

        try {
            Map<String, String> parametri = estraiParametri(exchange.getRequestURI().getQuery());

            if (!parametri.containsKey("operando1") ||
                !parametri.containsKey("operando2") ||
                !parametri.containsKey("operatore")) {
                inviaErrore(exchange, 400,
                    "Parametri mancanti. Necessari: operando1, operando2, operatore");
                return;
            }

            double operando1 = Double.parseDouble(parametri.get("operando1"));
            double operando2 = Double.parseDouble(parametri.get("operando2"));
            String operatore = parametri.get("operatore");

            OperazioneRecord record = service.calcola(operando1, operando2, operatore);

            inviaRisposta(exchange, 200, gson.toJson(record));

        } catch (NumberFormatException e) {
            inviaErrore(exchange, 400, "Operandi non validi. Devono essere numeri");
        } catch (IllegalArgumentException e) {
            inviaErrore(exchange, 400, e.getMessage());
        } catch (SQLException e) {
            inviaErrore(exchange, 500, "Errore database: " + e.getMessage());
        } catch (Exception e) {
            inviaErrore(exchange, 500, "Errore interno del server: " + e.getMessage());
        }
    }

    private Map<String, String> estraiParametri(String query) {
        Map<String, String> parametri = new HashMap<>();
        if (query == null || query.isEmpty()) return parametri;

        for (String coppia : query.split("&")) {
            String[] kv = coppia.split("=", 2);
            if (kv.length == 2) {
                try {
                    parametri.put(
                        URLDecoder.decode(kv[0], "UTF-8"),
                        URLDecoder.decode(kv[1], "UTF-8")
                    );
                } catch (Exception ignored) { }
            }
        }
        return parametri;
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
