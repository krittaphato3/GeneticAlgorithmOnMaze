package mazerunner.strategies.mutation;

import mazerunner.models.Walker;
import java.util.Random;

public class SwapMutation implements MutationStrategy {

    private Random random = new Random();

    @Override
    public void mutate(Walker walker, double mutationRate) {

        if (random.nextDouble() <= mutationRate) {
            int[] genes = walker.getGenes();
            int length = genes.length;

            int pos1 = random.nextInt(length);
            int pos2 = random.nextInt(length);

            while (pos1 == pos2) {
                pos2 = random.nextInt(length);
            }

            int temp = genes[pos1];
            genes[pos1] = genes[pos2];
            genes[pos2] = temp;
            walker.setGenes(genes);
        }
    }
}
