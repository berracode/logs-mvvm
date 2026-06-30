package com.ritallus.logsmvvm.backend.core.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ritallus.logsmvvm.backend.core.dto.LogRecordDto;
import com.ritallus.logsmvvm.backend.core.model.LogLine;
import com.ritallus.logsmvvm.backend.core.ports.outbound.LogRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogStreamService {

    @Getter
    private final BlockingQueue<LogLine> logBuffer = new LinkedBlockingQueue<>(5000);
    private final ExecutorService backendExecutor;
    private final LogRepository logRepository;
    private final DateTimeFormatter logFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final Pattern logPattern = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\w+)\\s+\\[([^\\]]+)\\]\\s+([^\\s]+)\\s+\\[([^\\]]+)\\]\\s+(.*)$"
    );
    // Mantiene la referencia de la tarea corriendo para poder cancelarla selectivamente
    private Future<?> streamingTask;
    @Getter
    private volatile boolean isRunning = false;

    public LogStreamService(@Qualifier("backendLogExecutor") ExecutorService backendExecutor,
                            LogRepository logRepositoryPort) {
        this.backendExecutor = backendExecutor;
        this.logRepository = logRepositoryPort;
    }

    public synchronized void startStreaming() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        log.info("Iniciando hilo productor de logs");

        // Enviamos la tarea al pool administrado por Spring y guardamos el token de control (Future)
        // Dentro de tu streamingTask = backendExecutor.submit(() -> { ...

        streamingTask = backendExecutor.submit(() -> {
            log.info("Hilo productor iniciado");
            AtomicInteger counter = new AtomicInteger(0);
            List<LogRecordDto> batchList = new ArrayList<>();

            try {
                while (isRunning && !Thread.currentThread().isInterrupted()) {

                    // 1. Simulamos la generación de logs en ráfaga corta (ej: generamos 50 de golpe)
                    for (int i = 0; i < 50; i++) {
                        int currentId = counter.incrementAndGet();
                        String rawLine = String.format(
                                "2026-06-29 15:10:%02d.102 INFO  [%s] c.r.l.b.c.s.LogStreamService [MSG-%d] Streaming data transaction payload simulated %d",
                                (int) (Math.random() * 59), Thread.currentThread().getName(), currentId, currentId);

                        LogLine logLine = parseRawLogLine(rawLine);

                        // Guardamos en la lista del lote actual
                        batchList.add(new LogRecordDto(logLine, rawLine));

                        // Enviamos al buffer de memoria inmediato para que la UI no sufra retrasos
                        logBuffer.put(logLine);
                    }

                    // 2. Mandar el bloque acumulado a la base de datos de un solo golpe
                    logRepository.saveAll(batchList);
                    batchList.clear(); // Limpiamos para la siguiente ráfaga

                    // Una tregua de 100ms antes de generar la siguiente ráfaga masiva
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                log.info("Hilo de streaming interrumpido de manera controlada.");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error inesperado en el bucle de streaming", e);
            } finally {
                // Guardar remanentes si quedaron encolados antes de apagar
                if (!batchList.isEmpty()) {
                    logRepository.saveAll(batchList);
                }
                log.info("Streaming finalizado limpiamente en el backend.");
            }
        });
    }

    private LogLine parseRawLogLine(String rawLine) {
        Matcher matcher = logPattern.matcher(rawLine);

        LocalDateTime internalNow = LocalDateTime.now(); // Tu internal timestamp de control

        if (matcher.matches()) {
            return new LogLine(
                    java.util.UUID.randomUUID().toString(),
                    LocalDateTime.parse(matcher.group(1), logFormatter), // Timestamp extraído del String
                    internalNow,                                         // Internal timestamp tuyo
                    matcher.group(2),                                    // Level
                    matcher.group(3),                                    // Thread
                    matcher.group(4),                                    // Logger
                    matcher.group(5),                                    // Message ID (MSG-XXX)
                    matcher.group(6)                                     // Mensaje
            );
        } else {
            // Si es un stacktrace o línea huérfana, manejamos un fallback amigable
            return new LogLine(
                    java.util.UUID.randomUUID().toString(),
                    internalNow, // Usamos el interno si no tiene fecha propia
                    internalNow,
                    "ERROR",
                    Thread.currentThread().getName(),
                    "UNKNOWN",
                    "MSG-UNKNOWN",
                    rawLine
            );
        }
    }

    public synchronized void stopStreaming() {
        if (!isRunning) {
            return;
        }
        isRunning = false;

        if (streamingTask != null) {
            // El true fuerza el envío de la señal de interrupción al hilo sin matar al ExecutorService completo
            streamingTask.cancel(true);
        }
    }

}
