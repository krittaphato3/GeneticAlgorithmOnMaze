package mazerunner.algorithms.Genetic;

import mazerunner.models.*;
import java.util.ArrayList;
import java.util.List;

public class PathDecoder {

    public boolean isGoal = false;
    public int weight;
    public int distance;

    public List<Cell> pathHistory = new ArrayList<>();

    public void solve(Maze maze, Walker walk) {
        this.weight = 0;
        this.isGoal = false;

        Cell currentCell = maze.start;
        int currentRow = currentCell.row;
        int currentCol = currentCell.col;

        pathHistory.add(currentCell);

        for (int direction : walk.getGenes()) {
            int nextRow = currentRow;
            int nextCol = currentCol;

            switch (direction) {
                case 0:
                    nextRow--;
                    break; // N
                case 1:
                    nextCol++;
                    break; // E
                case 2:
                    nextRow++;
                    break; // S 
                case 3:
                    nextCol--;
                    break; // W
                default:
                    break;
            }

            if (maze.isValid(nextRow, nextCol)) {
                currentRow = nextRow;
                currentCol = nextCol;
                currentCell = maze.grid[currentRow][currentCol];

                this.weight += currentCell.weight;

                pathHistory.add(currentCell);

                if (currentCell.isGoal) {
                    this.isGoal = true;
                    break;
                }
            }
        }
        this.distance = Math.abs(currentRow - maze.goal.row) + Math.abs(currentCol - maze.goal.col);
    }
}
