package com.example.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * نقطه شروع برنامه + سشن متمرکز کاربر (توکن و نام کاربری جاری)
 */
public class MainApplication extends Application {

    public static String jwtToken = "";
    public static String currentUsername = "";

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("بازارچه دست‌دوم دپارتمان");
        stage.setScene(scene);
        stage.show();
    }

    /** بازگشت به صفحه ورود (خروج یا انقضای نشست) */
    public static void redirectToLogin(Stage currentStage, String message) {
        try {
            jwtToken = "";
            currentUsername = "";
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage loginStage = new Stage();
            loginStage.setTitle("ورود به حساب کاربری");
            loginStage.setScene(scene);
            loginStage.show();
            if (currentStage != null) {
                currentStage.close();
            }
            if (message != null && !message.isBlank()) {
                LoginController loginController = fxmlLoader.getController();
                if (loginController != null) {
                    loginController.showInfoMessage(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
