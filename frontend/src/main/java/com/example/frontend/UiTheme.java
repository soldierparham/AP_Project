package com.example.frontend;

import javafx.animation.PauseTransition;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * ۰️⃣ استایل مشترک پاپ‌آپ‌ها و اعلان‌ها مطابق تم برنامه.
 * پاپ‌آپ فقط برای خطاها و تایید حذف آگهی استفاده می‌شود؛
 * بقیه پیام‌ها به صورت اعلان کوتاه (Toast) نمایش داده می‌شوند.
 */
public final class UiTheme {

    /**
     * سازنده کلاس UiTheme؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    private UiTheme() {
    }

    /** اعمال تم برنامه روی پاپ‌آپ (خطا یا تایید حذف). */
    public static void styleAlert(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        boolean danger = alert.getAlertType() == Alert.AlertType.ERROR
                || alert.getAlertType() == Alert.AlertType.WARNING;
        String accent = danger ? "#ff5555" : "#ffc83b";

        pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        pane.setStyle("-fx-background-color: #241942;"
                + "-fx-border-color: " + accent + ";"
                + "-fx-border-width: 1.5px;"
                + "-fx-padding: 18px;"
                + "-fx-font-family: 'Vazirmatn';");

        Node content = pane.lookup(".content.label");
        if (content != null) {
            content.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-family: 'Vazirmatn';");
        }
        Node header = pane.lookup(".header-panel");
        if (header != null) {
            header.setStyle("-fx-background-color: transparent;");
        }
        Node buttonBar = pane.lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setStyle("-fx-background-color: transparent;");
        }
        for (ButtonType bt : alert.getButtonTypes()) {
            Node b = pane.lookupButton(bt);
            if (b == null) continue;
            boolean primary = bt.getButtonData() != null && (bt.getButtonData().isDefaultButton()
                    || bt.getButtonData() == javafx.scene.control.ButtonBar.ButtonData.YES);
            String bg = primary ? (danger ? "#b71c1c" : "#ffc83b") : "#3b286b";
            String fg = primary && !danger ? "#160f29" : "white";
            b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";"
                    + "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;"
                    + "-fx-font-family: 'Vazirmatn'; -fx-font-size: 13px; -fx-padding: 6 20 6 20;");
        }
    }

    /** اعمال تم برنامه روی دیالوگ ورود متن (مثل دلیل رد آگهی). */
    public static void styleTextInputDialog(TextInputDialog dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        pane.setStyle("-fx-background-color: #241942;"
                + "-fx-border-color: #ffc83b;"
                + "-fx-border-width: 1.5px;"
                + "-fx-padding: 18px;"
                + "-fx-font-family: 'Vazirmatn';");
        dialog.getEditor().setStyle("-fx-background-color: #160f29; -fx-text-fill: white;"
                + "-fx-prompt-text-fill: #8b7ca6;"
                + "-fx-border-color: #3b286b; -fx-border-radius: 8; -fx-background-radius: 8;"
                + "-fx-font-family: 'Vazirmatn'; -fx-font-size: 13px; -fx-padding: 8;");

        // 🔧 برچسب دکمه‌های پیش‌فرض (OK/Cancel) باید همین‌جا، پیش از نمایش دیالوگ، به فارسی تغییر کند.
        // اگر این تغییر بعد از نمایش دیالوگ انجام شود (مثل حالت قبلی)، عرض پنجره بر اساس متن انگلیسی
        // «Cancel» محاسبه و ثابت می‌شود و دکمه‌ی فارسی «انصراف» به‌طور ناقص/بریده دیده می‌شود.
        // چون فقط ButtonData عوض نمی‌شود (همان OK_DONE / CANCEL_CLOSE می‌ماند)، منطق showAndWait().ifPresent(...)
        // در جاهایی که از این دیالوگ استفاده می‌کنند بدون تغییر درست کار می‌کند.
        for (int i = 0; i < pane.getButtonTypes().size(); i++) {
            ButtonType bt = pane.getButtonTypes().get(i);
            if (bt.getButtonData() == javafx.scene.control.ButtonBar.ButtonData.OK_DONE) {
                pane.getButtonTypes().set(i, new ButtonType("تأیید", javafx.scene.control.ButtonBar.ButtonData.OK_DONE));
            } else if (bt.getButtonData() == javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE) {
                pane.getButtonTypes().set(i, new ButtonType("انصراف", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE));
            }
        }

        // رنگ‌آمیزی باید بعد از نمایش دیالوگ اعمال شود، وگرنه lookup کار نمی‌کند؛ اما دیگر متنی تغییر نمی‌دهیم،
        // پس اندازه‌ی پنجره از همان ابتدا با متن نهایی فارسی محاسبه شده و نیازی به resize دوباره نیست.
        dialog.setOnShown(ev -> {
            pane.applyCss();
            pane.layout();
            Node header = pane.lookup(".header-panel");
            if (header != null) {
                header.setStyle("-fx-background-color: transparent;");
            }
            Node buttonBar = pane.lookup(".button-bar");
            if (buttonBar != null) {
                buttonBar.setStyle("-fx-background-color: transparent;");
            }
            for (Node n : pane.lookupAll(".label")) {
                n.setStyle("-fx-text-fill: white; -fx-font-size: 13.5px; -fx-font-family: 'Vazirmatn';");
            }
            for (ButtonType bt : pane.getButtonTypes()) {
                Node b = pane.lookupButton(bt);
                if (b == null) continue;
                boolean primary = bt.getButtonData() != null && bt.getButtonData().isDefaultButton();
                String bg = primary ? "#ffc83b" : "#3b286b";
                String fg = primary ? "#160f29" : "white";
                if (b instanceof javafx.scene.control.Button) {
                    ((javafx.scene.control.Button) b).setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                }
                b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";"
                        + "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;"
                        + "-fx-font-family: 'Vazirmatn'; -fx-font-size: 13px; -fx-padding: 6 20 6 20;");
            }
        });
    }

    /** اعلان کوتاه غیرمزاحم پایین پنجره — جایگزین پاپ‌آپ موفقیت. */
    public static void toast(String message) {
        Window owner = null;
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w.isFocused()) { owner = w; break; }
        }
        if (owner == null) {
            for (Window w : Window.getWindows()) {
                if (w.isShowing()) { owner = w; break; }
            }
        }
        if (owner == null) return;

        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.setMaxWidth(440);
        lbl.setAlignment(Pos.CENTER);
        lbl.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        lbl.setStyle("-fx-background-color: #241942; -fx-text-fill: #ffc83b;"
                + "-fx-border-color: #3b286b; -fx-border-width: 1.5; -fx-border-radius: 10;"
                + "-fx-background-radius: 10; -fx-padding: 12 24 12 24;"
                + "-fx-font-family: 'Vazirmatn'; -fx-font-size: 13px;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 18, 0, 0, 4);");

        Popup popup = new Popup();
        popup.getContent().add(lbl);
        popup.setAutoFix(true);
        popup.show(owner);
        popup.setX(owner.getX() + (owner.getWidth() - popup.getWidth()) / 2);
        popup.setY(owner.getY() + owner.getHeight() - popup.getHeight() - 60);

        PauseTransition wait = new PauseTransition(Duration.seconds(2.5));
        wait.setOnFinished(e -> popup.hide());
        wait.play();
    }
}
