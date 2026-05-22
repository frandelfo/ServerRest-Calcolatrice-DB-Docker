# ── Stage 1: Build ──────────────────────────────────────────
FROM eclipse-temurin:24-jdk AS build

# Installa Apache Ant e curl (necessari per la build e per scaricare i driver)
RUN apt-get update && apt-get install -y ant curl && rm -rf /var/lib/apt/lists/*

# Crea la directory di lavoro
WORKDIR /build

# Copia i file necessari per la build
COPY lib/        lib/
COPY src/        src/
COPY build.xml   .
COPY manifest.mf .
COPY nbproject/  nbproject/

# Scarica i driver JDBC: sqlite-jdbc (non tracciato in git) e mysql-connector-j
RUN curl -fsSL \
      "https://github.com/xerial/sqlite-jdbc/releases/download/3.46.1.3/sqlite-jdbc-3.46.1.3.jar" \
      -o lib/sqlite-jdbc.jar \
 && curl -fsSL \
      "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar" \
      -o lib/mysql-connector-j.jar

# Compila e crea il JAR tramite Ant
RUN ant jar

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:24-jdk

WORKDIR /app

# Copia il JAR prodotto e tutti i driver JDBC
COPY --from=build /build/dist/ServerRest.jar          dist/ServerRest.jar
COPY --from=build /build/lib/gson-2.13.2.jar          lib/gson-2.13.2.jar
COPY --from=build /build/lib/sqlite-jdbc.jar          lib/sqlite-jdbc.jar
COPY --from=build /build/lib/mysql-connector-j.jar    lib/mysql-connector-j.jar

# Espone la porta del server REST
EXPOSE 8080

# Avvia il server; il database è configurato tramite la variabile d'ambiente DB_TYPE.
# Il numero di porta può essere sovrascritto passando un argomento: docker run ... image 9090
ENTRYPOINT ["java", \
    "-cp", "dist/ServerRest.jar:lib/gson-2.13.2.jar:lib/sqlite-jdbc.jar:lib/mysql-connector-j.jar", \
    "serverrest.App"]
CMD ["8080"]
