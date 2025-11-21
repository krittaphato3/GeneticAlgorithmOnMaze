import ui.AppWindow;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // 1. Enable Hardware Acceleration for smooth graphics
        System.setProperty("sun.java2d.opengl", "true");
        
        // 2. Set Modern Look and Feel (Nimbus)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Fallback to default if Nimbus fails
        }

        // 3. Launch on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            AppWindow app = new AppWindow();
            app.setVisible(true);
        });
    }
}