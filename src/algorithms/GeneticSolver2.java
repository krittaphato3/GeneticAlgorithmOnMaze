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

    private int userPop = -1;
    private int userGen = -1;
    private int userLen = -1;
    private double userMutation = 0.03;
    private int userElitism = 50;

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
        return "GA (DOD/SoA Optimized)";
    }

    @Override
    public List<Cell> solve(Maze maze) {
        int mapArea = maze.rows * maze.cols;
        POPULATION_SIZE = (userPop > 0) ? userPop : Math.min(6000, Math.max(2000, mapArea * 4));
        MAX_GENERATIONS = (userGen > 0) ? userGen : 3000;
        GENOME_LENGTH = (userLen > 0) ? userLen : Math.min(10000, mapArea * 3);

        int paddedRows = maze.rows + 2;
        int paddedCols = maze.cols + 2;
        int totalSize = paddedRows * paddedCols;
        boolean[] wallMap = new boolean[totalSize];
        int[] weightMap = new int[totalSize];
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
        byte[] currentGenes = new byte[POPULATION_SIZE * GENOME_LENGTH];
        byte[] nextGenes = new byte[POPULATION_SIZE * GENOME_LENGTH];
        double[] fitness = new double[POPULATION_SIZE];
        int[] validSteps = new int[POPULATION_SIZE];
        boolean[] reachedGoal = new boolean[POPULATION_SIZE];

        initializeGenes(currentGenes, startIdx, goalIdx, paddedCols);

        byte[] bestGenes = new byte[GENOME_LENGTH];
        double bestFitness = -Double.MAX_VALUE;
        boolean bestReached = false;
        int bestValidSteps = 0;

        int stagnation = 0;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {

            final byte[] genesRef = currentGenes;

            IntStream.range(0, POPULATION_SIZE).parallel().forEach(i -> 
                evaluateInd(i, genesRef, fitness, validSteps, reachedGoal, 
                            wallMap, weightMap, moveOffsets, startIdx, goalIdx, paddedCols)
            );

            int bestIdx = 0;
            for (int i = 1; i < POPULATION_SIZE; i++) {
                if (fitness[i] > fitness[bestIdx]) bestIdx = i;
            }

            if (fitness[bestIdx] > bestFitness) {
                System.arraycopy(currentGenes, bestIdx * GENOME_LENGTH, bestGenes, 0, GENOME_LENGTH);
                bestFitness = fitness[bestIdx];
                bestReached = reachedGoal[bestIdx];
                bestValidSteps = validSteps[bestIdx];
                stagnation = 0;
            } else {
                stagnation++;
            }

            if (bestReached && stagnation > 200) break;
            final byte[] nextGenesRef = nextGenes;
            final int elites = Math.min(userElitism, POPULATION_SIZE/2);
            System.arraycopy(bestGenes, 0, nextGenesRef, 0, GENOME_LENGTH);
            IntStream.range(1, POPULATION_SIZE).parallel().forEach(i -> {
                ThreadLocalRandom rand = ThreadLocalRandom.current();
                int p1 = tournamentSelect(fitness, rand);
                int p2 = tournamentSelect(fitness, rand);
                breed(p1, p2, i, genesRef, nextGenesRef, rand, userMutation);
            });
            byte[] temp = currentGenes;
            currentGenes = nextGenes;
            nextGenes = temp;
        }

        return reconstructPathSmart(bestGenes, bestValidSteps, maze);
    }

    private void evaluateInd(int idx, byte[] genes, double[] fitness, int[] stepsOut, boolean[] reachedOut,
                             boolean[] walls, int[] weights, int[] offsets, int start, int goal, int width) {
        
        int curr = start;
        int cost = 0;
        int usedGenes = 0;
        int offset = idx * GENOME_LENGTH;
        boolean hit = false;

        for (int i = 0; i < GENOME_LENGTH; i++) {
            int next = curr + offsets[genes[offset + i]];
            
            if (!walls[next]) {
                curr = next;
                cost += weights[next];
                usedGenes = i + 1;
                
                if (curr == goal) {
                    hit = true;
                    fitness[idx] = 100_000_000.0 - cost;
                    break;
                }
            }
        }

        stepsOut[idx] = usedGenes;
        reachedOut[idx] = hit;
        
        if (!hit) {
            int r = curr / width, c = curr % width;
            int gr = goal / width, gc = goal % width;
            double distSq = (r - gr)*(r - gr) + (c - gc)*(c - gc);
            fitness[idx] = -distSq;
        }
    }

    private void breed(int p1, int p2, int childIdx, byte[] src, byte[] dst, ThreadLocalRandom rand, double mutation) {
        int o1 = p1 * GENOME_LENGTH;
        int o2 = p2 * GENOME_LENGTH;
        int oChild = childIdx * GENOME_LENGTH;
        
        int mid = rand.nextInt(GENOME_LENGTH);
        System.arraycopy(src, o1, dst, oChild, mid);
        System.arraycopy(src, o2 + mid, dst, oChild + mid, GENOME_LENGTH - mid);

        if (mutation > 0) {
            double logInv = Math.log(1.0 - mutation);
            int k = 0;
            while (k < GENOME_LENGTH) {
                double r = rand.nextDouble();
                if (r==0) r=0.0000001;
                k += (int)(Math.log(r)/logInv);
                if (k < GENOME_LENGTH) {
                    dst[oChild + k] = (byte)rand.nextInt(4);
                    k++;
                }
            }
        }
    }

    private int tournamentSelect(double[] fitness, ThreadLocalRandom rand) {
        int best = rand.nextInt(POPULATION_SIZE);
        for (int i = 0; i < TOURNAMENT_SIZE - 1; i++) {
            int c = rand.nextInt(POPULATION_SIZE);
            if (fitness[c] > fitness[best]) best = c;
        }
        return best;
    }

    private void initializeGenes(byte[] genes, int start, int goal, int width) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int sr = start/width, sc = start%width;
        int gr = goal/width, gc = goal%width;
        int bias1 = (gr > sr) ? 1 : 0;
        int bias2 = (gc > sc) ? 3 : 2;

        for (int i = 0; i < POPULATION_SIZE; i++) {
            int offset = i * GENOME_LENGTH;
            boolean guided = i < (POPULATION_SIZE * 0.7);
            for (int j = 0; j < GENOME_LENGTH; j++) {
                if (guided) {
                    double r = rand.nextDouble();
                    if (r < 0.4) genes[offset+j] = (byte)bias1;
                    else if (r < 0.8) genes[offset+j] = (byte)bias2;
                    else genes[offset+j] = (byte)rand.nextInt(4);
                } else {
                    genes[offset+j] = (byte)rand.nextInt(4);
                }
            }
        }
    }

    private List<Cell> reconstructPathSmart(byte[] genes, int validLen, Maze maze) {
        List<Cell> rawPath = new ArrayList<>();
        int r = maze.start.row, c = maze.start.col;
        rawPath.add(maze.grid[r][c]);
        
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        for (int i = 0; i < GENOME_LENGTH; i++) {
            if (i >= validLen) break; 
            int nr = r + dr[genes[i]];
            int nc = c + dc[genes[i]];
            if (maze.isValid(nr, nc)) {
                r = nr; c = nc;
                Cell cell = maze.grid[r][c];
                rawPath.add(cell);
                if (cell.isGoal) break;
            }
        }

        // Loop Erasure
        List<Cell> clean = new ArrayList<>();
        Set<Cell> vis = new HashSet<>();
        for (Cell cell : rawPath) {
            if (vis.contains(cell)) {
                while (!clean.isEmpty()) {
                    Cell top = clean.get(clean.size()-1);
                    if (top == cell) break;
                    clean.remove(clean.size()-1);
                    vis.remove(top);
                }
            } else {
                clean.add(cell);
                vis.add(cell);
            }
        }
        return clean;
    }
}