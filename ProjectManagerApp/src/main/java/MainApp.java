import javax.swing.SwingUtilities;

// Точка входа в приложение для управления проектами
public class MainApp {
    public static void main(String[] args) {
        // Запуск GUI в потоке обработки событий Swing
        SwingUtilities.invokeLater(() -> new UI().createAndShowGUI());
    }
}
