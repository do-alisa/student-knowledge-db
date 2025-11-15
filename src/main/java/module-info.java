module cs151.application {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;

    opens cs151.application to javafx.fxml, javafx.base;
    exports cs151.application;
}