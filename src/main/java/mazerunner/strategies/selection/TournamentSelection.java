package mazerunner.strategies.selection;

import mazerunner.models.Walker;
import java.util.*;

public class TournamentSelection implements SelectionStrategy {

    private int k;

    public TournamentSelection(int k) {
        this.k = k;
    }

    public Walker select(List<Walker> population) {
        Walker best = null;
        Random rand = new Random();
        for (int i = 0; i < k; i++) {
            int randomIndex = rand.nextInt(population.size());
            Walker choose = population.get(randomIndex);
            if (best == null || choose.getFitness() > best.getFitness()) {
                best = choose;
            }
        }
        return best;
    }
}
