package mazerunner.strategies.crossover;

import mazerunner.models.Walker;

public interface CrossOverStrategy {

    Walker[] crossover(Walker parent1, Walker parent2);
}
