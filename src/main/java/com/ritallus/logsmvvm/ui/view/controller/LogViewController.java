package com.ritallus.logsmvvm.ui.view.controller;

import com.ritallus.logsmvvm.ui.viewmodel.logvm.LogViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

    public LogViewController(LogViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
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


                // Obtenemos la posición del último carácter de la última línea que vamos a borrar
                // para hacer un solo corte limpio en el documento.
                int endPosition = 0;
                for (int i = 0; i < linesToRemove; i++) {
                    // Sumamos la longitud de cada párrafo más 1 (por el carácter \n)
                    endPosition += txtLogArea.getParagraph(i).length() + 1;
                }

                // Borramos el rango viejo desde el inicio (0) hasta el fin del lote excedido
                if (endPosition > 0 && endPosition <= txtLogArea.getLength()) {
                    txtLogArea.deleteText(0, endPosition);
                }
            }

            // 3. Mantener el scroll al fondo
            txtLogArea.requestFollowCaret();
        });

        // 4. Asignar las acciones manuales a los botones
        btnStart.setOnAction(event -> viewModel.handleStart());
        btnStop.setOnAction(event -> viewModel.handleStop());
    }
}
