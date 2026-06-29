package com.ritallus.logsmvvm.ui.viewmodel.logvm;

import java.util.function.Consumer;

import com.ritallus.logsmvvm.backend.core.service.LogStreamService;
import com.ritallus.logsmvvm.ui.viewmodel.logvm.services.UiLogFeederService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.springframework.stereotype.Component;

@Component
public class LogViewModel {

    private final LogStreamService logStreamService;
    private final BooleanProperty streamingActive = new SimpleBooleanProperty(false);

    private UiLogFeederService uiFeederService;
    private Consumer<String> onLinesAppended;

    public LogViewModel(LogStreamService logStreamService) {
        this.logStreamService = logStreamService;
    }

    // La vista llamará a esto para conectar el RichTextFX antes de dar Start
    public void setOnLinesAppended(Consumer<String> callback) {
        this.onLinesAppended = callback;
        // Inicializamos el servicio independiente pasando el buffer del backend y el callback
        this.uiFeederService = new UiLogFeederService(logStreamService.getLogBuffer(), onLinesAppended);
    }

    public void handleStart() {
        logStreamService.startStreaming();
        streamingActive.set(true);

        if (uiFeederService != null && !uiFeederService.isRunning()) {
            uiFeederService.restart();
        }
    }

    public void handleStop() {
        logStreamService.stopStreaming();
        streamingActive.set(false);

        if (uiFeederService != null && uiFeederService.isRunning()) {
            uiFeederService.cancel();
        }
    }

    public BooleanProperty streamingActiveProperty() {
        return streamingActive;
    }
}
