module com.ritallus.logsmvvm {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.boot;
    requires spring.core;

    opens com.ritallus.logsmvvm to javafx.fxml;
    exports com.ritallus.logsmvvm;
}