module com.ritallus.logsmvvm {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.boot;
    requires spring.core;
    requires org.fxmisc.richtext;
    requires static lombok;
    requires org.slf4j;
    requires spring.beans;

    opens com.ritallus.logsmvvm to javafx.fxml;
    exports com.ritallus.logsmvvm;
    exports com.ritallus.logsmvvm.startup;
    opens com.ritallus.logsmvvm.startup to javafx.fxml;

    // 2. ABRIR el paquete del servicio (Esto corrige tu error actual)
    exports com.ritallus.logsmvvm.backend.core.service;
    opens com.ritallus.logsmvvm.backend.core.service to spring.core, spring.context, spring.beans;

    // 3. Abrir los paquetes de la UI para Spring y JavaFX (Controladores y ViewModels)
    exports com.ritallus.logsmvvm.ui.view.controller;
    opens com.ritallus.logsmvvm.ui.view.controller to javafx.fxml, spring.core, spring.context, spring.beans;

    exports com.ritallus.logsmvvm.ui.viewmodel.logvm;
    opens com.ritallus.logsmvvm.ui.viewmodel.logvm to spring.core, spring.context, spring.beans;

    // Abrir el paquete de configuraciones asíncronas para que Spring aplique CGLIB
    exports com.ritallus.logsmvvm.configs.async;
    opens com.ritallus.logsmvvm.configs.async to spring.core, spring.context, spring.beans;
}
