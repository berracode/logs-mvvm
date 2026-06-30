package com.ritallus.logsmvvm.ui.view.controller;

import com.ritallus.logsmvvm.ui.viewmodel.logvm.LogViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.model.Paragraph;
import org.reactfx.collection.LiveList;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogViewController {

    private static final int MAX_VISIBLE_PARAGRAPHS = 5000;
    private final LogViewModel viewModel;
    @FXML
    private Button btnStart;
    @FXML
    private Button btnStop;
    @FXML
    private InlineCssTextArea txtLogArea;
    @FXML
    private TextField txtSearch;

    public LogViewController(LogViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {

        // 1. Vincular el TextField con el StringProperty del ViewModel
        txtSearch.textProperty().bindBidirectional(viewModel.getSearchQuery());

        // 1. Configurar el comportamiento de los botones usando el BooleanProperty del ViewModel
        btnStart.disableProperty().bind(viewModel.streamingActiveProperty());
        btnStop.disableProperty().bind(viewModel.streamingActiveProperty().not());

        // 2. Hacer el área de texto de RichTextFX de solo lectura
        txtLogArea.setEditable(false);

        // 3. Registrar el Consumer (callback) en el ViewModel
        viewModel.setOnLinesAppended(textBatch -> {
            // 1. Insertar el nuevo lote de logs al final
            txtLogArea.appendText(textBatch);

            // 2. Verificar si excedimos el límite máximo de párrafos permitidos
            LiveList<Paragraph<String, String, String>> currentParagraphsLiveList = txtLogArea.getParagraphs();
            var currentParagraphs = (long) currentParagraphsLiveList.size();
            log.info("Cantidad de lineas {}", currentParagraphs);

            if (currentParagraphs > MAX_VISIBLE_PARAGRAPHS) {
                int linesToRemove = Math.toIntExact(currentParagraphs - MAX_VISIBLE_PARAGRAPHS);
                log.info("Se procede a limpiar lineas de UI {}", linesToRemove);

                int endPosition = 0;
                for (int i = 0; i < linesToRemove; i++) {
                    endPosition += txtLogArea.getParagraph(i).length() + 1;
                }

                if (endPosition > 0 && endPosition <= txtLogArea.getLength()) {
                    // 1. Borramos el bloque excedido arriba
                    txtLogArea.deleteText(0, endPosition);

                    // 2. Ejecutamos el ajuste en el siguiente pulso del hilo de JavaFX
                    Platform.runLater(() -> {
                        // Forzamos al caret a ir al final absoluto del documento actual
                        txtLogArea.moveTo(txtLogArea.getLength());
                        // Le pedimos al visor virtualizado que baje hasta la posición del caret
                        txtLogArea.requestFollowCaret();
                    });
                }
            } else {
                // Si no está limpiando (bajada normal de logs), mantenemos el comportamiento base
                txtLogArea.requestFollowCaret();
            }

            // 3. Mantener el scroll al fondo
            txtLogArea.requestFollowCaret();
        });

        // 3. NUEVO: Callback para resultados de búsqueda (Reemplazo total)
        viewModel.setOnLogsClearedAndReloaded(searchResultsText -> {
            // Como es una búsqueda estática de SQLite, limpiamos todo el visor
            // e inyectamos los resultados del tirón
            txtLogArea.clear();
            txtLogArea.appendText(searchResultsText);

            // Llevamos el scroll al inicio para que el usuario lea desde el primer resultado
            Platform.runLater(() -> {
                txtLogArea.moveTo(0);
                txtLogArea.requestFollowCaret();
            });
        });

        // 4. Asignar las acciones manuales a los botones
        btnStart.setOnAction(event -> viewModel.startStreaming());
        btnStop.setOnAction(event -> viewModel.stopStreaming());
    }
}
