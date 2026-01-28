package mazerunner.models;

public class Walker implements Comparable<Walker> {

    private int[] genes;
    private double fitness;

    public Walker(int[] genes) {
        this.genes = genes;
    }

    public Walker(int length) {
        this.genes = new int[length];
    }

    public Walker(Walker other) {
        this.genes = other.genes.clone();
        this.fitness = other.fitness;
    }

    public int[] getGenes() {
        return genes;
    }

    public void setGenes(int[] genes) {
        this.genes = genes;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    @Override
    public int compareTo(Walker other) {
        return Double.compare(other.fitness, this.fitness);
    }
}
