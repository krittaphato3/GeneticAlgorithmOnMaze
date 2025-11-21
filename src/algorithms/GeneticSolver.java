package algorithms;

import models.Cell;
import models.Maze;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class GeneticSolver implements PathSolver {

    // --- Tuned Parameters for Large Maps ---
    private int POPULATION_SIZE;
    private int MAX_GENERATIONS;
    private int GENOME_LENGTH;
    private static final double MUTATION_RATE = 0.04; // Slightly lower to keep good paths stable
    private static final int TOURNAMENT_SIZE = 6;     // Higher pressure to pick best parents

    // Lookup Tables
    private static final int[] dRow = {-1, 1, 0, 0};
    private static final int[] dCol = {0, 0, -1, 1};

    @Override
    public String getName() {
        return "GA (Deep Search)";
    }

    @Override
    public List<Cell> solve(Maze maze) {
        // 1. AUTO-SCALE PARAMETERS
        int mapArea = maze.rows * maze.cols;
        
        // Massive population for massive mazes
        POPULATION_SIZE = Math.min(5000, Math.max(1000, mapArea)); 
        
        // Give them enough steps to actually reach the end (2x area is a safe buffer)
        GENOME_LENGTH = Math.min(10000, mapArea * 2); 
        
        // More time to evolve
        MAX_GENERATIONS = 1000; 

        // 2. FLATTEN MAZE (Speed Optimization)
        boolean[] wallMap = new boolean[maze.rows * maze.cols];
        for (int r = 0; r < maze.rows; r++) {
            for (int c = 0; c < maze.cols; c++) {
                wallMap[r * maze.cols + c] = maze.grid[r][c].isWall;
            }
        }
        
        int startR = maze.start.row;
        int startC = maze.start.col;
        int goalR = maze.goal.row;
        int goalC = maze.goal.col;

        // 3. INITIALIZE
        List<Individual> population = initializePopulation(POPULATION_SIZE, GENOME_LENGTH, maze);
        Individual globalBest = null;

        // 4. EVOLUTION LOOP
        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            
            // Parallel Evaluation
            population.parallelStream().forEach(ind -> 
                evaluateFast(ind, wallMap, maze.rows, maze.cols, startR, startC, goalR, goalC)
            );

            // Find Gen Best
            Individual genBest = null;
            for (Individual ind : population) {
                if (genBest == null || ind.fitness > genBest.fitness) {
                    genBest = ind;
                }
            }

            // Update Global Best
            if (globalBest == null || genBest.fitness > globalBest.fitness) {
                globalBest = new Individual(genBest);
            }

            // Exit early if Goal Reached
            if (globalBest.reachedGoal) {
                // Optimization: If found, break immediately. 
                // For video demo, you can comment this out to see if it finds a SHORTER path later.
                break; 
            }

            // Breeding
            List<Individual> nextGen = new ArrayList<>(POPULATION_SIZE);
            nextGen.add(new Individual(globalBest)); // Elitism
            
            List<Individual> currentPop = population;

            // Parallel Breeding
            List<Individual> children = IntStream.range(0, POPULATION_SIZE - 1)
                .parallel()
                .mapToObj(i -> {
                    Individual p1 = tournamentSelection(currentPop);
                    Individual p2 = tournamentSelection(currentPop);
                    Individual child = uniformCrossover(p1, p2);
                    mutate(child);
                    return child;
                })
                .toList();
            
            nextGen.addAll(children);
            population = nextGen;
        }

        return reconstructPath(globalBest, maze);
    }

    // --- High-Performance Logic (No "Stuck" Check) ---
    private void evaluateFast(Individual ind, boolean[] wallMap, int rows, int cols, 
                              int startR, int startC, int goalR, int goalC) {
        
        int currR = startR;
        int currC = startC;
        int stepsTaken = 0;
        
        // Walk the full genome
        for (int i = 0; i < ind.genes.length; i++) {
            byte move = ind.genes[i];
            int nextR = currR + dRow[move];
            int nextC = currC + dCol[move];

            // Boundary Check
            if (nextR >= 0 && nextR < rows && nextC >= 0 && nextC < cols) {
                // Wall Check
                if (!wallMap[nextR * cols + nextC]) {
                    currR = nextR;
                    currC = nextC;
                    stepsTaken++;
                    
                    if (currR == goalR && currC == goalC) {
                        ind.reachedGoal = true;
                        ind.validStepsCount = stepsTaken;
                        break; 
                    }
                } 
                // REMOVED THE COLLISION "ELSE" BLOCK HERE
                // We just ignore the move if it hits a wall, but we KEEP TRYING.
            }
        }
        
        if (!ind.reachedGoal) ind.validStepsCount = stepsTaken;

        // Fitness Calculation
        int dist = Math.abs(currR - goalR) + Math.abs(currC - goalC);
        
        if (ind.reachedGoal) {
            // Priority 1: Reached Goal (Score > 1M)
            ind.fitness = 1_000_000.0 - ind.validStepsCount; 
        } else {
            // Priority 2: Get Close (Score < 10k)
            // We use distance squared to HEAVILY punish being far away
            // This pulls the population towards the goal like a magnet
            ind.fitness = 10_000.0 - (dist * dist); 
        }
    }

    // --- Standard Genetic Ops ---
    private Individual tournamentSelection(List<Individual> pop) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        Individual best = pop.get(rand.nextInt(pop.size()));
        for (int i = 0; i < TOURNAMENT_SIZE - 1; i++) {
            Individual contender = pop.get(rand.nextInt(pop.size()));
            if (contender.fitness > best.fitness) {
                best = contender;
            }
        }
        return best;
    }

    private Individual uniformCrossover(Individual p1, Individual p2) {
        Individual child = new Individual(p1.genes.length);
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < p1.genes.length; i++) {
            child.genes[i] = rand.nextBoolean() ? p1.genes[i] : p2.genes[i];
        }
        return child;
    }

    private void mutate(Individual ind) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < ind.genes.length; i++) {
            if (rand.nextDouble() < MUTATION_RATE) {
                ind.genes[i] = (byte) rand.nextInt(4);
            }
        }
    }

    private List<Individual> initializePopulation(int size, int length, Maze maze) {
        List<Individual> pop = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Individual ind = new Individual(length);
            ThreadLocalRandom rand = ThreadLocalRandom.current();
            
            // Smart Initialization: 50% Guided
            int biasDir = -1;
            if (rand.nextDouble() < 0.5) { 
                 int dR = maze.goal.row - maze.start.row;
                 int dC = maze.goal.col - maze.start.col;
                 if (Math.abs(dR) > Math.abs(dC)) biasDir = dR > 0 ? 1 : 0;
                 else biasDir = dC > 0 ? 3 : 2;
            }

            for (int j = 0; j < length; j++) {
                if (biasDir != -1 && rand.nextDouble() < 0.4) { // 40% bias strength
                    ind.genes[j] = (byte) biasDir;
                } else {
                    ind.genes[j] = (byte) rand.nextInt(4);
                }
            }
            pop.add(ind);
        }
        return pop;
    }

    private List<Cell> reconstructPath(Individual ind, Maze maze) {
        List<Cell> path = new ArrayList<>();
        int r = maze.start.row;
        int c = maze.start.col;
        path.add(maze.grid[r][c]);

        for (int i = 0; i < ind.genes.length; i++) {
            if (path.size() > ind.validStepsCount + 1) break;

            int nextR = r + dRow[ind.genes[i]];
            int nextC = c + dCol[ind.genes[i]];

            if (maze.isValid(nextR, nextC)) {
                r = nextR;
                c = nextC;
                path.add(maze.grid[r][c]);
                if (maze.grid[r][c].isGoal) break;
            }
        }
        return path;
    }

    private static class Individual implements Comparable<Individual> {
        byte[] genes;
        double fitness;
        boolean reachedGoal;
        int validStepsCount;

        public Individual(int length) {
            genes = new byte[length];
        }
        
        public Individual(Individual other) {
            this.genes = Arrays.copyOf(other.genes, other.genes.length);
            this.fitness = other.fitness;
            this.reachedGoal = other.reachedGoal;
            this.validStepsCount = other.validStepsCount;
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(other.fitness, this.fitness);
        }
    }
}