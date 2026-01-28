package mazerunner.ui;

import mazerunner.algorithms.AStarSolver;
import mazerunner.algorithms.DijkstraSolver;
import mazerunner.algorithms.Genetic.GeneticSolverI;
import mazerunner.algorithms.Genetic.GeneticSolverII;
import mazerunner.algorithms.MazeSolver;
import mazerunner.models.*;
import mazerunner.strategies.crossover.*;
import mazerunner.strategies.fitness.*;
import mazerunner.strategies.initial.*;
import mazerunner.strategies.mutation.*;
import mazerunner.strategies.selection.*;
import mazerunner.utils.MazeParser;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class AppWindow extends JFrame {

    private MazePanel mazePanel;
    private JTextArea logArea;
    private JLabel statusLabel;
    private Maze currentMaze;

    private JSpinner populationSpinner, generationsSpinner, genomeLenSpinner, elitismSpinner;
    private JTextField adaptiveRatesTextField;
    private JPanel gaParamsPanel;

    private String selectedAlgorithm = "Genetic Algorithm";
    private JButton algorithmDisplayButton;
    private JPanel algorithmChoicesPanel;

    private String selectedSelectionStrategy = "Tournament";
    private JButton selectionDisplayButton;
    private JPanel selectionChoicesPanel;
    private JPanel tournamentPanel;
    private JSpinner tournamentKSpinner;

    private String selectedCrossoverStrategy = "Multi-Point";
    private JButton crossoverDisplayButton;
    private JPanel crossoverChoicesPanel;
    private JPanel multiPointPanel;
    private JSpinner crossoverRateSpinner;
    private JSpinner crossoverPointsSpinner;
    private JPanel uniformPanel;
    private JSpinner mixingRatioSpinner;

    private String selectedInitialStrategy = "Heuristic";
    private JButton initialDisplayButton;
    private JPanel initialChoicesPanel;

    private String selectedFitnessStrategy = "Hybrid";
    private JButton fitnessDisplayButton;
    private JPanel fitnessChoicesPanel;

    private String selectedMutationStrategy = "Inversion";
    private JButton mutationDisplayButton;
    private JPanel mutationChoicesPanel;

    private final Color SIDEBAR_COLOR = new Color(36, 49, 68);
    private final Color TEXT_COLOR = new Color(236, 240, 241);
    private final Color ACCENT_COLOR = new Color(52, 152, 219);
    private final Color BACKGROUND_COLOR = new Color(18, 22, 30);
    private final Color CARD_COLOR = new Color(30, 39, 50);

    private final int VERTICAL_GAP = 10;
    private final int SECTION_GAP = 20;

    public AppWindow() {
        setTitle("KMUTT Maze Runner • Visual Lab");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_COLOR);
        sidebar.setBorder(new EmptyBorder(24, 24, 24, 24));
        
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
        sidebar.add(Box.createRigidArea(new Dimension(0, SECTION_GAP)));
        sidebar.add(createSectionLabel("Map"));
        sidebar.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        sidebar.add(createStyledButton("Load Maze File", e -> loadMap()));
        sidebar.add(Box.createRigidArea(new Dimension(0, SECTION_GAP)));
        
        // --- ALGORITHM SELECTION ---
        sidebar.add(createSectionLabel("Algorithm"));
        sidebar.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        String[] algorithms = {"Genetic Algorithm", "GA (DOD/SoA Optimized)", "Dijkstra (Baseline)", "A* Search (Heuristic)"};
        createStrategySelector(sidebar, algorithms, selectedAlgorithm, "Algorithm", (selection) -> {
            selectedAlgorithm = selection;
            algorithmDisplayButton.setText(selection);
            gaParamsPanel.setVisible("Genetic Algorithm".equals(selection) || "GA (DOD/SoA Optimized)".equals(selection));
        });

        // --- DYNAMIC PANELS ---
        gaParamsPanel = createGeneticAlgorithmPanel();
        sidebar.add(gaParamsPanel);
        
        sidebar.add(Box.createRigidArea(new Dimension(0, SECTION_GAP)));
        sidebar.add(createStyledButton("Run Algorithm", e -> runSelectedAlgorithm()));
        sidebar.add(Box.createVerticalGlue());

        JScrollPane sidebarScrollPane = new JScrollPane(sidebar);
        sidebarScrollPane.setBorder(BorderFactory.createEmptyBorder());
        sidebarScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // --- MAIN CONTENT ---
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

        add(sidebarScrollPane, BorderLayout.WEST);
        add(mainContentWrapper, BorderLayout.CENTER);

        log("System ready. Load a maze from the left panel.");
    }

    private void runSelectedAlgorithm() {
        if (selectedAlgorithm == null) return;
        
        MazeSolver solver = null;
        try {
            switch (selectedAlgorithm) {
                case "Genetic Algorithm":
                    int populationSize = (int) populationSpinner.getValue();
                    int maxGenerations = (int) generationsSpinner.getValue();
                    int genomeLength = (int) genomeLenSpinner.getValue();
                    int elitismCount = (int) elitismSpinner.getValue();
                    String[] rateStrings = adaptiveRatesTextField.getText().split(",");
                    double[] adaptiveRates = new double[rateStrings.length];
                    for (int i = 0; i < rateStrings.length; i++) {
                        adaptiveRates[i] = Double.parseDouble(rateStrings[i].trim());
                    }

                    SelectionStrategy selectionStrategy = null;
                    switch(selectedSelectionStrategy) {
                        case "Tournament":
                            selectionStrategy = new TournamentSelection((int)tournamentKSpinner.getValue());
                            break;
                        case "Rank":
                            selectionStrategy = new RankSelection();
                            break;
                        case "Roulette":
                            selectionStrategy = new RouletteWheel();
                            break;
                    }

                    CrossOverStrategy crossoverStrategy = null;
                    switch(selectedCrossoverStrategy) {
                        case "Multi-Point":
                            crossoverStrategy = new MultiPointCrossover((double)crossoverRateSpinner.getValue(), (int)crossoverPointsSpinner.getValue());
                            break;
                        case "Uniform":
                            crossoverStrategy = new UniformCrossover((double)mixingRatioSpinner.getValue());
                            break;
                    }

                    InitStrategy initialStrategy = null;
                    switch(selectedInitialStrategy) {
                        case "Heuristic":
                            initialStrategy = new HeuristicInitial();
                            break;
                        case "Hybrid":
                            initialStrategy = new HybridInitial();
                            break;
                        case "Random":
                            initialStrategy = new RandomInitial();
                            break;
                    }

                    FitnessStrategy fitnessStrategy = null;
                    switch(selectedFitnessStrategy) {
                        case "Proximity":
                            fitnessStrategy = new FitnessProximity();
                            break;
                        case "Hybrid":
                            fitnessStrategy = new HybridFitness();
                            break;
                        case "Minimum Weight":
                            fitnessStrategy = new MinimumWeightFitness();
                            break;
                    }

                    MutationStrategy mutationStrategy = null;
                    switch(selectedMutationStrategy) {
                        case "Fixed Count":
                            mutationStrategy = new FixedCountMutation();
                            break;
                        case "Inversion":
                            mutationStrategy = new InversionMutation();
                            break;
                        case "Random Reset":
                            mutationStrategy = new RandomResetMutation();
                            break;
                        case "Swap":
                            mutationStrategy = new SwapMutation();
                            break;
                    }
                    
                    solver = new GeneticSolverI(
                            currentMaze,
                            populationSize,
                            maxGenerations,
                            genomeLength,
                            initialStrategy,
                            fitnessStrategy,
                            selectionStrategy,
                            crossoverStrategy,
                            mutationStrategy,
                            adaptiveRates,
                            elitismCount
                    );
                    break;
                case "GA (DOD/SoA Optimized)":
                    populationSize = (int) populationSpinner.getValue();
                    maxGenerations = (int) generationsSpinner.getValue();
                    genomeLength = (int) genomeLenSpinner.getValue();
                    elitismCount = (int) elitismSpinner.getValue();
                    rateStrings = adaptiveRatesTextField.getText().split(",");
                    adaptiveRates = new double[rateStrings.length];
                    for (int i = 0; i < rateStrings.length; i++) {
                        adaptiveRates[i] = Double.parseDouble(rateStrings[i].trim());
                    }

                    selectionStrategy = null;
                    switch(selectedSelectionStrategy) {
                        case "Tournament":
                            selectionStrategy = new TournamentSelection((int)tournamentKSpinner.getValue());
                            break;
                        case "Rank":
                            selectionStrategy = new RankSelection();
                            break;
                        case "Roulette":
                            selectionStrategy = new RouletteWheel();
                            break;
                    }

                    crossoverStrategy = null;
                    switch(selectedCrossoverStrategy) {
                        case "Multi-Point":
                            crossoverStrategy = new MultiPointCrossover((double)crossoverRateSpinner.getValue(), (int)crossoverPointsSpinner.getValue());
                            break;
                        case "Uniform":
                            crossoverStrategy = new UniformCrossover((double)mixingRatioSpinner.getValue());
                            break;
                    }
                    
                    initialStrategy = null;
                    switch(selectedInitialStrategy) {
                        case "Heuristic":
                            initialStrategy = new HeuristicInitial();
                            break;
                        case "Hybrid":
                            initialStrategy = new HybridInitial();
                            break;
                        case "Random":
                            initialStrategy = new RandomInitial();
                            break;
                    }

                    fitnessStrategy = null;
                    switch(selectedFitnessStrategy) {
                        case "Proximity":
                            fitnessStrategy = new FitnessProximity();
                            break;
                        case "Hybrid":
                            fitnessStrategy = new HybridFitness();
                            break;
                        case "Minimum Weight":
                            fitnessStrategy = new MinimumWeightFitness();
                            break;
                    }

                    mutationStrategy = null;
                    switch(selectedMutationStrategy) {
                        case "Fixed Count":
                            mutationStrategy = new FixedCountMutation();
                            break;
                        case "Inversion":
                            mutationStrategy = new InversionMutation();
                            break;
                        case "Random Reset":
                            mutationStrategy = new RandomResetMutation();
                            break;
                        case "Swap":
                            mutationStrategy = new SwapMutation();
                            break;
                    }

                    solver = new GeneticSolverII(
                            currentMaze,
                            populationSize,
                            maxGenerations,
                            genomeLength,
                            initialStrategy,
                            fitnessStrategy,
                            selectionStrategy,
                            crossoverStrategy,
                            mutationStrategy,
                            adaptiveRates,
                            elitismCount
                    );
                    break;
                case "Dijkstra (Baseline)":
                    solver = new DijkstraSolver(currentMaze);
                    break;
                case "A* Search (Heuristic)":
                    solver = new AStarSolver(currentMaze);
                    break;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number in GA parameters.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (solver != null) {
            runAlgorithm(solver);
        }
    }

    // ... createGeneticAlgorithmPanel and other panel creators
    private JPanel createGeneticAlgorithmPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(VERTICAL_GAP, 0, 0, 0));

        panel.add(createSectionLabel("GA Parameters"));
        panel.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));

        populationSpinner = new JSpinner(new SpinnerNumberModel(1000, 10, 10000, 100));
        generationsSpinner = new JSpinner(new SpinnerNumberModel(15000, 100, 100000, 1000));
        genomeLenSpinner = new JSpinner(new SpinnerNumberModel(1000, 10, 5000, 100));
        elitismSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 100, 1));

        panel.add(createSpinnerPanel("Population Size:", populationSpinner));
        panel.add(createSpinnerPanel("Max Generations:", generationsSpinner));
        panel.add(createSpinnerPanel("Genome Length:", genomeLenSpinner));
        panel.add(createSpinnerPanel("Elitism Count:", elitismSpinner));

        panel.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        panel.add(createSectionLabel("Adaptive Mutation Rates"));
        panel.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        
        adaptiveRatesTextField = new JTextField("0.01, 0.1, 0.5, 1.0");
        adaptiveRatesTextField.setBackground(CARD_COLOR);
        adaptiveRatesTextField.setForeground(TEXT_COLOR);
        adaptiveRatesTextField.setCaretColor(ACCENT_COLOR);
        adaptiveRatesTextField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(50, 60, 70)),
            new EmptyBorder(5, 5, 5, 5)
        ));
        adaptiveRatesTextField.setMaximumSize(new Dimension(260, 30));
        adaptiveRatesTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(adaptiveRatesTextField);

        panel.add(Box.createRigidArea(new Dimension(0, SECTION_GAP)));

        // --- SELECTION STRATEGY ---
        panel.add(createSectionLabel("Selection Strategy"));
        panel.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        String[] selectionStrategies = {"Tournament", "Rank", "Roulette"};
        createStrategySelector(panel, selectionStrategies, selectedSelectionStrategy, "Selection", (selection) -> {
            selectedSelectionStrategy = selection;
            selectionDisplayButton.setText(selection);
            tournamentPanel.setVisible("Tournament".equals(selection));
        });
        tournamentPanel = createTournamentPanel();
        panel.add(tournamentPanel);
        
        panel.add(Box.createRigidArea(new Dimension(0, SECTION_GAP)));

        // --- CROSSOVER STRATEGY ---
        panel.add(createSectionLabel("Crossover Operator"));
        panel.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        String[] crossoverStrategies = {"Multi-Point", "Uniform"};
        createStrategySelector(panel, crossoverStrategies, selectedCrossoverStrategy, "Crossover", (selection) -> {
            selectedCrossoverStrategy = selection;
            crossoverDisplayButton.setText(selection);
            multiPointPanel.setVisible("Multi-Point".equals(selection));
            uniformPanel.setVisible("Uniform".equals(selection));
        });
        multiPointPanel = createMultiPointCrossoverPanel();
        uniformPanel = createUniformCrossoverPanel();
        panel.add(multiPointPanel);
        panel.add(uniformPanel);

        panel.add(Box.createRigidArea(new Dimension(0, SECTION_GAP)));

        // --- INITIAL STRATEGY ---
        panel.add(createSectionLabel("Initial Population"));
        panel.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        String[] initialStrategies = {"Heuristic", "Hybrid", "Random"};
        createStrategySelector(panel, initialStrategies, selectedInitialStrategy, "Initial", (selection) -> {
            selectedInitialStrategy = selection;
            initialDisplayButton.setText(selection);
        });

        panel.add(Box.createRigidArea(new Dimension(0, SECTION_GAP)));

        // --- FITNESS STRATEGY ---
        panel.add(createSectionLabel("Fitness Evaluation"));
        panel.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        String[] fitnessStrategies = {"Hybrid", "Proximity", "Minimum Weight"};
        createStrategySelector(panel, fitnessStrategies, selectedFitnessStrategy, "Fitness", (selection) -> {
            selectedFitnessStrategy = selection;
            fitnessDisplayButton.setText(selection);
        });

        panel.add(Box.createRigidArea(new Dimension(0, SECTION_GAP)));

        // --- MUTATION STRATEGY ---
        panel.add(createSectionLabel("Mutation Operator"));
        panel.add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        String[] mutationStrategies = {"Inversion", "Fixed Count", "Random Reset", "Swap"};
        createStrategySelector(panel, mutationStrategies, selectedMutationStrategy, "Mutation", (selection) -> {
            selectedMutationStrategy = selection;
            mutationDisplayButton.setText(selection);
        });

        return panel;
    }
    
    private JPanel createTournamentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        tournamentKSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        panel.add(createSpinnerPanel("K Value:", tournamentKSpinner));
        return panel;
    }
    
    private JPanel createMultiPointCrossoverPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        crossoverRateSpinner = new JSpinner(new SpinnerNumberModel(0.8, 0.0, 1.0, 0.1));
        crossoverPointsSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        panel.add(createSpinnerPanel("Crossover Rate:", crossoverRateSpinner));
        panel.add(Box.createRigidArea(new Dimension(0,5)));
        panel.add(createSpinnerPanel("Number of Points:", crossoverPointsSpinner));
        return panel;
    }
    
    private JPanel createUniformCrossoverPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        mixingRatioSpinner = new JSpinner(new SpinnerNumberModel(0.5, 0.0, 1.0, 0.1));
        panel.add(createSpinnerPanel("Mixing Ratio:", mixingRatioSpinner));
        panel.setVisible(false);
        return panel;
    }

    private void createStrategySelector(JPanel parent, String[] strategies, String defaultStrategy, String type, Consumer<String> onSelect) {
        JButton displayButton;
        JPanel choicesPanel;

        displayButton = createStyledButton(defaultStrategy, e -> {
            // This is tricky because we have multiple choice panels
            // A better implementation would be needed for more complex scenarios
        });
        
        choicesPanel = new JPanel();
        choicesPanel.setLayout(new BoxLayout(choicesPanel, BoxLayout.Y_AXIS));
        choicesPanel.setOpaque(false);
        choicesPanel.setVisible(false);
        choicesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (String strategy : strategies) {
            choicesPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            JLabel choiceLabel = new JLabel(strategy);
            choiceLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            choiceLabel.setForeground(TEXT_COLOR);
            choiceLabel.setBorder(new EmptyBorder(8, 12, 8, 12));
            choiceLabel.setOpaque(true);
            choiceLabel.setBackground(CARD_COLOR);
            choiceLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            choiceLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onSelect.accept(strategy);
                    choicesPanel.setVisible(false);
                }
                @Override
                public void mouseEntered(MouseEvent e) { choiceLabel.setBackground(ACCENT_COLOR); }
                @Override
                public void mouseExited(MouseEvent e) { choiceLabel.setBackground(CARD_COLOR); }
            });
            choicesPanel.add(choiceLabel);
        }

        displayButton.addActionListener(e -> {
            choicesPanel.setVisible(!choicesPanel.isVisible());
        });
        
        if ("Selection".equals(type)) {
            selectionDisplayButton = displayButton;
            selectionChoicesPanel = choicesPanel;
        } else if ("Crossover".equals(type)) {
            crossoverDisplayButton = displayButton;
            crossoverChoicesPanel = choicesPanel;
        } else if ("Algorithm".equals(type)) {
            algorithmDisplayButton = displayButton;
            algorithmChoicesPanel = choicesPanel;
        } else if ("Initial".equals(type)) {
            initialDisplayButton = displayButton;
            initialChoicesPanel = choicesPanel;
        } else if ("Fitness".equals(type)) {
            fitnessDisplayButton = displayButton;
            fitnessChoicesPanel = choicesPanel;
        } else if ("Mutation".equals(type)) {
            mutationDisplayButton = displayButton;
            mutationChoicesPanel = choicesPanel;
        }
        
        parent.add(displayButton);
        parent.add(choicesPanel);
    }
    
    // ... UI helper methods
    private JLabel createSectionLabel(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setForeground(new Color(149, 165, 166));
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel createChoiceLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(TEXT_COLOR);
        label.setBorder(new EmptyBorder(8, 12, 8, 12));
        label.setOpaque(true);
        label.setBackground(CARD_COLOR);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedAlgorithm = text;
                algorithmDisplayButton.setText(selectedAlgorithm);
                algorithmChoicesPanel.setVisible(false);
                gaParamsPanel.setVisible("Genetic Algorithm".equals(selectedAlgorithm));
                revalidate();
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                ((JLabel)e.getSource()).setBackground(ACCENT_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ((JLabel)e.getSource()).setBackground(CARD_COLOR);
            }
        });
        return label;
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

    private JPanel createSpinnerPanel(String label, JSpinner spinner) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setForeground(TEXT_COLOR);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(lbl, BorderLayout.WEST);
        
        spinner.setBackground(CARD_COLOR);
        spinner.setForeground(TEXT_COLOR);
        spinner.setBorder(BorderFactory.createLineBorder(new Color(50, 60, 70), 1));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(CARD_COLOR);
            tf.setForeground(TEXT_COLOR);
            tf.setCaretColor(ACCENT_COLOR);
            tf.setBorder(new EmptyBorder(0, 5, 0, 5));
        }

        for (Component c : spinner.getComponents()) {
            if (c instanceof JButton) {
                JButton button = (JButton) c;
                button.setBackground(CARD_COLOR);
                button.setBorder(BorderFactory.createLineBorder(new Color(50, 60, 70), 1));
            }
        }

        panel.add(spinner, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(260, 30));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }
    
    // ... log, loadMap, runAlgorithm
    private void log(String message) {
        logArea.append(">> " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
        statusLabel.setText("Status: " + message);
    }

    private void loadMap() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("src\\code\\Maze"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                currentMaze = MazeParser.parseMaze(selectedFile);
                mazePanel.setMaze(currentMaze);
                String msg = "Loaded " + selectedFile.getName()
                        + "  [" + currentMaze.rows + " x " + currentMaze.cols + "]";
                log(msg);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage(),
                        "Load Error", JOptionPane.ERROR_MESSAGE);
                log("Error: " + ex.getMessage());
            }
        }
    }

    private void runAlgorithm(MazeSolver solver) {
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
            List<Cell> path = solver.solve();
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
