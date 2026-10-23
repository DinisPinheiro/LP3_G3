module com.hotel4you.hotel4you {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.hotel4you.hotel4you to javafx.fxml;
    exports com.hotel4you.hotel4you;
}