package mazerunner.strategies.initial;

import mazerunner.models.Cell;
import mazerunner.models.Maze;
import mazerunner.models.Walker;
import java.util.List;
import java.util.Random;

public class HybridInitial implements InitStrategy {

    public void initialize(List<Walker> population, Maze maze, int popSize, int geneLength) {
        population.clear();

        Random rand = new Random();
        Cell start = maze.start;
        Cell goal = maze.goal;

        int rowDiff = goal.row - start.row;
        int colDiff = goal.col - start.col;

        int verticalDir = (rowDiff > 0) ? 2 : 0;

        int horizontalDir = (colDiff > 0) ? 1 : 3;

        for (int i = 0; i < popSize; i++) {
            int[] genes = new int[geneLength];

            boolean isExplorer = i < (popSize * 0.2);

            for (int j = 0; j < genes.length; j++) {
                if (isExplorer) {
                    genes[j] = rand.nextInt(4);
                } else {
                    double chance = rand.nextDouble();

                    if (chance < 0.5) {
                        genes[j] = verticalDir;
                    } else if (chance < 0.9) {
                        genes[j] = horizontalDir;
                    } else {
                        genes[j] = rand.nextInt(4);
                    }
                }
            }

            Walker w = new Walker(genes);
            population.add(w);
        }
    }
}
