package ui;

import algorithms.*; // Import all solvers
import models.Cell;
import models.Maze;
import utils.MazeParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

public class AppWindow extends JFrame {

    private MazePanel mazePanel;
    private JTextArea logArea;
    private JLabel statusLabel;
    private Maze currentMaze;

    private JComboBox<String> algoSelector;
    private JPanel gaSettingsPanel;

    private JTextField popSizeField;
    private JTextField generationField;
    private JTextField genomeLenField;
    private JTextField mutationRateField;
    private JTextField elitismField;

    private final Color SIDEBAR_COLOR = new Color(36, 49, 68);
    private final Color TEXT_COLOR = new Color(236, 240, 241);
    private final Color ACCENT_COLOR = new Color(52, 152, 219);
    private final Color BACKGROUND_COLOR = new Color(18, 22, 30);
    private final Color CARD_COLOR = new Color(30, 39, 50);
    private final Color INPUT_BG = new Color(44, 62, 80);
    private final Color INPUT_BORDER = new Color(52, 73, 94);

    public AppWindow() {
        setTitle("KMUTT Maze Runner • Visual Lab");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 850);
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
        sidebar.setPreferredSize(new Dimension(300, 0));
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
        sidebar.add(Box.createRigidArea(new Dimension(0, 25)));

        sidebar.add(createSectionLabel("1. Map Configuration"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(createStyledButton("Load Maze File", e -> loadMap()));
        sidebar.add(Box.createRigidArea(new Dimension(0, 25)));

        sidebar.add(createSectionLabel("2. Algorithm Selection"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        // --- UPDATED ALGORITHM LIST ---
        String[] algos = {
            "Dijkstra (Baseline)", 
            "A* Search (Heuristic)", 
            "Genetic Algorithm (Standard)",
            "Genetic Algorithm (DOD/SoA)" 
        };
        algoSelector = new JComboBox<>(algos);
        algoSelector.setMaximumSize(new Dimension(260, 35));
        algoSelector.setFont(new Font("SansSerif", Font.PLAIN, 13));
        algoSelector.setBackground(INPUT_BG);
        algoSelector.setForeground(TEXT_COLOR);
        algoSelector.setAlignmentX(Component.LEFT_ALIGNMENT);
        algoSelector.setFocusable(false);
        algoSelector.addActionListener(e -> toggleGASettings());

        sidebar.add(algoSelector);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));

        JButton runBtn = createStyledButton("Run Algorithm", e -> dispatchAlgorithm());
        sidebar.add(runBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 25)));

        // --- GA SETTINGS ---
        gaSettingsPanel = new JPanel();
        gaSettingsPanel.setLayout(new BoxLayout(gaSettingsPanel, BoxLayout.Y_AXIS));
        gaSettingsPanel.setOpaque(false);
        gaSettingsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gaSettingsPanel.setVisible(false);

        gaSettingsPanel.add(createSectionLabel("3. Genetic Config"));
        gaSettingsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        gaSettingsPanel.add(createInputRow("Population:", "2000"));
        gaSettingsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        gaSettingsPanel.add(createInputRow("Generations:", "2000"));
        gaSettingsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        gaSettingsPanel.add(createInputRow("Genome Len:", "3000"));
        gaSettingsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        gaSettingsPanel.add(createInputRow("Mutation Rate:", "0.03"));
        gaSettingsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        gaSettingsPanel.add(createInputRow("Elitism Count:", "50"));

        sidebar.add(gaSettingsPanel);
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
        statusBar.add(statusLabel, BorderLayout.WEST);

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
        mazeCard.add(mazeCardHeader, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(17, 24, 32));
        logArea.setForeground(new Color(236, 240, 241));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setRows(7);
        logArea.setMargin(new Insets(8, 8, 8, 8));
        logArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(44, 62, 80)));
        scrollPane.setPreferredSize(new Dimension(0, 150));

        mainContentWrapper.add(statusBar, BorderLayout.NORTH);
        mainContentWrapper.add(mazeCard, BorderLayout.CENTER);
        mainContentWrapper.add(scrollPane, BorderLayout.SOUTH);
        add(sidebar, BorderLayout.WEST);
        add(mainContentWrapper, BorderLayout.CENTER);

        log("System ready.");
    }

    private void toggleGASettings() {
        String selected = (String) algoSelector.getSelectedItem();
        boolean isGA = selected != null && selected.contains("Genetic");
        gaSettingsPanel.setVisible(isGA);
        gaSettingsPanel.revalidate();
        gaSettingsPanel.getParent().repaint();
    }

    private void dispatchAlgorithm() {
        if (currentMaze == null) {
            JOptionPane.showMessageDialog(this, "Please load a maze first.", "No Maze Loaded", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String selected = (String) algoSelector.getSelectedItem();
        if (selected == null) return;

        PathSolver solver = null;

        try {
            if (selected.contains("Dijkstra")) {
                solver = new DijkstraSolver();
            } else if (selected.contains("A*")) {
                solver = new AStarSolver();
            } else {
                // GA PARAMS
                int pop = Integer.parseInt(popSizeField.getText().trim());
                int gen = Integer.parseInt(generationField.getText().trim());
                int len = Integer.parseInt(genomeLenField.getText().trim());
                double mut = Double.parseDouble(mutationRateField.getText().trim());
                int elite = Integer.parseInt(elitismField.getText().trim());

                if (selected.contains("DOD")) {
                    GeneticSolver2 ga2 = new GeneticSolver2();
                    ga2.setParameters(pop, gen, len, mut, elite);
                    solver = ga2;
                } else {
                    GeneticSolver ga1 = new GeneticSolver();
                    // FIX: Now passing all 5 parameters as required by the updated class
                    ga1.setParameters(pop, gen, len, mut, elite);
                    solver = ga1;
                }
            }
        } catch (Exception e) {
            log("Error parsing params: " + e.getMessage());
            return;
        }

        runAlgorithm(solver);
    }

    private JPanel createInputRow(String labelText, String defaultValue) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(260, 30));
        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(new Color(189, 195, 199));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setPreferredSize(new Dimension(100, 30));
        JTextField field = new JTextField(defaultValue);
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(new LineBorder(INPUT_BORDER, 1), new EmptyBorder(0, 8, 0, 8)));
        field.setFont(new Font("Consolas", Font.PLAIN, 12));
        panel.add(lbl, BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);

        if (labelText.equals("Population:")) popSizeField = field;
        else if (labelText.equals("Generations:")) generationField = field;
        else if (labelText.equals("Genome Len:")) genomeLenField = field;
        else if (labelText.equals("Mutation Rate:")) mutationRateField = field;
        else if (labelText.equals("Elitism Count:")) elitismField = field;

        return panel;
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
                if (getModel().isPressed()) g2.setColor(ACCENT_COLOR.darker());
                else if (getModel().isRollover()) g2.setColor(ACCENT_COLOR);
                else g2.setColor(base);
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
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); }
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
        File dataDir = new File("./data");
        if (!dataDir.exists()) dataDir = new File("../data");
        fileChooser.setCurrentDirectory(dataDir.exists() ? dataDir : new File("."));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = fileChooser.getSelectedFile();
                currentMaze = MazeParser.parseMaze(f);
                mazePanel.setMaze(currentMaze);
                log("Loaded " + f.getName() + " [" + currentMaze.rows + "x" + currentMaze.cols + "]");
            } catch (Exception ex) {
                log("Error: " + ex.getMessage());
            }
        }
    }

    private void runAlgorithm(PathSolver solver) {
        String name = solver.getName();
        log("Running " + name + "...");
        statusLabel.setText("Running " + name + "...");
        SwingUtilities.invokeLater(() -> {
            long startTime = System.nanoTime();
            List<Cell> path = solver.solve(currentMaze);
            long endTime = System.nanoTime();
            if (path == null || path.isEmpty()) {
                log("Failure: " + name + " found no path.");
            } else {
                int cost = path.stream().mapToInt(c -> c.weight).sum();
                double time = (endTime - startTime) / 1_000_000.0;
                boolean success = path.get(path.size() - 1).isGoal;
                mazePanel.setPath(path);
                log(String.format("[%s] %s | Cost: %d | Time: %.2f ms | Steps: %d", success ? "SUCCESS" : "FAIL", name, cost, time, path.size()));
            }
        });
    }
}