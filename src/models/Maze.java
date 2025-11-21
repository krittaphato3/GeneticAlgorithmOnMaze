package models;

public class Maze {
    public Cell[][] grid;
    public int rows;
    public int cols;
    public Cell start;
    public Cell goal;

    public Maze(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new Cell[rows][cols];
    }

    public boolean isValid(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols && !grid[r][c].isWall;
    }
}