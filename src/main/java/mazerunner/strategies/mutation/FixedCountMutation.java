package mazerunner.strategies.mutation;

import mazerunner.models.Walker;
import java.util.Random;

public class FixedCountMutation implements MutationStrategy {

    public void mutate(Walker child, double mutationRate) {
        Random rand = new Random();
        int geneLength = child.getGenes().length;
        double numMutations = geneLength * mutationRate;
        for (int k = 0; k < numMutations; k++) {
            int pos = rand.nextInt(geneLength);
            child.getGenes()[pos] = rand.nextInt(4);
        }
    }
}
