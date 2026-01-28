package mazerunner.strategies.selection;

import mazerunner.models.Walker;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RankSelection implements SelectionStrategy {

    private Random random = new Random();

    public Walker select(List<Walker> population) {
        Collections.sort(population);

        int size = population.size();
        int totalRank = (size * (size + 1) / 2);

        int randomValue = random.nextInt(totalRank);

        int currentSum = 0;
        for (int i = 0; i < size; i++) {
            int rankSocre = size - 1;
            currentSum += rankSocre;

            if (currentSum >= randomValue) {
                return population.get(i);
            }
        }
        return population.get(0);
    }

}
