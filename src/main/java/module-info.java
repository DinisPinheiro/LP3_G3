module com.hotel4you.hotel4you {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.xml;
    requires com.microsoft.sqlserver.jdbc;

    opens com.hotel4you.hotel4you to javafx.fxml;
    opens com.hotel4you.hotel4you.View to javafx.fxml;
    exports com.hotel4you.hotel4you;
}