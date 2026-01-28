package mazerunner.strategies.initial;

import mazerunner.models.Maze;
import mazerunner.models.Walker;
import java.util.List;

public interface InitStrategy {

    void initialize(List<Walker> population, Maze maze, int popSize, int geneLength);
}
