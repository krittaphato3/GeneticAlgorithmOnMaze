import ui.AppWindow;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");
        
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
        }

        SwingUtilities.invokeLater(() -> {
            AppWindow app = new AppWindow();
            app.setVisible(true);
        });
    }
}