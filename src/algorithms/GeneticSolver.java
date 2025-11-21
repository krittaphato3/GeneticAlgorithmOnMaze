package algorithms;

import models.Cell;
import models.Maze;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class GeneticSolver implements PathSolver {

    // --- Hyper-Optimized Parameters ---
    private int POPULATION_SIZE;
    private int MAX_GENERATIONS;
    private int GENOME_LENGTH;
    private static final double MUTATION_RATE = 0.04;
    // Pre-calculate log for Skip Mutation: log(1 - rate)
    private static final double LOG_ONE_MINUS_RATE = Math.log(1.0 - MUTATION_RATE);
    private static final int TOURNAMENT_SIZE = 5;

    // LUTs
    private static final int[] dRow = {-1, 1, 0, 0};
    private static final int[] dCol = {0, 0, -1, 1};

    @Override
    public String getName() {
        return "GA (Skip-Mutation / Padding)";
    }

    @Override
    public List<Cell> solve(Maze maze) {
        // 1. SETUP
        int mapArea = maze.rows * maze.cols;
        POPULATION_SIZE = Math.min(3000, Math.max(1000, mapArea));
        GENOME_LENGTH = Math.min(8000, mapArea * 2); 
        MAX_GENERATIONS = 1000;

        // 2. PADDED WALL MAP (The Boundary Removal Trick)
        // We add a 1-block border of walls around the entire map.
        // This lets us remove "if (x < 0 || x >= width)" checks in the hot loop.
        int paddedRows = maze.rows + 2;
        int paddedCols = maze.cols + 2;
        boolean[] wallMap = new boolean[paddedRows * paddedCols];
        
        // Fill border with walls (true)
        Arrays.fill(wallMap, true);
        
        // Copy inner maze
        for (int r = 0; r < maze.rows; r++) {
            for (int c = 0; c < maze.cols; c++) {
                if (!maze.grid[r][c].isWall) {
                    // Offset by +1 row and +1 col
                    wallMap[(r + 1) * paddedCols + (c + 1)] = false;
                }
            }
        }
        
        // Adjust Start/Goal coordinates to padded space
        int startR = maze.start.row + 1;
        int startC = maze.start.col + 1;
        int goalR = maze.goal.row + 1;
        int goalC = maze.goal.col + 1;

        // 3. ZERO-ALLOC BUFFERS
        Individual[] population = new Individual[POPULATION_SIZE];
        Individual[] nextGen = new Individual[POPULATION_SIZE];

        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = new Individual(GENOME_LENGTH);
            nextGen[i] = new Individual(GENOME_LENGTH);
        }

        initializePopulation(population, startR, startC, goalR, goalC);
        
        Individual globalBest = new Individual(GENOME_LENGTH);
        globalBest.fitness = -1;

        // 4. MAIN LOOP
        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            
            final Individual[] currentPop = population;

            // A. Parallel Evaluation
            IntStream.range(0, POPULATION_SIZE).parallel().forEach(i -> 
                evaluateFast(currentPop[i], wallMap, paddedCols, startR, startC, goalR, goalC)
            );

            // B. Find Best (Linear Scan)
            Individual genBest = currentPop[0];
            for (int i = 1; i < POPULATION_SIZE; i++) {
                if (currentPop[i].fitness > genBest.fitness) {
                    genBest = currentPop[i];
                }
            }

            // C. Update Global Best
            if (genBest.fitness > globalBest.fitness) {
                System.arraycopy(genBest.genes, 0, globalBest.genes, 0, GENOME_LENGTH);
                globalBest.fitness = genBest.fitness;
                globalBest.reachedGoal = genBest.reachedGoal;
                globalBest.validStepsCount = genBest.validStepsCount;
            }

            if (globalBest.reachedGoal) break;

            // D. Parallel Breeding (With Skip Mutation)
            final Individual[] nextPopRef = nextGen;
            
            // Elitism
            System.arraycopy(globalBest.genes, 0, nextPopRef[0].genes, 0, GENOME_LENGTH);
            
            IntStream.range(1, POPULATION_SIZE).parallel().forEach(i -> {
                ThreadLocalRandom rand = ThreadLocalRandom.current();
                Individual p1 = tournamentSelect(currentPop, rand);
                Individual p2 = tournamentSelect(currentPop, rand);
                
                // Optimized Crossover + Skip Mutation
                produceChild(p1, p2, nextPopRef[i], rand);
            });

            Individual[] temp = population;
            population = nextGen;
            nextGen = temp;
        }

        return reconstructPath(globalBest, maze);
    }

    // --- Evaluation without Boundary Checks ---
    private void evaluateFast(Individual ind, boolean[] wallMap, int cols, 
                              int startR, int startC, int goalR, int goalC) {
        int currR = startR;
        int currC = startC;
        int steps = 0;
        
        // Direct array access is the fastest possible way to read memory
        // No getters, no bounds checks (wallMap handles borders)
        byte[] genes = ind.genes; 
        
        for (int i = 0; i < GENOME_LENGTH; i++) {
            byte move = genes[i];
            int nextR = currR + dRow[move];
            int nextC = currC + dCol[move];

            // Only check if it's NOT a wall. 
            // We removed "nextR >= 0 && nextR < rows" checks thanks to Padding!
            if (!wallMap[nextR * cols + nextC]) {
                currR = nextR;
                currC = nextC;
                steps++;
                if (currR == goalR && currC == goalC) {
                    ind.reachedGoal = true;
                    ind.validStepsCount = steps;
                    ind.fitness = 1_000_000.0 - steps; 
                    return;
                }
            }
        }
        
        ind.reachedGoal = false;
        ind.validStepsCount = steps;
        int dist = Math.abs(currR - goalR) + Math.abs(currC - goalC);
        ind.fitness = 10_000.0 - (dist * dist);
    }

    // --- Production Pipeline ---
    private void produceChild(Individual p1, Individual p2, Individual child, ThreadLocalRandom rand) {
        // 1. Crossover (System.arraycopy)
        int mid = rand.nextInt(GENOME_LENGTH);
        System.arraycopy(p1.genes, 0, child.genes, 0, mid);
        System.arraycopy(p2.genes, mid, child.genes, mid, GENOME_LENGTH - mid);

        // 2. Skip Mutation (The 96% Optimization)
        // Instead of checking every gene, we jump to the next mutated gene.
        // Formula: jump = log(random) / log(1 - rate)
        int index = 0;
        while (index < GENOME_LENGTH) {
            // How many to skip?
            double r = rand.nextDouble();
            // Math.log(0) is -Infinity, so guard against extremely rare 0.0
            if (r == 0) r = 0.0000001; 
            
            int jump = (int) (Math.log(r) / LOG_ONE_MINUS_RATE);
            index += jump;

            if (index < GENOME_LENGTH) {
                child.genes[index] = (byte) rand.nextInt(4);
                index++; // Move past this mutation
            }
        }
    }

    private Individual tournamentSelect(Individual[] pop, ThreadLocalRandom rand) {
        Individual best = pop[rand.nextInt(POPULATION_SIZE)];
        for (int i = 0; i < TOURNAMENT_SIZE - 1; i++) {
            Individual contender = pop[rand.nextInt(POPULATION_SIZE)];
            if (contender.fitness > best.fitness) {
                best = contender;
            }
        }
        return best;
    }

    private void initializePopulation(Individual[] pop, int startR, int startC, int goalR, int goalC) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int dR = goalR - startR;
        int dC = goalC - startC;
        int biasDir = -1;
        if (Math.abs(dR) > Math.abs(dC)) biasDir = dR > 0 ? 1 : 0;
        else biasDir = dC > 0 ? 3 : 2;

        for (int i = 0; i < POPULATION_SIZE; i++) {
            boolean guided = i < POPULATION_SIZE / 2;
            for (int j = 0; j < GENOME_LENGTH; j++) {
                if (guided && rand.nextDouble() < 0.4) {
                    pop[i].genes[j] = (byte) biasDir;
                } else {
                    pop[i].genes[j] = (byte) rand.nextInt(4);
                }
            }
        }
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

    private static class Individual {
        byte[] genes;
        double fitness;
        boolean reachedGoal;
        int validStepsCount;
        public Individual(int length) { genes = new byte[length]; }
    }
}