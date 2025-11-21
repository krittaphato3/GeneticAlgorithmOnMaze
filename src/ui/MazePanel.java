package ui;

import models.Cell;
import models.Maze;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class MazePanel extends JPanel {

    private Maze maze;
    private List<Cell> path;
    private Cell hoveredCell;

    private double zoomFactor = 1.0;
    private double viewX = 0;
    private double viewY = 0;
    private Point lastMousePosition;

    private static final Color BG_COLOR = new Color(30, 30, 30);
    private static final Color WALL_COLOR = new Color(10, 10, 10);
    private static final Color PATH_COLOR = new Color(230, 230, 230);
    private static final Color START_COLOR = new Color(46, 204, 113);
    private static final Color GOAL_COLOR = new Color(231, 76, 60);
    private static final Color SOLUTION_COLOR = new Color(52, 152, 219, 200);
    private static final Color GRID_LINE_COLOR = new Color(0, 0, 0, 50);
    private static final Color WEIGHT_COLOR = new Color(0, 0, 0, 100);

    private static final int MARGIN = 20;
    private static final double ZOOM_STEP = 0.1;
    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 6.0;

    public MazePanel() {
        setBackground(BG_COLOR);
        setDoubleBuffered(true);

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
                if (maze == null || zoomFactor <= 1.0 || lastMousePosition == null) return;

                int dx = e.getX() - lastMousePosition.x;
                int dy = e.getY() - lastMousePosition.y;
                viewX += dx;
                viewY += dy;
                lastMousePosition = e.getPoint();
                repaint();
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        addMouseWheelListener(e -> {
            if (maze == null) return;

            int[] oldGrid = calculateGridGeometry(zoomFactor);
            int oldCellSize = oldGrid[0];
            if (oldCellSize <= 0) return;

            int oldXOffset = oldGrid[1] + (int) viewX;
            int oldYOffset = oldGrid[2] + (int) viewY;

            int mouseRelX = e.getX() - oldXOffset;
            int mouseRelY = e.getY() - oldYOffset;

            double delta = -e.getPreciseWheelRotation() * ZOOM_STEP;
            double newZoom = zoomFactor + delta;
            newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));

            if (newZoom == MIN_ZOOM) {
                zoomFactor = MIN_ZOOM;
                viewX = 0;
                viewY = 0;
                repaint();
                handleHover(e.getX(), e.getY());
                return;
            }

            int[] newGrid = calculateGridGeometry(newZoom);
            int newCellSize = newGrid[0];
            if (newCellSize <= 0) return;

            int newCenteredX = newGrid[1];
            int newCenteredY = newGrid[2];

            double scaleRatio = (double) newCellSize / oldCellSize;
            double newMouseRelX = mouseRelX * scaleRatio;
            double newMouseRelY = mouseRelY * scaleRatio;

            double targetXOffset = e.getX() - newMouseRelX;
            double targetYOffset = e.getY() - newMouseRelY;

            zoomFactor = newZoom;
            viewX = targetXOffset - newCenteredX;
            viewY = targetYOffset - newCenteredY;

            repaint();
            handleHover(e.getX(), e.getY());
        });
    }

    public void setMaze(Maze maze) {
        this.maze = maze;
        this.path = null;
        this.hoveredCell = null;
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

        if (cellSize <= 0) {
            hoveredCell = null;
            setToolTipText(null);
            return;
        }

        int c = (mouseX - xOffset) / cellSize;
        int r = (mouseY - yOffset) / cellSize;

        if (r >= 0 && r < maze.rows && c >= 0 && c < maze.cols) {
            Cell newHover = maze.grid[r][c];
            if (newHover != hoveredCell) {
                hoveredCell = newHover;
                setToolTipText(String.format(
                        "Pos: (%d, %d) | Weight: %d | Type: %s",
                        r, c, newHover.weight, getCellType(newHover)
                ));
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

    private int[] calculateGridGeometry(double zoom) {
        if (maze == null) return new int[]{0, 0, 0};

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        int playableWidth = Math.max(1, panelWidth - (MARGIN * 2));
        int playableHeight = Math.max(1, panelHeight - (MARGIN * 2));

        int baseCellWidth = playableWidth / Math.max(1, maze.cols);
        int baseCellHeight = playableHeight / Math.max(1, maze.rows);
        int baseCellSize = Math.max(1, Math.min(baseCellWidth, baseCellHeight));

        int cellSize = (int) Math.max(1, baseCellSize * zoom);

        int totalMazeWidth = maze.cols * cellSize;
        int totalMazeHeight = maze.rows * cellSize;

        int centeredX = (panelWidth - totalMazeWidth) / 2;
        int centeredY = (panelHeight - totalMazeHeight) / 2;

        return new int[]{cellSize, centeredX, centeredY};
    }

    private int[] calculateFinalMetrics() {
        int[] geo = calculateGridGeometry(this.zoomFactor);
        int cellSize = geo[0];
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

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int[] metrics = calculateFinalMetrics();
        int cellSize = metrics[0];
        int xOffset = metrics[1];
        int yOffset = metrics[2];

        if (cellSize <= 0) {
            g2.dispose();
            drawPlaceholder(g);
            return;
        }

        int startCol = Math.max(0, -xOffset / cellSize);
        int endCol = Math.min(maze.cols, (getWidth() - xOffset) / cellSize + 1);
        int startRow = Math.max(0, -yOffset / cellSize);
        int endRow = Math.min(maze.rows, (getHeight() - yOffset) / cellSize + 1);

        Font weightFont = null;
        FontMetrics weightFm = null;
        boolean drawWeights = cellSize > 20;

        if (drawWeights) {
            int fontSize = Math.max(10, Math.min(16, cellSize / 2));
            weightFont = new Font("SansSerif", Font.PLAIN, fontSize);
            g2.setFont(weightFont);
            weightFm = g2.getFontMetrics();
        }

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
                g2.setColor(GRID_LINE_COLOR);
                g2.drawRect(x, y, cellSize, cellSize);

                if (!cell.isWall && drawWeights) {
                    String s = String.valueOf(cell.weight);
                    g2.setColor(WEIGHT_COLOR);
                    int textX = x + (cellSize - weightFm.stringWidth(s)) / 2;
                    int textY = y + (cellSize + weightFm.getAscent()) / 2 - 2;
                    g2.drawString(s, textX, textY);
                }
            }
        }

        if (path != null && !path.isEmpty()) {
            g2.setColor(SOLUTION_COLOR);
            int stroke = Math.max(3, cellSize / 3);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            java.awt.geom.GeneralPath polyline = new java.awt.geom.GeneralPath();
            Cell start = path.get(0);
            polyline.moveTo(
                    xOffset + start.col * cellSize + cellSize / 2.0,
                    yOffset + start.row * cellSize + cellSize / 2.0
            );

            for (int i = 1; i < path.size(); i++) {
                Cell next = path.get(i);
                polyline.lineTo(
                        xOffset + next.col * cellSize + cellSize / 2.0,
                        yOffset + next.row * cellSize + cellSize / 2.0
                );
            }
            g2.draw(polyline);
        }

        if (zoomFactor > 1.0) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.drawString(String.format("Zoom: %.1fx", zoomFactor), 20, getHeight() - 20);
        }

        g2.dispose();
    }

    private void drawPlaceholder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(BG_COLOR);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        String msg = "Load a maze from the left panel to begin";
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(msg)) / 2;
        int y = getHeight() / 2;
        g2.drawString(msg, x, y);
        g2.dispose();
    }
}
