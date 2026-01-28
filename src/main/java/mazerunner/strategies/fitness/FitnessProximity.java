package mazerunner.strategies.fitness;

import mazerunner.algorithms.Genetic.PathDecoder;
import mazerunner.models.Maze;
import mazerunner.models.Walker;

public class FitnessProximity implements FitnessStrategy {

    public void calculate(Walker w, Maze maze) {
        PathDecoder find = new PathDecoder();
        find.solve(maze, w);

        double fitness;

        if (find.isGoal) {
            // โหดๆเอาไปหมื่น
            fitness = 10000.0;

        } else {
            // จอกเอาไปน้อยๆ
            fitness = 1.0 / (find.distance + 1.0);
        }
        w.setFitness(fitness);
    }
}
