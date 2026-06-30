package com.ritallus.logsmvvm.backend.infrastructure.database;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.ritallus.logsmvvm.backend.core.model.LogLine;
import com.ritallus.logsmvvm.backend.core.ports.outbound.LogRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class SQLiteLogRepositoryAdapter implements LogRepository {

    private final JdbcTemplate jdbcTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public SQLiteLogRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initDatabase() {
        // Creamos la tabla orientada a texto en bruto + metadatos de búsqueda
        jdbcTemplate.execute("""
                                         CREATE TABLE IF NOT EXISTS log_store (
                                             id TEXT PRIMARY KEY,
                                             internal_timestamp TEXT,
                                             log_timestamp TEXT,
                                             message_id TEXT,
                                             content TEXT
                                         )
                                     """);
        // Índices vitales para búsquedas instantáneas
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_msg_id ON log_store(message_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_log_time ON log_store(log_timestamp)");
    }

    @Override
    public void save(LogLine logLine, String rawLine) {
        try {
            log.info("Insertando datos en BD - ID: {}", logLine.id());

            jdbcTemplate.update("""
                                            INSERT INTO log_store (id, internal_timestamp, log_timestamp, message_id, content)
                                            VALUES (?, ?, ?, ?, ?)
                                        """,
                                logLine.id(),
                                logLine.internalTimestamp().format(formatter),
                                logLine.timestamp().format(formatter),
                                logLine.messageId(),
                                rawLine
            );

        } catch (DataAccessException e) {
            // Captura cualquier fallo de Spring JDBC (Llave duplicada, tabla bloqueada, error de sintaxis, etc.)
            log.error("Fallo crítico al insertar log en SQLite. ID: {}, MessageId: {}", logLine.id(), logLine.messageId(), e);

            // Opción A: Relanzar una excepción técnica si quieres que el hilo de streaming se entere y se detenga
            // throw new TechnicalException("Error al persistir en la base de datos local", e);

            // Opción B: Si prefieres tolerancia a fallos (que la UI siga mostrando logs aunque SQLite falle en ese instante),
            // simplemente dejas el catch sin relanzar, permitiendo que el flujo del hilo continúe.
        } catch (Exception e) {
            // Fallo genérico (por ejemplo, un NullPointerException al formatear las fechas)
            log.error("Error inesperado de lógica antes o durante la inserción", e);
            throw e;
        }
    }

    @Override
    public List<LogLine> searchByMessageId(String messageId) {
        return jdbcTemplate.query(
                "SELECT * FROM log_store WHERE message_id = ?",
                (rs, rowNum) -> mapRow(rs),
                messageId
        );
    }

    @Override
    public List<LogLine> searchByContent(String query) {
        return jdbcTemplate.query(
                "SELECT * FROM log_store WHERE content LIKE ?",
                (rs, rowNum) -> mapRow(rs),
                "%" + query + "%"
        );
    }

    private LogLine mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new LogLine(
                rs.getString("id"),
                LocalDateTime.parse(rs.getString("log_timestamp"), formatter),
                LocalDateTime.parse(rs.getString("internal_timestamp"), formatter),
                null, // No los necesitamos reconstruir completos para la UI si usamos el 'content'
                null,
                null,
                rs.getString("message_id"),
                rs.getString("content")
        );
    }
}
