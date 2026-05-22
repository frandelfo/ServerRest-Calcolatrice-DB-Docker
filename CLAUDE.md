# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Setup iniziale (una tantum)

1. Scaricare il driver JDBC per SQLite da https://github.com/xerial/sqlite-jdbc/releases
   e rinominarlo `sqlite-jdbc.jar`, quindi copiarlo nella cartella `lib/`.
2. Il database `calcolatrice.db` viene creato automaticamente nella directory corrente
   alla prima esecuzione (schema inizializzato da `DatabaseManager.initSchema()`).

## Build & Run

Progetto NetBeans/Ant, Java 24. Dipendenze: `lib/gson-2.13.2.jar`, `lib/sqlite-jdbc.jar`.

```bash
ant compile          # Compila in build/classes/
ant jar              # Genera dist/ServerRest.jar
ant run              # Compila ed esegue (porta default 8080)
ant clean            # Rimuove build/ e dist/
```

Esecuzione diretta con porta opzionale:
```bash
java -cp "dist/ServerRest.jar:lib/gson-2.13.2.jar:lib/sqlite-jdbc.jar" serverrest.App 8080
```

## Test manuale degli endpoint

```bash
# Calcolo via POST (salva in DB, risponde con id e timestamp)
curl -X POST http://localhost:8080/api/calcola/post \
     -H "Content-Type: application/json" \
     -d '{"operando1": 10, "operando2": 5, "operatore": "SOMMA"}'

# Calcolo via GET
curl "http://localhost:8080/api/calcola/get?operando1=10&operando2=5&operatore=SOMMA"

# Lista di tutte le operazioni salvate
curl http://localhost:8080/api/operazioni

# Singola operazione per id
curl http://localhost:8080/api/operazioni/1
```

In alternativa aprire `src/serverrest/test.html` nel browser con il server attivo.

## Architettura

Il server usa `com.sun.net.httpserver.HttpServer` (JDK built-in). Nessun framework web esterno.

### Catena di dipendenze (costruita in `ServerRest.avviaServer()`)

```
DatabaseManager (Singleton JDBC/SQLite)
    └── OperazioneRepositoryImpl  implements OperazioneRepository
            └── CalcolatriceService   (logica di calcolo + delega persistenza)
                    ├── PostHandler
                    └── GetHandler
OperazioneRepository ──→ OperazioniHandler  (storico)
```

### Package layout

| Package | Responsabilità |
|---------|---------------|
| `serverrest` | Entry point, server, handlers HTTP, model (request/record) |
| `serverrest.db` | Accesso dati: `DatabaseManager`, interfaccia `OperazioneRepository`, impl JDBC |

### Flusso di una richiesta di calcolo

1. `PostHandler` / `GetHandler` riceve la richiesta HTTP e valida i parametri
2. Chiama `CalcolatriceService.calcola()` che esegue la matematica (`eseguiCalcolo()` privata)
3. Il service chiama `OperazioneRepository.salva()` → INSERT nel DB SQLite
4. Il record persistito (con `id` e `timestamp` generati dal DB) viene serializzato da Gson e restituito al client

### Flusso di consultazione storico (`OperazioniHandler`)

- Path esattamente `/api/operazioni` → `repository.trovaTutte()` → lista JSON con campo `totale`
- Path `/api/operazioni/{id}` → `repository.trovaPerID(id)` → record singolo o 404

### Endpoint completi

| Metodo | Path | Descrizione |
|--------|------|-------------|
| POST | `/api/calcola/post` | Calcolo da body JSON |
| GET  | `/api/calcola/get`  | Calcolo da query string |
| GET  | `/api/operazioni`   | Storico completo |
| GET  | `/api/operazioni/{id}` | Operazione per id |
| GET  | `/`                 | Info/welcome |

**Operatori accettati:** `SOMMA`, `SOTTRAZIONE`, `MOLTIPLICAZIONE`, `DIVISIONE` (anche simboli `+`, `-`, `*`/`X`, `/`; case-insensitive).

Tutte le risposte hanno `Content-Type: application/json; charset=UTF-8` e `Access-Control-Allow-Origin: *`.

### Schema SQL (`sql/create_tables.sql`)

```sql
CREATE TABLE IF NOT EXISTS operazioni (
    id        INTEGER     PRIMARY KEY AUTOINCREMENT,
    operando1 REAL        NOT NULL,
    operando2 REAL        NOT NULL,
    operatore VARCHAR(20) NOT NULL,
    risultato REAL        NOT NULL,
    timestamp DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```
