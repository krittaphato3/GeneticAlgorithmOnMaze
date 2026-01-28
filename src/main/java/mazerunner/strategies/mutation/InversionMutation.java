package mazerunner.strategies.mutation;

import mazerunner.models.Walker;
import java.util.Random;

public class InversionMutation implements MutationStrategy {

    private Random random = new Random();

    public void mutate(Walker walker, double mutationRate) {

        if (random.nextDouble() <= mutationRate) {
            int[] genes = walker.getGenes();
            int length = genes.length;

            int point1 = random.nextInt(length);
            int point2 = random.nextInt(length);

            int start = Math.min(point1, point2);
            int end = Math.max(point1, point2);

            while (start < end) {
                int temp = genes[start];
                genes[start] = genes[end];
                genes[end] = temp;

                start++;
                end--;
            }
            walker.setGenes(genes);
        }
    }

}
