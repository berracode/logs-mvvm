package com.ritallus.logsmvvm.startup;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ritallus.logsmvvm")
public class MainApplication {

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}
