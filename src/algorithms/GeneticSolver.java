package algorithms;

import models.Cell;
import models.Maze;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class GeneticSolver implements PathSolver {

    private int POPULATION_SIZE; 
    private int MAX_GENERATIONS;
    private int GENOME_LENGTH;
    
    // User Configuration Values
    private int userPop = -1;
    private int userGen = -1;
    private int userLen = -1;
    private double userMutation = 0.03; // Default
    private int userElitism = 50;       // Default

    private static final int TOURNAMENT_SIZE = 5;

    public void setParameters(int pop, int gen, int len, double mutation, int elitism) {
        this.userPop = pop;
        this.userGen = gen;
        this.userLen = len;
        this.userMutation = mutation;
        this.userElitism = elitism;
    }

    @Override
    public String getName() {
        return String.format("GA (Mut: %.2f, Elite: %d)", userMutation, userElitism);
    }

    @Override
    public List<Cell> solve(Maze maze) {
        int mapArea = maze.rows * maze.cols;
        
        // Apply Config
        POPULATION_SIZE = (userPop > 0) ? userPop : Math.min(6000, Math.max(2000, mapArea * 4));
        MAX_GENERATIONS = (userGen > 0) ? userGen : 3000;
        GENOME_LENGTH = (userLen > 0) ? userLen : Math.min(10000, mapArea * 3);
        
        // Flatten Map
        int paddedCols = maze.cols + 2;
        int size = (maze.rows + 2) * paddedCols;
        boolean[] wallMap = new boolean[size];
        int[] weightMap = new int[size];
        Arrays.fill(wallMap, true);

        for (int r = 0; r < maze.rows; r++) {
            for (int c = 0; c < maze.cols; c++) {
                int idx = (r + 1) * paddedCols + (c + 1);
                if (!maze.grid[r][c].isWall) {
                    wallMap[idx] = false;
                    weightMap[idx] = Math.max(1, maze.grid[r][c].weight); 
                }
            }
        }

        int startIdx = (maze.start.row + 1) * paddedCols + (maze.start.col + 1);
        int goalIdx = (maze.goal.row + 1) * paddedCols + (maze.goal.col + 1);
        int[] moveOffsets = {-paddedCols, paddedCols, -1, 1}; 

        // Initialize
        Individual[] population = new Individual[POPULATION_SIZE];
        Individual[] nextGen = new Individual[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = new Individual(GENOME_LENGTH);
            nextGen[i] = new Individual(GENOME_LENGTH);
        }

        initializePopulation(population, startIdx, goalIdx, paddedCols);
        Individual globalBest = new Individual(GENOME_LENGTH);
        globalBest.fitness = -Double.MAX_VALUE;

        int stagnation = 0;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            final Individual[] currentPop = population;

            // 1. Fitness (Use Cost-Based Logic)
            IntStream.range(0, POPULATION_SIZE).parallel().forEach(i ->
                evaluate(currentPop[i], wallMap, weightMap, moveOffsets, startIdx, goalIdx, paddedCols)
            );

            // 2. Sort
            Arrays.parallelSort(currentPop, (a, b) -> Double.compare(b.fitness, a.fitness));

            // 3. Track Best
            if (currentPop[0].fitness > globalBest.fitness) {
                System.arraycopy(currentPop[0].genes, 0, globalBest.genes, 0, GENOME_LENGTH);
                globalBest.fitness = currentPop[0].fitness;
                globalBest.reachedGoal = currentPop[0].reachedGoal;
                globalBest.validGenes = currentPop[0].validGenes;
                stagnation = 0;
            } else {
                stagnation++;
            }

            if (globalBest.reachedGoal && stagnation > 200) break;

            // 4. Elitism (Preserve top N based on user setting)
            final Individual[] nextPopRef = nextGen;
            int elitesToKeep = Math.min(userElitism, POPULATION_SIZE / 2); // Safety clamp
            
            for (int i = 0; i < elitesToKeep; i++) {
                System.arraycopy(currentPop[i].genes, 0, nextPopRef[i].genes, 0, GENOME_LENGTH);
                nextPopRef[i].fitness = currentPop[i].fitness;
            }

            // 5. Crossover & Mutation (Using userMutation rate)
            IntStream.range(elitesToKeep, POPULATION_SIZE).parallel().forEach(i -> {
                ThreadLocalRandom rand = ThreadLocalRandom.current();
                Individual p1 = tournamentSelect(currentPop, rand);
                Individual p2 = tournamentSelect(currentPop, rand);
                produceChild(p1, p2, nextPopRef[i], rand, userMutation);
            });

            Individual[] temp = population;
            population = nextGen;
            nextGen = temp;
        }

        return reconstructPath(globalBest, maze);
    }

    private void evaluate(Individual ind, boolean[] walls, int[] weights, int[] offsets, int start, int goal, int width) {
        int curr = start;
        int cost = 0;
        int steps = 0;
        
        for (int i = 0; i < GENOME_LENGTH; i++) {
            int next = curr + offsets[ind.genes[i]];
            if (!walls[next]) {
                curr = next;
                cost += weights[next];
                steps = i + 1;
                if (curr == goal) {
                    ind.reachedGoal = true;
                    ind.validGenes = steps;
                    // Maximize fitness = minimize cost
                    ind.fitness = 100_000_000.0 - cost;
                    return;
                }
            }
        }
        
        ind.reachedGoal = false;
        ind.validGenes = steps;
        // Heuristic penalty distance
        int r = curr / width, c = curr % width;
        int gr = goal / width, gc = goal % width;
        double distSq = (r - gr) * (r - gr) + (c - gc) * (c - gc);
        ind.fitness = -distSq;
    }

    private void produceChild(Individual p1, Individual p2, Individual child, ThreadLocalRandom rand, double mutationRate) {
        int mid = rand.nextInt(GENOME_LENGTH);
        System.arraycopy(p1.genes, 0, child.genes, 0, mid);
        System.arraycopy(p2.genes, mid, child.genes, mid, GENOME_LENGTH - mid);

        // Geometric Skip Mutation
        if (mutationRate > 0.0) {
            double logInv = Math.log(1.0 - mutationRate);
            int idx = 0;
            while (idx < GENOME_LENGTH) {
                double r = rand.nextDouble();
                if (r == 0) r = 0.0000001;
                int jump = (int) (Math.log(r) / logInv);
                idx += jump;
                if (idx < GENOME_LENGTH) {
                    child.genes[idx] = (byte) rand.nextInt(4);
                    idx++;
                }
            }
        }
    }

    private Individual tournamentSelect(Individual[] pop, ThreadLocalRandom rand) {
        Individual best = pop[rand.nextInt(POPULATION_SIZE)];
        for (int i = 0; i < TOURNAMENT_SIZE - 1; i++) {
            Individual contender = pop[rand.nextInt(POPULATION_SIZE)];
            if (contender.fitness > best.fitness) best = contender;
        }
        return best;
    }

    private void initializePopulation(Individual[] pop, int start, int goal, int width) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int sr = start/width, sc = start%width;
        int gr = goal/width, gc = goal%width;
        int bias1 = (gr > sr) ? 1 : 0; // Vertical bias
        int bias2 = (gc > sc) ? 3 : 2; // Horizontal bias

        for (int i = 0; i < POPULATION_SIZE; i++) {
            boolean guided = i < (POPULATION_SIZE * 0.7);
            for (int j = 0; j < GENOME_LENGTH; j++) {
                if (guided) {
                    double r = rand.nextDouble();
                    if (r < 0.4) pop[i].genes[j] = (byte) bias1;
                    else if (r < 0.8) pop[i].genes[j] = (byte) bias2;
                    else pop[i].genes[j] = (byte) rand.nextInt(4);
                } else {
                    pop[i].genes[j] = (byte) rand.nextInt(4);
                }
            }
        }
    }

    private List<Cell> reconstructPath(Individual ind, Maze maze) {
        List<Cell> rawPath = new ArrayList<>();
        int r = maze.start.row, c = maze.start.col;
        rawPath.add(maze.grid[r][c]);
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        for (int i = 0; i < ind.genes.length; i++) {
            if (i >= ind.validGenes && ind.reachedGoal) break;
            int nr = r + dr[ind.genes[i]];
            int nc = c + dc[ind.genes[i]];
            if (maze.isValid(nr, nc)) {
                r = nr; c = nc;
                Cell cell = maze.grid[r][c];
                rawPath.add(cell);
                if (cell.isGoal) break;
            }
        }

        // Loop Erasure for Dijkstra-like path
        List<Cell> cleanPath = new ArrayList<>();
        Set<Cell> visited = new HashSet<>();
        for (Cell cell : rawPath) {
            if (visited.contains(cell)) {
                while (!cleanPath.isEmpty()) {
                    Cell top = cleanPath.get(cleanPath.size()-1);
                    if (top == cell) break;
                    cleanPath.remove(cleanPath.size()-1);
                    visited.remove(top);
                }
            } else {
                cleanPath.add(cell);
                visited.add(cell);
            }
        }
        return cleanPath;
    }

    private static class Individual {
        byte[] genes;
        double fitness;
        boolean reachedGoal;
        int validGenes;
        public Individual(int len) { genes = new byte[len]; }
    }
}