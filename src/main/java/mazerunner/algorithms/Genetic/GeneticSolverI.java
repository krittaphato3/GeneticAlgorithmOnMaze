package mazerunner.algorithms.Genetic;

import mazerunner.models.Cell;
import mazerunner.models.Maze;
import mazerunner.models.Walker;
import mazerunner.strategies.crossover.CrossOverStrategy;
import mazerunner.strategies.fitness.FitnessStrategy;
import mazerunner.strategies.initial.InitStrategy;
import mazerunner.strategies.mutation.MutationStrategy;
import mazerunner.strategies.selection.SelectionStrategy;
import java.util.ArrayList;
import java.util.List;

public class GeneticSolverI extends BaseGeneticSolver {

    private SelectionStrategy selection;
    private CrossOverStrategy crossover;
    private MutationStrategy mutation;
    private FitnessStrategy fitness;
    private InitStrategy initial;
    private int nElitism;
    private double[] adaptiveRates;

    public GeneticSolverI(Maze maze, int popSize, int generation, int geneLength,
            InitStrategy initial,
            FitnessStrategy fitness,
            SelectionStrategy selection,
            CrossOverStrategy crossover,
            MutationStrategy mutation,
            double[] adaptiveRates,
            int nElitism) {

        super(maze, popSize, generation, geneLength);
        this.selection = selection;
        this.crossover = crossover;
        this.mutation = mutation;
        this.fitness = fitness;
        this.initial = initial;
        this.adaptiveRates = adaptiveRates;
        this.nElitism = nElitism;
    }

    @Override
    public List<Cell> solve() {

        // Initial Population
        initial.initialize(population, maze, popSize, geneLength);

        int stagnationCount = 0;
        double lastBestFitness = 0;

        double mutationRate = adaptiveRates[0];

        for (int i = 0; i < generation; i++) {

            // Calculate Fitness
            for (Walker w : population) {
                fitness.calculate(w, maze);
            }

            List<Walker> childPopulation = new ArrayList<>();

            // Elitism
            List<Walker> eList = getBestSolution(nElitism);
            for (int j = 0; j < nElitism; j++) {
                Walker elite = eList.get(j);
                childPopulation.add(new Walker(elite));
            }

            //Check Best & Change Mutate Rate
            Walker currentBest = childPopulation.get(0);

            if (currentBest.getFitness() > lastBestFitness) {
                lastBestFitness = currentBest.getFitness();
                stagnationCount = 0;
                mutationRate = adaptiveRates[0];
            } else {
                stagnationCount++;
            }

            if (stagnationCount > 600) {
                mutationRate = adaptiveRates[3];
            } else if (stagnationCount > 300) {
                mutationRate = adaptiveRates[2];
            } else if (stagnationCount > 100) {
                mutationRate = adaptiveRates[1];
            } else {
                mutationRate = adaptiveRates[0];
            }

            if (i % 50 == 0) {
                System.out.println("Gen " + i + " | Best Fit: " + currentBest.getFitness() + " | Rate: " + String.format("%.2f", mutationRate));
            }

            while (childPopulation.size() < popSize) {

                // Selection
                Walker parent1 = selection.select(population);
                Walker parent2 = selection.select(population);

                // Crossover
                Walker[] children = crossover.crossover(parent1, parent2);

                for (Walker child : children) {
                    if (childPopulation.size() < popSize) {
                        // Mutation
                        mutation.mutate(child, mutationRate);
                        childPopulation.add(child);
                    }
                }
            }
            population = new ArrayList<>(childPopulation);
        }

        for (Walker w : population) {
            fitness.calculate(w, maze);
        }

        Walker best = getBestSolution();
        PathDecoder decoder = new PathDecoder();
        decoder.solve(maze, best);
        printDetail(decoder);

        return decoder.pathHistory;
    }

    @Override
    public String getName() {
        return "Genetic Algorithm V1";
    }
}
