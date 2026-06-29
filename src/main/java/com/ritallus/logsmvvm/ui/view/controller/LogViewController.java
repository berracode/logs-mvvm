package com.ritallus.logsmvvm.ui.view.controller;

import com.ritallus.logsmvvm.ui.viewmodel.logvm.LogViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.fxmisc.richtext.InlineCssTextArea;
import org.springframework.stereotype.Component;

@Component
public class LogViewController {

    @FXML
    private Button btnStart;
    @FXML
    private Button btnStop;
    @FXML
    private InlineCssTextArea txtLogArea;

    private final LogViewModel viewModel;

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
            txtLogArea.appendText(textBatch);

            // Auto-scroll automático hacia el final del documento
            txtLogArea.requestFollowCaret();
        });

        // 4. Asignar las acciones manuales a los botones
        btnStart.setOnAction(event -> viewModel.handleStart());
        btnStop.setOnAction(event -> viewModel.handleStop());
    }
}
