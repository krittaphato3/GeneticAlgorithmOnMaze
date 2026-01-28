package mazerunner.strategies.selection;

import mazerunner.models.Walker;
import java.util.*;

public class RouletteWheel implements SelectionStrategy {

    @Override
    public Walker select(List<Walker> population) {
        double sumFitness = 0;
        for (int i = 0; i < population.size(); i++) {
            sumFitness += population.get(i).getFitness();
        }
        Random rand = new Random();
        double final_rand = rand.nextDouble() * sumFitness;
        
        double cSum = 0;
        for (int i = 0; i < population.size(); i++) {
            cSum += population.get(i).getFitness();
            if (cSum >= final_rand) {
                return population.get(i);
            }
        }
        return population.get(population.size() - 1);
    }
}
