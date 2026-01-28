package mazerunner.algorithms.Genetic;

import mazerunner.algorithms.MazeSolver;
import mazerunner.models.Cell;
import mazerunner.models.Maze;
import mazerunner.models.Walker;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public abstract class BaseGeneticSolver implements MazeSolver {

    protected Maze maze;
    protected int popSize;
    protected int geneLength;
    protected int generation;

    protected List<Walker> population = new ArrayList<>();

    private PriorityQueue<Walker> q = new PriorityQueue<>((a, b) -> {
        return Double.compare(b.getFitness(), a.getFitness());
    });

    public BaseGeneticSolver(Maze maze, int popSize, int generation, int geneLength) {
        this.maze = maze;
        this.popSize = popSize;
        this.generation = generation;
        this.geneLength = geneLength;
    }

    @Override
    public abstract List<Cell> solve();

    @Override
    public abstract String getName();

    //For Elitism
    protected List<Walker> getBestSolution(int n) {
        q.clear();
        q.addAll(population);

        if (q.isEmpty()) {
            return null;
        }

        List<Walker> bestList = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            Walker best = q.poll();
            bestList.add(best);
        }
        return bestList;
    }

    //For final Best
    protected Walker getBestSolution() {
        if (population.isEmpty()) {
            return null;
        }

        Walker best = population.get(0);

        for (int i = 1; i < population.size(); i++) {
            Walker current = population.get(i);

            if (current.getFitness() > best.getFitness()) {
                best = current;
            }
        }
        return best;
    }

    protected void printDetail(PathDecoder decoder) {
        System.out.println("Goal Reached: " + decoder.isGoal + " | Weight: " + decoder.weight);
    }

}
