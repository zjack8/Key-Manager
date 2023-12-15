module com.zjack.lakmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.yaml.snakeyaml;
    requires jasypt;


    opens com.zjack.lakmanager to javafx.fxml;
    exports com.zjack.lakmanager;
}