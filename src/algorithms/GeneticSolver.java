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
    private static final double MUTATION_RATE = 0.04;
    private static final double LOG_ONE_MINUS_RATE = Math.log(1.0 - MUTATION_RATE);
    private static final int TOURNAMENT_SIZE = 5;

    @Override
    public String getName() {
        return "GA (Pure 1D Arithmetic)";
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

        Individual[] population = new Individual[POPULATION_SIZE];
        Individual[] nextGen = new Individual[POPULATION_SIZE];

        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = new Individual(GENOME_LENGTH);
            nextGen[i] = new Individual(GENOME_LENGTH);
        }

        initializePopulation(population, startIdx, goalIdx, paddedCols);

        Individual globalBest = new Individual(GENOME_LENGTH);
        globalBest.fitness = -1;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {

            final Individual[] currentPop = population;

            IntStream.range(0, POPULATION_SIZE).parallel().forEach(i ->
                evaluateFast(currentPop[i], wallMap, moveOffsets, startIdx, goalIdx, paddedCols)
            );

            Individual genBest = currentPop[0];
            for (int i = 1; i < POPULATION_SIZE; i++) {
                if (currentPop[i].fitness > genBest.fitness) {
                    genBest = currentPop[i];
                }
            }

            if (genBest.fitness > globalBest.fitness) {
                System.arraycopy(genBest.genes, 0, globalBest.genes, 0, GENOME_LENGTH);
                globalBest.fitness = genBest.fitness;
                globalBest.reachedGoal = genBest.reachedGoal;
                globalBest.validStepsCount = genBest.validStepsCount;
            }

            if (globalBest.reachedGoal) break;

            final Individual[] nextPopRef = nextGen;

            System.arraycopy(globalBest.genes, 0, nextPopRef[0].genes, 0, GENOME_LENGTH);

            IntStream.range(1, POPULATION_SIZE).parallel().forEach(i -> {
                ThreadLocalRandom rand = ThreadLocalRandom.current();
                Individual p1 = tournamentSelect(currentPop, rand);
                Individual p2 = tournamentSelect(currentPop, rand);
                produceChild(p1, p2, nextPopRef[i], rand);
            });

            Individual[] temp = population;
            population = nextGen;
            nextGen = temp;
        }

        return reconstructPath(globalBest, maze, paddedCols);
    }

    private void evaluateFast(Individual ind, boolean[] wallMap, int[] moveOffsets,
                              int startIdx, int goalIdx, int width) {
        int currIdx = startIdx;
        int steps = 0;
        byte[] genes = ind.genes;

        for (int i = 0; i < GENOME_LENGTH; i++) {
            int nextIdx = currIdx + moveOffsets[genes[i]];
            
            if (!wallMap[nextIdx]) {
                currIdx = nextIdx;
                steps++;
                if (currIdx == goalIdx) {
                    ind.reachedGoal = true;
                    ind.validStepsCount = steps;
                    ind.fitness = 1_000_000.0 - steps;
                    return;
                }
            }
        }

        ind.reachedGoal = false;
        ind.validStepsCount = steps;
        
        int currR = currIdx / width;
        int currC = currIdx % width;
        int goalR = goalIdx / width;
        int goalC = goalIdx % width;
        
        int dist = Math.abs(currR - goalR) + Math.abs(currC - goalC);
        ind.fitness = 10_000.0 - (dist * dist);
    }

    private void produceChild(Individual p1, Individual p2, Individual child, ThreadLocalRandom rand) {
        int mid = rand.nextInt(GENOME_LENGTH);
        System.arraycopy(p1.genes, 0, child.genes, 0, mid);
        System.arraycopy(p2.genes, mid, child.genes, mid, GENOME_LENGTH - mid);

        int index = 0;
        while (index < GENOME_LENGTH) {
            double r = rand.nextDouble();
            if (r == 0) r = 0.0000001;
            int jump = (int) (Math.log(r) / LOG_ONE_MINUS_RATE);
            index += jump;

            if (index < GENOME_LENGTH) {
                child.genes[index] = (byte) rand.nextInt(4);
                index++;
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

    private void initializePopulation(Individual[] pop, int startIdx, int goalIdx, int width) {
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

    private List<Cell> reconstructPath(Individual ind, Maze maze, int paddedWidth) {
        List<Cell> path = new ArrayList<>();
        int r = maze.start.row;
        int c = maze.start.col;
        path.add(maze.grid[r][c]);

        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = {0, 0, -1, 1};

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