package models;

public class Cell {
    public int row;
    public int col;
    public int weight;
    public boolean isWall;
    public boolean isStart;
    public boolean isGoal;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.weight = 0;
        this.isWall = false;
    }

    @Override
    public String toString() {
        if (isWall) return "[#]";
        if (isStart) return "[S]";
        if (isGoal) return "[G]";
        return "[" + weight + "]";
    }
}