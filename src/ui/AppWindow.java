package ui;

import algorithms.AStarSolver;
import algorithms.DijkstraSolver;
import algorithms.GeneticSolver;
import algorithms.PathSolver;
import models.Cell;
import models.Maze;
import utils.MazeParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class AppWindow extends JFrame {

    private MazePanel mazePanel;
    private JTextArea logArea;
    private Maze currentMaze;

    // Theme Colors
    private final Color SIDEBAR_COLOR = new Color(44, 62, 80);
    private final Color TEXT_COLOR = new Color(236, 240, 241);
    private final Color ACCENT_COLOR = new Color(52, 152, 219); // Blue

    public AppWindow() {
        setTitle("KMUTT Maze Runner | Pro Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. Sidebar (Controls)
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(SIDEBAR_COLOR);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title in Sidebar
        JLabel lblTitle = new JLabel("CONTROLS");
        lblTitle.setForeground(Color.GRAY);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lblTitle);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));

        // Buttons
        sidebar.add(createStyledButton("Load Map File", e -> loadMap()));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(new JSeparator());
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        
        sidebar.add(createStyledButton("Run Dijkstra (Benchmark)", e -> runAlgorithm(new DijkstraSolver())));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createStyledButton("Run A* Search (Fast)", e -> runAlgorithm(new AStarSolver())));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createStyledButton("Run Genetic Algorithm", e -> runAlgorithm(new GeneticSolver())));

        // 2. Main Content Area (Maze + Log)
        JPanel mainContent = new JPanel(new BorderLayout());
        
        mazePanel = new MazePanel();
        mainContent.add(mazePanel, BorderLayout.CENTER);

        // Log Area (Bottom)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(Color.GREEN); // Hacker terminal style
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setRows(6);
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60,60,60)));
        mainContent.add(scrollPane, BorderLayout.SOUTH);

        // Assemble
        add(sidebar, BorderLayout.WEST);
        add(mainContent, BorderLayout.CENTER);
        
        log("System Ready. Please load a maze.");
    }

    // --- Helper: Custom Button Factory ---
    private JButton createStyledButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text) {
            // Custom paint for modern flat look
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(ACCENT_COLOR.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(ACCENT_COLOR);
                } else {
                    g2.setColor(new Color(52, 73, 94)); // Default Dark Blue
                }
                
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                g2.setColor(TEXT_COLOR);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        
        btn.addActionListener(action);
        btn.setPreferredSize(new Dimension(200, 40));
        btn.setMaximumSize(new Dimension(210, 40)); // Prevent expanding
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void log(String message) {
        logArea.append(">> " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength()); // Auto scroll
    }

    private void loadMap() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("./data"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                currentMaze = MazeParser.parseMaze(selectedFile);
                mazePanel.setMaze(currentMaze);
                log("Map Loaded: " + selectedFile.getName() + 
                    " [" + currentMaze.rows + "x" + currentMaze.cols + "]");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage());
                log("Error: " + ex.getMessage());
            }
        }
    }

    private void runAlgorithm(PathSolver solver) {
        if (currentMaze == null) {
            JOptionPane.showMessageDialog(this, "Please load a map first!");
            return;
        }

        log("Running " + solver.getName() + "...");
        
        SwingUtilities.invokeLater(() -> {
            long startTime = System.nanoTime();
            List<Cell> path = solver.solve(currentMaze);
            long endTime = System.nanoTime();

            if (path == null || path.isEmpty()) {
                log("FAILURE: " + solver.getName() + " found no path.");
            } else {
                int totalCost = path.stream().mapToInt(c -> c.weight).sum();
                double durationMs = (endTime - startTime) / 1_000_000.0;
                boolean reachedGoal = path.get(path.size()-1).isGoal;

                mazePanel.setPath(path);
                
                String status = reachedGoal ? "SUCCESS" : "PARTIAL";
                log(String.format("[%s] %s | Cost: %d | Time: %.2fms | Steps: %d", 
                    status, solver.getName(), totalCost, durationMs, path.size()));
            }
        });
    }
}