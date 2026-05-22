-- =============================================================
--  Script di creazione dello schema per la Calcolatrice REST
--  Compatibile con SQLite (usato come DB embedded)
-- =============================================================

-- Tabella principale delle operazioni eseguite
CREATE TABLE IF NOT EXISTS operazioni (
    id        INTEGER     PRIMARY KEY AUTOINCREMENT,
    operando1 REAL        NOT NULL,
    operando2 REAL        NOT NULL,
    operatore VARCHAR(20) NOT NULL,
    risultato REAL        NOT NULL,
    timestamp DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indice per ricerche ordinate per data (opzionale, migliora le performance)
CREATE INDEX IF NOT EXISTS idx_operazioni_timestamp ON operazioni (timestamp DESC);
