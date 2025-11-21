package models;

public class Cell {
    public int row;
    public int col;
    public int weight;      // The time cost (e.g., 10, 5)
    public boolean isWall;  // True if this is a wall (#)
    public boolean isStart; // True if (S)
    public boolean isGoal;  // True if (G)

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.weight = 0; // Default
        this.isWall = false;
    }

    // Useful for debugging
    @Override
    public String toString() {
        if (isWall) return "[#]";
        if (isStart) return "[S]";
        if (isGoal) return "[G]";
        return "[" + weight + "]";
    }
}