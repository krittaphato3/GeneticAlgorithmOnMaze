package mazerunner.strategies.initial;

import mazerunner.models.*;
import java.util.List;
import java.util.Random;

public class RandomInitial implements InitStrategy {

    public void initialize(List<Walker> population, Maze maze, int popSize, int geneLength) {
        population.clear();
        for (int i = 0; i < popSize; i++) {
            int[] genes = new int[geneLength];
            Random rand = new Random();
            for (int j = 0; j < genes.length; j++) {
                genes[j] = rand.nextInt(4);
            }
            Walker w = new Walker(genes);
            population.add(w);
        }
    }
}
