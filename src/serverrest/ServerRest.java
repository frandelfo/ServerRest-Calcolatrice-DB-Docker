package serverrest;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import serverrest.db.DatabaseManager;
import serverrest.db.OperazioneRepository;
import serverrest.db.OperazioneRepositoryImpl;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Punto di configurazione del server REST:
 *   - inizializza il database (DatabaseManager)
 *   - costruisce la catena di dipendenze: Repository → Service → Handlers
 *   - registra tutti gli endpoint sull'HttpServer
 *
 * @author delfo
 */
public class ServerRest {

    public static void avviaServer(int porta) {
        try {
            // --- Costruzione della catena di dipendenze (Dependency Injection manuale) ---
            DatabaseManager     dbManager  = DatabaseManager.getInstance();
            OperazioneRepository repository = new OperazioneRepositoryImpl(dbManager);
            CalcolatriceService  service    = new CalcolatriceService(repository);

            // --- Creazione del server ---
            HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);

            // Endpoint di calcolo (esistenti)
            server.createContext("/api/calcola/post", new PostHandler(service));
            server.createContext("/api/calcola/get",  new GetHandler(service));

            // Endpoint di consultazione storico (nuovi)
            server.createContext("/api/operazioni", new OperazioniHandler(repository));

            // Endpoint di benvenuto
            server.createContext("/", ServerRest::gestisciBenvenuto);

            server.setExecutor(null);
            server.start();

            System.out.println("==============================================");
            System.out.println("  Server REST Calcolatrice con DB avviato!");
            System.out.println("==============================================");
            System.out.println("Porta: " + porta);
            System.out.println();
            System.out.println("Endpoint calcolo:");
            System.out.println("  POST " + "http://localhost:" + porta + "/api/calcola/post");
            System.out.println("  GET  " + "http://localhost:" + porta + "/api/calcola/get?operando1=X&operando2=Y&operatore=OP");
            System.out.println();
            System.out.println("Endpoint storico:");
            System.out.println("  GET  " + "http://localhost:" + porta + "/api/operazioni");
            System.out.println("  GET  " + "http://localhost:" + porta + "/api/operazioni/{id}");
            System.out.println();
            System.out.println("Operatori: SOMMA, SOTTRAZIONE, MOLTIPLICAZIONE, DIVISIONE");
            System.out.println("Database:  calcolatrice.db (SQLite, nella directory corrente)");
            System.out.println("Premi Ctrl+C per fermare il server");
            System.out.println("==============================================");

        } catch (SQLException e) {
            System.err.println("Errore di connessione al database: " + e.getMessage());
            System.err.println("Verifica che il driver sqlite-jdbc.jar sia nel classpath.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Errore nell'avvio del server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void gestisciBenvenuto(HttpExchange exchange) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Map<String, Object> info = new HashMap<>();
        info.put("messaggio", "Benvenuto alla Calcolatrice REST API");
        info.put("versione", "3.0.0");
        info.put("tecnologia", "Java + GSON + SQLite");

        Map<String, String> calcolo = new HashMap<>();
        calcolo.put("POST", "/api/calcola/post");
        calcolo.put("GET",  "/api/calcola/get?operando1=X&operando2=Y&operatore=OP");
        info.put("endpoints_calcolo", calcolo);

        Map<String, String> storico = new HashMap<>();
        storico.put("GET lista",      "/api/operazioni");
        storico.put("GET per id",     "/api/operazioni/{id}");
        info.put("endpoints_storico", storico);

        Map<String, String> operatori = new HashMap<>();
        operatori.put("somma",          "SOMMA o +");
        operatori.put("sottrazione",    "SOTTRAZIONE o -");
        operatori.put("moltiplicazione","MOLTIPLICAZIONE o * o X");
        operatori.put("divisione",      "DIVISIONE o /");
        info.put("operatori_supportati", operatori);

        byte[] bytes = gson.toJson(info).getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
}
