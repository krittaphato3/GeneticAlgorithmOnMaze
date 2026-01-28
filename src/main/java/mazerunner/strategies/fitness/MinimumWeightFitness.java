package mazerunner.strategies.fitness;
import mazerunner.models.Cell;
import mazerunner.models.Maze;
import mazerunner.models.Walker;

public class MinimumWeightFitness implements FitnessStrategy {

    public void calculate(Walker walker, Maze maze) {
        int currentRow = maze.start.row;
        int currentCol = maze.start.col;

        int totalWeight = 0;
        boolean reachedGoal = false;

        int[] dRow = {-1, 0, 1, 0};
        int[] dCol = {0, 1, 0, -1};

        int[] genes = walker.getGenes();
        for (int gene : genes) {
            int direction = gene % 4;

            int nextRow = currentRow + dRow[direction];
            int nextCol = currentCol + dCol[direction];

            if (maze.isValid(nextRow, nextCol)) {
                currentRow = nextRow;
                currentCol = nextCol;

                Cell currentCell = maze.grid[currentRow][currentCol];
                totalWeight += currentCell.weight;

                if (currentCell.isGoal) {
                    reachedGoal = true;
                    break;
                }
            }
        }

        double fitness;

        if (reachedGoal) {

            double goalBonus = 100000.0;
            fitness = goalBonus - totalWeight;
        } else {
            int distance = Math.abs(currentRow - maze.goal.row)
                    + Math.abs(currentCol - maze.goal.col);

            fitness = 100.0 / (distance + 1);
        }

        walker.setFitness(Math.max(0, fitness));
    }
}
