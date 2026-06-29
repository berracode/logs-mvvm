package com.ritallus.logsmvvm.startup;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PrimaryStageInitializer implements ApplicationListener<StageReadyEvent> {

    private final ApplicationContext context;

    // Inyectamos el contexto de Spring
    public PrimaryStageInitializer(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        try {
            Stage stage = event.getStage();

            ClassPathResource fxmlResource = new ClassPathResource("ui/main-view.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ui/main-view.fxml"));
            fxmlLoader.setControllerFactory(context::getBean);

            Parent parent = fxmlLoader.load();
            Scene scene = new Scene(parent, 1000, 650);

            stage.setTitle("Kabrilla LogStreamer");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            throw new RuntimeException("Error al inicializar la Stage principal de JavaFX", e);
        }
    }
}
