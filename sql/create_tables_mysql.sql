-- =============================================================
--  Script di creazione dello schema per la Calcolatrice REST
--  Sintassi MySQL 8.x
-- =============================================================

-- Crea il database (da eseguire una sola volta come amministratore)
CREATE DATABASE IF NOT EXISTS calcolatrice_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE calcolatrice_db;

-- Tabella principale delle operazioni eseguite
CREATE TABLE IF NOT EXISTS operazioni (
    id        INT         PRIMARY KEY AUTO_INCREMENT,
    operando1 DOUBLE      NOT NULL,
    operando2 DOUBLE      NOT NULL,
    operatore VARCHAR(20) NOT NULL,
    risultato DOUBLE      NOT NULL,
    timestamp DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Indice per ricerche ordinate per data (opzionale, migliora le performance)
CREATE INDEX idx_operazioni_timestamp ON operazioni (timestamp DESC);

-- =============================================================
--  Utente dedicato (buona pratica: non usare root in produzione)
-- =============================================================
-- CREATE USER 'calcolatrice'@'localhost' IDENTIFIED BY 'passwordSicura123!';
-- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, INDEX
--     ON calcolatrice_db.* TO 'calcolatrice'@'localhost';
-- FLUSH PRIVILEGES;
