package ui;

import models.Cell;
import models.Maze;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class MazePanel extends JPanel {

    private Maze maze;
    private List<Cell> path;
    private Cell hoveredCell;

    // --- Zoom & Pan State ---
    private double zoomFactor = 1.0;
    private double viewX = 0; // Pan Offset X
    private double viewY = 0; // Pan Offset Y
    private Point lastMousePosition;

    // --- Design Constants ---
    private final Color BG_COLOR = new Color(30, 30, 30);
    private final Color WALL_COLOR = new Color(10, 10, 10);
    private final Color PATH_COLOR = new Color(230, 230, 230);
    private final Color START_COLOR = new Color(46, 204, 113);
    private final Color GOAL_COLOR = new Color(231, 76, 60);
    private final Color SOLUTION_COLOR = new Color(52, 152, 219, 200);

    public MazePanel() {
        setBackground(BG_COLOR);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleHover(e.getX(), e.getY());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePosition = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (zoomFactor > 1.0 && lastMousePosition != null) {
                    int dx = e.getX() - lastMousePosition.x;
                    int dy = e.getY() - lastMousePosition.y;
                    viewX += dx;
                    viewY += dy;
                    lastMousePosition = e.getPoint();
                    repaint();
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        // --- ADVANCED ZOOM LOGIC ---
        addMouseWheelListener(e -> {
            if (maze == null) return;

            // 1. Capture State BEFORE Zoom
            // Calculate where the grid is currently drawn
            int[] oldGrid = calculateGridGeometry(zoomFactor);
            int oldCellSize = oldGrid[0];
            int oldXOffset = oldGrid[1] + (int) viewX;
            int oldYOffset = oldGrid[2] + (int) viewY;

            // Calculate mouse position relative to the grid (in pixels)
            int mouseRelX = e.getX() - oldXOffset;
            int mouseRelY = e.getY() - oldYOffset;

            // 2. Update Zoom Factor
            double delta = e.getPreciseWheelRotation() * -0.1;
            double newZoom = zoomFactor + delta;
            newZoom = Math.max(1.0, newZoom); // Clamp to 1.0 minimum

            // Special Case: If resetting to 1.0, snap to center
            if (newZoom == 1.0) {
                zoomFactor = 1.0;
                viewX = 0;
                viewY = 0;
                repaint();
                handleHover(e.getX(), e.getY());
                return;
            }

            // 3. Capture State AFTER Zoom (Hypothetically)
            int[] newGrid = calculateGridGeometry(newZoom);
            int newCellSize = newGrid[0];
            int newCenteredX = newGrid[1];
            int newCenteredY = newGrid[2];

            // 4. Calculate required Pan (viewX/Y) to keep mouse fixed
            // The mouse should be at the same relative percentage of the grid
            double scaleRatio = (double) newCellSize / oldCellSize;
            
            double newMouseRelX = mouseRelX * scaleRatio;
            double newMouseRelY = mouseRelY * scaleRatio;

            // Derived formula: TargetOffset = MousePos - NewRelativePos
            double targetXOffset = e.getX() - newMouseRelX;
            double targetYOffset = e.getY() - newMouseRelY;

            // Set the new view offsets relative to the centered position
            this.zoomFactor = newZoom;
            this.viewX = targetXOffset - newCenteredX;
            this.viewY = targetYOffset - newCenteredY;

            repaint();
            handleHover(e.getX(), e.getY());
        });
    }

    public void setMaze(Maze maze) {
        this.maze = maze;
        this.path = null;
        this.zoomFactor = 1.0;
        this.viewX = 0;
        this.viewY = 0;
        repaint();
    }

    public void setPath(List<Cell> path) {
        this.path = path;
        repaint();
    }

    private void handleHover(int mouseX, int mouseY) {
        if (maze == null) return;
        
        int[] metrics = calculateFinalMetrics();
        int cellSize = metrics[0];
        int xOffset = metrics[1];
        int yOffset = metrics[2];

        int c = (mouseX - xOffset) / cellSize;
        int r = (mouseY - yOffset) / cellSize;

        if (maze.isValid(r, c) || (r >= 0 && r < maze.rows && c >= 0 && c < maze.cols)) {
            Cell newHover = maze.grid[r][c];
            if (newHover != hoveredCell) {
                hoveredCell = newHover;
                setToolTipText(String.format("Pos: (%d, %d) | Weight: %d | Type: %s",
                        r, c, newHover.weight, getCellType(newHover)));
            }
        } else {
            hoveredCell = null;
            setToolTipText(null);
        }
    }

    private String getCellType(Cell c) {
        if (c.isStart) return "START";
        if (c.isGoal) return "GOAL";
        if (c.isWall) return "WALL";
        return "PATH";
    }

    // --- HELPER: Calculates pure grid geometry (Size & Centered Position) ---
    // Returns [cellSize, centeredX, centeredY]
    // DOES NOT include viewX/viewY panning!
    private int[] calculateGridGeometry(double zoom) {
        if (maze == null) return new int[]{0, 0, 0};

        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int margin = 20;

        int playableWidth = panelWidth - (margin * 2);
        int playableHeight = panelHeight - (margin * 2);

        int baseCellWidth = playableWidth / maze.cols;
        int baseCellHeight = playableHeight / maze.rows;
        int baseCellSize = Math.max(1, Math.min(baseCellWidth, baseCellHeight));

        int cellSize = (int) (baseCellSize * zoom);
        
        int totalMazeWidth = maze.cols * cellSize;
        int totalMazeHeight = maze.rows * cellSize;
        
        int centeredX = (panelWidth - totalMazeWidth) / 2;
        int centeredY = (panelHeight - totalMazeHeight) / 2;

        return new int[]{cellSize, centeredX, centeredY};
    }

    // --- HELPER: Calculates final drawing metrics including Pan ---
    // Returns [cellSize, xOffset, yOffset]
    private int[] calculateFinalMetrics() {
        int[] geo = calculateGridGeometry(this.zoomFactor);
        int cellSize = geo[0];
        // Add the dynamic pan (viewX/viewY) to the centered coordinates
        int xOffset = geo[1] + (int) viewX;
        int yOffset = geo[2] + (int) viewY;
        return new int[]{cellSize, xOffset, yOffset};
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (maze == null) {
            drawPlaceholder(g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int[] metrics = calculateFinalMetrics();
        int cellSize = metrics[0];
        int xOffset = metrics[1];
        int yOffset = metrics[2];

        // Visibility Culling (Optimization)
        int startCol = Math.max(0, -xOffset / cellSize);
        int endCol = Math.min(maze.cols, (getWidth() - xOffset) / cellSize + 1);
        int startRow = Math.max(0, -yOffset / cellSize);
        int endRow = Math.min(maze.rows, (getHeight() - yOffset) / cellSize + 1);

        // Draw Grid
        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                Cell cell = maze.grid[r][c];
                int x = xOffset + c * cellSize;
                int y = yOffset + r * cellSize;

                if (cell.isWall) g2.setColor(WALL_COLOR);
                else if (cell.isStart) g2.setColor(START_COLOR);
                else if (cell.isGoal) g2.setColor(GOAL_COLOR);
                else g2.setColor(PATH_COLOR);

                g2.fillRect(x, y, cellSize, cellSize);
                g2.setColor(new Color(0, 0, 0, 50));
                g2.drawRect(x, y, cellSize, cellSize);

                if (!cell.isWall && cellSize > 20) {
                    g2.setColor(new Color(0, 0, 0, 100));
                    String s = String.valueOf(cell.weight);
                    FontMetrics fm = g2.getFontMetrics();
                    // Dynamic font size
                    int fontSize = Math.max(10, Math.min(14, cellSize/2));
                    g2.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
                    
                    int textX = x + (cellSize - fm.stringWidth(s)) / 2;
                    int textY = y + (cellSize + fm.getAscent()) / 2 - 2;
                    g2.drawString(s, textX, textY);
                }
            }
        }

        // Draw Path
        if (path != null && !path.isEmpty()) {
            g2.setColor(SOLUTION_COLOR);
            int stroke = Math.max(3, cellSize / 3);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            java.awt.geom.GeneralPath polyline = new java.awt.geom.GeneralPath();
            Cell start = path.get(0);
            polyline.moveTo(xOffset + start.col * cellSize + cellSize/2.0, 
                            yOffset + start.row * cellSize + cellSize/2.0);

            for (int i = 1; i < path.size(); i++) {
                Cell next = path.get(i);
                polyline.lineTo(xOffset + next.col * cellSize + cellSize/2.0, 
                                yOffset + next.row * cellSize + cellSize/2.0);
            }
            g2.draw(polyline);
        }
        
        // Draw Zoom Indicator
        if (zoomFactor > 1.0) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.drawString(String.format("Zoom: %.1fx", zoomFactor), 20, getHeight() - 20);
        }
    }

    private void drawPlaceholder(Graphics g) {
        g.setColor(Color.GRAY);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        String msg = "Please Load a Map to Begin";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
    }
}