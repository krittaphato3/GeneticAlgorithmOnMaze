package mazerunner.strategies.initial;

import mazerunner.models.*;
import java.util.List;
import java.util.Random;

public class HeuristicInitial implements InitStrategy {

    public void initialize(List<Walker> population, Maze maze, int popSize, int geneLength) {
        population.clear();

        Cell start = maze.start;
        Cell goal = maze.goal;

        int rowDiff = goal.row - start.row;
        int colDiff = goal.col - start.col;

        for (int i = 0; i < popSize; i++) {
            int[] genes = new int[geneLength];
            Random rand = new Random();

            for (int j = 0; j < genes.length; j++) {
                double chance = rand.nextDouble();
                if (chance < 0.5) {
                    genes[j] = (rowDiff > 0) ? 2 : 0;
                } else if (chance < 0.8) {
                    genes[j] = (colDiff > 0) ? 1 : 3;
                } else {
                    genes[j] = rand.nextInt(4);
                }
            }
            Walker w = new Walker(genes);
            population.add(w);
        }

    }
}
