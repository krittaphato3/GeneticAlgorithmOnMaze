package mazerunner.strategies.fitness;
import mazerunner.models.Maze;
import mazerunner.models.Walker;

public interface FitnessStrategy {

    void calculate(Walker walker, Maze maze);
}
