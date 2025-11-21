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
import java.io.File;
import java.util.List;

public class AppWindow extends JFrame {

    private MazePanel mazePanel;
    private JTextArea logArea;
    private JLabel statusLabel;
    private Maze currentMaze;

    private final Color SIDEBAR_COLOR = new Color(36, 49, 68);
    private final Color TEXT_COLOR = new Color(236, 240, 241);
    private final Color ACCENT_COLOR = new Color(52, 152, 219);
    private final Color BACKGROUND_COLOR = new Color(18, 22, 30);
    private final Color CARD_COLOR = new Color(30, 39, 50);

    public AppWindow() {
        setTitle("KMUTT Maze Runner • Visual Lab");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);

        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, SIDEBAR_COLOR, 0, getHeight(), SIDEBAR_COLOR.darker());
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(24, 24, 24, 24));
        sidebar.setOpaque(false);

        JLabel appTitle = new JLabel("KMUTT Maze Runner");
        appTitle.setForeground(TEXT_COLOR);
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        appTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel appSubtitle = new JLabel("Pathfinding Sandbox");
        appSubtitle.setForeground(new Color(189, 195, 199));
        appSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        appSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(appTitle);
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        sidebar.add(appSubtitle);
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));
        sidebar.add(createSectionLabel("Map"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createStyledButton("Load Maze File", e -> loadMap()));
        sidebar.add(Box.createRigidArea(new Dimension(0, 24)));
        sidebar.add(createSectionLabel("Algorithms"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createStyledButton("Dijkstra (Baseline)", e -> runAlgorithm(new DijkstraSolver())));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createStyledButton("A* Search (Heuristic)", e -> runAlgorithm(new AStarSolver())));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createStyledButton("Genetic Solver (Experimental)", e -> runAlgorithm(new GeneticSolver())));
        sidebar.add(Box.createRigidArea(new Dimension(0, 24)));
        sidebar.add(createSectionLabel("Session"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        JLabel hint = new JLabel("<html><body style='width:200px'>1. Load a maze file<br>2. Choose an algorithm<br>3. Compare cost, time and steps</body></html>");
        hint.setForeground(new Color(189, 195, 199));
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(hint);
        sidebar.add(Box.createVerticalGlue());

        JPanel mainContentWrapper = new JPanel(new BorderLayout());
        mainContentWrapper.setBackground(BACKGROUND_COLOR);
        mainContentWrapper.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(23, 32, 42));
        statusBar.setBorder(new EmptyBorder(8, 14, 8, 14));

        statusLabel = new JLabel("Status: Ready. Load a maze to begin.");
        statusLabel.setForeground(new Color(214, 219, 223));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JLabel branding = new JLabel("KMUTT • CPE");
        branding.setForeground(new Color(127, 140, 141));
        branding.setFont(new Font("SansSerif", Font.PLAIN, 11));

        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(branding, BorderLayout.EAST);

        JPanel mazeCard = new JPanel(new BorderLayout());
        mazeCard.setBackground(CARD_COLOR);
        mazeCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        mazePanel = new MazePanel();
        mazePanel.setBackground(new Color(24, 32, 44));
        mazeCard.add(mazePanel, BorderLayout.CENTER);

        JPanel mazeCardHeader = new JPanel(new BorderLayout());
        mazeCardHeader.setOpaque(false);
        JLabel mazeTitle = new JLabel("Maze View");
        mazeTitle.setForeground(TEXT_COLOR);
        mazeTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        mazeCardHeader.add(mazeTitle, BorderLayout.WEST);
        JLabel mazeSubtitle = new JLabel("Visualizes grid, start, goal and explored path");
        mazeSubtitle.setForeground(new Color(149, 165, 166));
        mazeSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        mazeCardHeader.add(mazeSubtitle, BorderLayout.EAST);
        mazeCard.add(mazeCardHeader, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(17, 24, 32));
        logArea.setForeground(new Color(236, 240, 241));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setRows(7);
        logArea.setMargin(new Insets(8, 8, 8, 8));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(44, 62, 80)));
        scrollPane.getViewport().setBackground(new Color(17, 24, 32));
        scrollPane.setPreferredSize(new Dimension(0, 150));

        mainContentWrapper.add(statusBar, BorderLayout.NORTH);
        mainContentWrapper.add(mazeCard, BorderLayout.CENTER);
        mainContentWrapper.add(scrollPane, BorderLayout.SOUTH);

        add(sidebar, BorderLayout.WEST);
        add(mainContentWrapper, BorderLayout.CENTER);

        log("System ready. Load a maze from the left panel.");
    }

    private JLabel createSectionLabel(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setForeground(new Color(149, 165, 166));
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JButton createStyledButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color base = new Color(52, 73, 94);
                if (getModel().isPressed()) {
                    g2.setColor(ACCENT_COLOR.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(ACCENT_COLOR);
                } else {
                    g2.setColor(base);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);

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
        btn.setPreferredSize(new Dimension(220, 40));
        btn.setMaximumSize(new Dimension(260, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setForeground(TEXT_COLOR);
        btn.setOpaque(false);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });

        return btn;
    }

    private void log(String message) {
        logArea.append(">> " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
        statusLabel.setText("Status: " + message);
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
                String msg = "Loaded " + selectedFile.getName() +
                        "  [" + currentMaze.rows + " x " + currentMaze.cols + "]";
                log(msg);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage(),
                        "Load Error", JOptionPane.ERROR_MESSAGE);
                log("Error: " + ex.getMessage());
            }
        }
    }

    private void runAlgorithm(PathSolver solver) {
        if (currentMaze == null) {
            JOptionPane.showMessageDialog(this, "Please load a maze first.",
                    "No Maze Loaded", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String name = solver.getName();
        log("Running " + name + "...");
        statusLabel.setText("Status: Running " + name + "...");

        SwingUtilities.invokeLater(() -> {
            long startTime = System.nanoTime();
            List<Cell> path = solver.solve(currentMaze);
            long endTime = System.nanoTime();

            if (path == null || path.isEmpty()) {
                log("Failure: " + name + " found no path.");
            } else {
                int totalCost = path.stream().mapToInt(c -> c.weight).sum();
                double durationMs = (endTime - startTime) / 1_000_000.0;
                boolean reachedGoal = path.get(path.size() - 1).isGoal;
                mazePanel.setPath(path);
                String status = reachedGoal ? "SUCCESS" : "PARTIAL";
                String msg = String.format(
                        "[%s] %s | Cost: %d | Time: %.2f ms | Steps: %d",
                        status, name, totalCost, durationMs, path.size()
                );
                log(msg);
            }
        });
    }
}
