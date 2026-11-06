module com.example.projra3ds {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.projra3ds to javafx.fxml;
    exports com.example.projra3ds;
}