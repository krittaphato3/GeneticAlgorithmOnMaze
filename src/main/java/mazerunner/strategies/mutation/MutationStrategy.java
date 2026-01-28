package mazerunner.strategies.mutation;

import mazerunner.models.Walker;

public interface MutationStrategy {

    void mutate(Walker child, double mutationRate);
}
