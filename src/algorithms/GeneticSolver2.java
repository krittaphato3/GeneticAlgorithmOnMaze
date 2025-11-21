package algorithms;

import models.Cell;
import models.Maze;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class GeneticSolver2 implements PathSolver {

    private int POPULATION_SIZE;
    private int MAX_GENERATIONS;
    private int GENOME_LENGTH;
    private static final double MUTATION_RATE = 0.04;
    private static final double LOG_ONE_MINUS_RATE = Math.log(1.0 - MUTATION_RATE);
    private static final int TOURNAMENT_SIZE = 5;

    @Override
    public String getName() {
        return "GA (Data-Oriented / SoA)";
    }

    @Override
    public List<Cell> solve(Maze maze) {
        int mapArea = maze.rows * maze.cols;
        POPULATION_SIZE = Math.min(3000, Math.max(1000, mapArea));
        GENOME_LENGTH = Math.min(8000, mapArea * 2); 
        MAX_GENERATIONS = 1000;

        int paddedRows = maze.rows + 2;
        int paddedCols = maze.cols + 2;
        boolean[] wallMap = new boolean[paddedRows * paddedCols];
        Arrays.fill(wallMap, true);

        for (int r = 0; r < maze.rows; r++) {
            for (int c = 0; c < maze.cols; c++) {
                if (!maze.grid[r][c].isWall) {
                    wallMap[(r + 1) * paddedCols + (c + 1)] = false;
                }
            }
        }

        int startIdx = (maze.start.row + 1) * paddedCols + (maze.start.col + 1);
        int goalIdx = (maze.goal.row + 1) * paddedCols + (maze.goal.col + 1);
        int[] moveOffsets = {-paddedCols, paddedCols, -1, 1};

        // --- DATA ORIENTED DESIGN (SoA) ---
        // Instead of objects, we use giant primitive arrays (Structure of Arrays)
        // This keeps memory contiguous for CPU prefetching.
        
        byte[] currentGenes = new byte[POPULATION_SIZE * GENOME_LENGTH];
        byte[] nextGenes = new byte[POPULATION_SIZE * GENOME_LENGTH];
        double[] fitness = new double[POPULATION_SIZE];
        int[] validSteps = new int[POPULATION_SIZE];
        boolean[] reachedGoal = new boolean[POPULATION_SIZE];

        // Init Population
        initializeGenes(currentGenes, startIdx, goalIdx, paddedCols);

        // Global Best Tracker (Primitive fields)
        byte[] bestGenes = new byte[GENOME_LENGTH];
        double bestFitness = -1;
        int bestSteps = 0;
        boolean bestReached = false;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {

            // 1. Parallel Evaluation on contiguous memory
            final byte[] genesRef = currentGenes;
            IntStream.range(0, POPULATION_SIZE).parallel().forEach(i -> 
                evaluateInd(i, genesRef, fitness, validSteps, reachedGoal, 
                            wallMap, moveOffsets, startIdx, goalIdx, paddedCols)
            );

            // 2. Find Best (Linear Scan)
            int bestIdx = 0;
            for (int i = 1; i < POPULATION_SIZE; i++) {
                if (fitness[i] > fitness[bestIdx]) {
                    bestIdx = i;
                }
            }

            // 3. Update Global Best
            if (fitness[bestIdx] > bestFitness) {
                System.arraycopy(currentGenes, bestIdx * GENOME_LENGTH, bestGenes, 0, GENOME_LENGTH);
                bestFitness = fitness[bestIdx];
                bestReached = reachedGoal[bestIdx];
                bestSteps = validSteps[bestIdx];
            }

            if (bestReached) break;

            // 4. Parallel Breeding
            final byte[] nextGenesRef = nextGenes;
            
            // Elitism: Copy best directly to slot 0
            System.arraycopy(bestGenes, 0, nextGenesRef, 0, GENOME_LENGTH);
            
            IntStream.range(1, POPULATION_SIZE).parallel().forEach(i -> {
                ThreadLocalRandom rand = ThreadLocalRandom.current();
                
                // Tournament (Indices only)
                int p1 = tournamentSelect(fitness, rand);
                int p2 = tournamentSelect(fitness, rand);
                
                breed(p1, p2, i, genesRef, nextGenesRef, rand);
            });

            // 5. Swap Buffers
            byte[] temp = currentGenes;
            currentGenes = nextGenes;
            nextGenes = temp;
        }

        return reconstructPath(bestGenes, bestSteps, maze);
    }

    private void evaluateInd(int idx, byte[] genes, double[] fitness, int[] stepsOut, boolean[] reachedOut,
                             boolean[] wallMap, int[] moveOffsets, int startIdx, int goalIdx, int width) {
        
        int currIdx = startIdx;
        int steps = 0;
        int geneOffset = idx * GENOME_LENGTH;
        boolean hitGoal = false;

        for (int i = 0; i < GENOME_LENGTH; i++) {
            int nextIdx = currIdx + moveOffsets[genes[geneOffset + i]];
            
            if (!wallMap[nextIdx]) {
                currIdx = nextIdx;
                steps++;
                if (currIdx == goalIdx) {
                    hitGoal = true;
                    // Optimization: Early exit if goal hit
                    break; 
                }
            }
        }

        stepsOut[idx] = steps;
        reachedOut[idx] = hitGoal;
        
        if (hitGoal) {
            fitness[idx] = 1_000_000.0 - steps;
        } else {
            int currR = currIdx / width;
            int currC = currIdx % width;
            int goalR = goalIdx / width;
            int goalC = goalIdx % width;
            int dist = Math.abs(currR - goalR) + Math.abs(currC - goalC);
            fitness[idx] = 10_000.0 - (dist * dist);
        }
    }

    private void breed(int p1Idx, int p2Idx, int childIdx, byte[] source, byte[] dest, ThreadLocalRandom rand) {
        int p1Offset = p1Idx * GENOME_LENGTH;
        int p2Offset = p2Idx * GENOME_LENGTH;
        int childOffset = childIdx * GENOME_LENGTH;
        
        int mid = rand.nextInt(GENOME_LENGTH);
        
        // Block Copy Crossover
        System.arraycopy(source, p1Offset, dest, childOffset, mid);
        System.arraycopy(source, p2Offset + mid, dest, childOffset + mid, GENOME_LENGTH - mid);

        // Skip Mutation
        int index = 0;
        while (index < GENOME_LENGTH) {
            double r = rand.nextDouble();
            if (r == 0) r = 0.0000001;
            int jump = (int) (Math.log(r) / LOG_ONE_MINUS_RATE);
            index += jump;

            if (index < GENOME_LENGTH) {
                dest[childOffset + index] = (byte) rand.nextInt(4);
                index++;
            }
        }
    }

    private int tournamentSelect(double[] fitness, ThreadLocalRandom rand) {
        int best = rand.nextInt(POPULATION_SIZE);
        for (int i = 0; i < TOURNAMENT_SIZE - 1; i++) {
            int contender = rand.nextInt(POPULATION_SIZE);
            if (fitness[contender] > fitness[best]) {
                best = contender;
            }
        }
        return best;
    }

    private void initializeGenes(byte[] genes, int startIdx, int goalIdx, int width) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        
        int startR = startIdx / width;
        int startC = startIdx % width;
        int goalR = goalIdx / width;
        int goalC = goalIdx % width;
        
        int dR = goalR - startR;
        int dC = goalC - startC;
        int biasDir = -1;
        if (Math.abs(dR) > Math.abs(dC)) biasDir = dR > 0 ? 1 : 0;
        else biasDir = dC > 0 ? 3 : 2;

        for (int i = 0; i < POPULATION_SIZE; i++) {
            int offset = i * GENOME_LENGTH;
            boolean guided = i < POPULATION_SIZE / 2;
            for (int j = 0; j < GENOME_LENGTH; j++) {
                if (guided && rand.nextDouble() < 0.4) {
                    genes[offset + j] = (byte) biasDir;
                } else {
                    genes[offset + j] = (byte) rand.nextInt(4);
                }
            }
        }
    }

    private List<Cell> reconstructPath(byte[] genes, int stepCount, Maze maze) {
        List<Cell> path = new ArrayList<>();
        int r = maze.start.row;
        int c = maze.start.col;
        path.add(maze.grid[r][c]);

        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = {0, 0, -1, 1};

        for (int i = 0; i < GENOME_LENGTH; i++) {
            if (path.size() > stepCount + 1) break;
            
            int nextR = r + dRow[genes[i]];
            int nextC = c + dCol[genes[i]];

            if (maze.isValid(nextR, nextC)) {
                r = nextR;
                c = nextC;
                path.add(maze.grid[r][c]);
                if (maze.grid[r][c].isGoal) break;
            }
        }
        return path;
    }
}