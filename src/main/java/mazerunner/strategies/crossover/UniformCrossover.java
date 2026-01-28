package mazerunner.strategies.crossover;

import mazerunner.models.Walker;
import java.util.Random;

public class UniformCrossover implements CrossOverStrategy {

    private Random random = new Random();
    private double mixingRatio;

    public UniformCrossover(double mixingRatio) {
        this.mixingRatio = mixingRatio;
    }

    // default mixingration = 50/50
    public UniformCrossover() {
        this.mixingRatio = 0.5;
    }

    @Override
    public Walker[] crossover(Walker parent1, Walker parent2) {
        int[] p1Genes = parent1.getGenes();
        int[] p2Genes = parent2.getGenes();
        int length = p1Genes.length;

        int[] cGenes = new int[length];

        for (int i = 0; i < length; i++) {
            Random random = new Random();
            double final_random = random.nextDouble();
            if (final_random <= mixingRatio) {
                cGenes[i] = p1Genes[i];
            } else {
                cGenes[i] = p2Genes[i];
            }
        }

        Walker child = new Walker(cGenes);

        Walker[] children = new Walker[1];
        children[0] = child;

        return children;
    }
}
