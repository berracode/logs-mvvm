package com.ritallus.logsmvvm.ui.viewmodel.logvm.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import com.ritallus.logsmvvm.backend.core.model.LogLine;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UiLogFeederService extends Service<Void> {
    private final BlockingQueue<LogLine> buffer;
    private final Consumer<String> onLinesBatchProcessed;

    // Pasamos el buffer y el callback explícitamente en el constructor
    public UiLogFeederService(BlockingQueue<LogLine> buffer, Consumer<String> onLinesBatchProcessed) {
        this.buffer = buffer;
        this.onLinesBatchProcessed = onLinesBatchProcessed;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<LogLine> drainList = new ArrayList<>();
                log.info("Inicia el hilo consumidor de LOGS");

                while (!isCancelled()) {
                    // Bloquea el hilo secundario hasta que entre un log
                    LogLine firstLog = buffer.take();
                    drainList.add(firstLog);

                    // Extrae ráfagas acumuladas para armar el lote (batch)
                    buffer.drainTo(drainList, 200);

                    // Construcción del bloque de texto plano
                    StringBuilder sb = new StringBuilder();
                    for (LogLine log : drainList) {
                        sb.append(String.format("%s [%s] %s - %s\n",
                                                log.timestamp(), log.level(), log.messageId(), log.message()));
                    }
                    String textBatch = sb.toString();

                    // Despachamos el bloque al hilo de JavaFX de forma segura
                    Platform.runLater(() -> {
                        if (onLinesBatchProcessed != null) {
                            onLinesBatchProcessed.accept(textBatch);
                        }
                    });

                    drainList.clear();
                    Thread.sleep(40); // Pequeña tregua para acumular el siguiente lote
                }
                return null;
            }
        };
    }
}
