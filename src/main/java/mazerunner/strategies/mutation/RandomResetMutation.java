package mazerunner.strategies.mutation;

import mazerunner.models.Walker;
import java.util.Random;

public class RandomResetMutation implements MutationStrategy {

    public void mutate(Walker child, double mutationRate) {

        Random rand = new Random();
        int geneLength = child.getGenes().length;
        for (int i = 0; i < geneLength; i++) {
            if (rand.nextDouble() < mutationRate) {
                child.getGenes()[i] = rand.nextInt(4);
            }
        }
    }
}
