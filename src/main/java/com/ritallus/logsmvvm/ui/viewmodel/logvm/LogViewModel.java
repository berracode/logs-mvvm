package com.ritallus.logsmvvm.ui.viewmodel.logvm;

import java.util.List;
import java.util.function.Consumer;

import com.ritallus.logsmvvm.backend.core.model.LogLine;
import com.ritallus.logsmvvm.backend.core.ports.outbound.LogRepository;
import com.ritallus.logsmvvm.backend.core.service.LogStreamService;
import com.ritallus.logsmvvm.ui.viewmodel.logvm.services.UiLogFeederService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogViewModel {

    private final LogStreamService logStreamService;
    private final LogRepository logRepository;
    private final BooleanProperty streamingActive = new SimpleBooleanProperty(false);

    @Getter
    private final StringProperty searchQuery = new SimpleStringProperty("");

    private UiLogFeederService uiLogFeederService;
    private Consumer<String> onLinesAppended;
    private Consumer<String> onLogsClearedAndReloaded;

    public LogViewModel(LogStreamService logStreamService, LogRepository logRepository) {
        this.logStreamService = logStreamService;
        this.logRepository = logRepository;
        this.searchQuery.addListener((observable, oldValue, newValue) -> handleSearch(newValue));
    }

    // Aquí es donde realmente se engancha la UI al flujo
    public void setOnLinesAppended(Consumer<String> onLinesAppended) {
        // Instanciamos TU UiLogFeederService pasándole el buffer real del backend
        // y el callback que actualizará el RichTextFX en la Vista
        this.uiLogFeederService = new UiLogFeederService(
                logStreamService.getLogBuffer(),
                onLinesAppended
        );
    }

    public void setOnLogsClearedAndReloaded(Consumer<String> callback) {
        this.onLogsClearedAndReloaded = callback;
    }

    public void startStreaming() {
        this.searchQuery.set(""); // Limpiamos filtro
        streamingActive.set(true);

        // 1. Arranca el hilo productor (Backend -> Virtual Threads)
        logStreamService.startStreaming();

        // 2. Arranca el hilo consumidor (JavaFX Service en Background Pool)
        if (uiLogFeederService != null && !uiLogFeederService.isRunning()) {
            uiLogFeederService.restart(); // Usamos restart() por si ya se había cancelado antes
        }
    }

    public void stopStreaming() {
        log.info("Deteniendo streaming");
        streamingActive.set(false);

        // 1. Apaga la ingesta del backend
        logStreamService.stopStreaming();

        // 2. Apaga el consumidor de la UI para que deje de bloquearse en buffer.take()
        if (uiLogFeederService != null && uiLogFeederService.isRunning()) {
            uiLogFeederService.cancel();
        }
    }

    private void handleSearch(String query) {
        log.info("Iniciando busqueda por contenido");
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        log.info("Contenido no NULL Iniciando busqueda por contenido");


        // Si escriben algo, apagamos el streaming por completo (ambos hilos)
        stopStreaming();

        // Buscamos en frío en SQLite
        List<LogLine> results = logRepository.searchByContent(query.trim());

        StringBuilder sb = new StringBuilder();
        for (LogLine log : results) {
            sb.append(log.message()).append("\n");
        }

        if (onLogsClearedAndReloaded != null) {
            if (sb.isEmpty()) {
                onLogsClearedAndReloaded.accept("No data to show");
            } else {
                onLogsClearedAndReloaded.accept(sb.toString());
            }
        }
    }

    public BooleanProperty streamingActiveProperty() {
        return streamingActive;
    }
}
