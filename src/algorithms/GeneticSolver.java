package algorithms;

import models.Cell;
import models.Maze;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GeneticSolver implements PathSolver {

    // --- TUNED PARAMETERS FOR PERFORMANCE ---
    private static final int POPULATION_SIZE = 300; // More diversity
    private static final int MAX_GENERATIONS = 200; // Give it time
    private static final int VICTORY_LAP = 30;      // optimizing gens after finding goal
    private static final double MUTATION_RATE = 0.05;
    private static final int TOURNAMENT_SIZE = 10;
    private static final double ELITISM_RATE = 0.02; // Top 2% survive

    // Directions: 0=Up, 1=Down, 2=Left, 3=Right
    private static final int[] dRow = {-1, 1, 0, 0};
    private static final int[] dCol = {0, 0, -1, 1};

    @Override
    public String getName() {
        return "Genetic Algorithm (Optimized)";
    }

    @Override
    public List<Cell> solve(Maze maze) {
        int genomeLength = Math.max(maze.rows * maze.cols, 200); // Cap minimum length
        List<Individual> population = initializeSmartPopulation(maze, genomeLength);
        Individual bestSolution = null;
        int gensSinceSolution = 0;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            
            // 1. Parallel Evaluation (Uses all CPU cores)
            // We map the fitness calc to threads
            final Individual currentBest = bestSolution;
            population.parallelStream().forEach(ind -> evaluate(ind, maze));

            // 2. Find Best in this Generation
            Individual genBest = Collections.min(population); // Min because we implemented Comparable backwards? No, check compareTo

            // Update Global Best
            if (bestSolution == null || genBest.fitness > bestSolution.fitness) {
                bestSolution = new Individual(genBest);
            }

            // 3. Early Exit Logic
            if (bestSolution.reachedGoal) {
                gensSinceSolution++;
                // If we found the goal and optimized for a bit, STOP.
                if (gensSinceSolution > VICTORY_LAP) {
                    break;
                }
            }

            // 4. Evolution (Selection & Crossover)
            List<Individual> nextGen = new ArrayList<>();
            
            // Elitism: Keep top X% directly
            Collections.sort(population); // Sorts Descending (Best first)
            int eliteCount = (int) (POPULATION_SIZE * ELITISM_RATE);
            for (int i = 0; i < eliteCount; i++) {
                nextGen.add(new Individual(population.get(i)));
            }

            // Breed the rest
            while (nextGen.size() < POPULATION_SIZE) {
                Individual p1 = tournamentSelection(population);
                Individual p2 = tournamentSelection(population);
                Individual child = crossover(p1, p2, genomeLength);
                mutate(child, maze); // Pass maze for smart mutation
                nextGen.add(child);
            }

            population = nextGen;
        }

        return bestSolution != null ? bestSolution.path : new ArrayList<>();
    }

    // --- Helper Classes & Methods ---

    private class Individual implements Comparable<Individual> {
        int[] genes;
        double fitness;
        int cost;
        boolean reachedGoal;
        List<Cell> path;
        int distanceToGoal;

        public Individual(int length) {
            genes = new int[length];
            path = new ArrayList<>();
        }
        
        public Individual(Individual other) {
            this.genes = other.genes.clone();
            this.fitness = other.fitness;
            this.cost = other.cost;
            this.reachedGoal = other.reachedGoal;
            this.path = new ArrayList<>(other.path);
            this.distanceToGoal = other.distanceToGoal;
        }

        @Override
        public int compareTo(Individual other) {
            // Descending order: Higher fitness comes first
            return Double.compare(other.fitness, this.fitness);
        }
    }

    // OPTIMIZATION 1: Smart Initialization
    // Biases the random path to generally move towards the goal
    private List<Individual> initializeSmartPopulation(Maze maze, int length) {
        List<Individual> pop = new ArrayList<>();
        Random rand = new Random();
        
        int startR = maze.start.row;
        int startC = maze.start.col;
        int goalR = maze.goal.row;
        int goalC = maze.goal.col;

        for (int i = 0; i < POPULATION_SIZE; i++) {
            Individual ind = new Individual(length);
            
            // Simulate walker state to give smart directions
            int currR = startR;
            int currC = startC;

            for (int j = 0; j < length; j++) {
                // 40% chance to pick a "Smart" direction, 60% random
                if (rand.nextDouble() < 0.4) {
                    int preferredMove = -1;
                    if (currR < goalR) preferredMove = 1; // Down
                    else if (currR > goalR) preferredMove = 0; // Up
                    else if (currC < goalC) preferredMove = 3; // Right
                    else if (currC > goalC) preferredMove = 2; // Left
                    
                    if (preferredMove != -1) {
                        ind.genes[j] = preferredMove;
                    } else {
                        ind.genes[j] = rand.nextInt(4);
                    }
                } else {
                    ind.genes[j] = rand.nextInt(4);
                }
                
                // Roughly update position for next gene (ignoring walls for initialization speed)
                currR += dRow[ind.genes[j]];
                currC += dCol[ind.genes[j]];
            }
            pop.add(ind);
        }
        return pop;
    }

    private void evaluate(Individual ind, Maze maze) {
        // Reset
        ind.path.clear();
        ind.cost = 0;
        ind.reachedGoal = false;
        
        int currR = maze.start.row;
        int currC = maze.start.col;
        ind.path.add(maze.grid[currR][currC]);
        
        // Keep track of visited to punish loops
        Set<String> visited = new HashSet<>();
        visited.add(currR + "," + currC);

        for (int move : ind.genes) {
            int nextR = currR + dRow[move];
            int nextC = currC + dCol[move];

            if (maze.isValid(nextR, nextC)) {
                currR = nextR;
                currC = nextC;
                Cell cell = maze.grid[currR][currC];
                ind.path.add(cell);
                ind.cost += cell.weight;

                // Small penalty for revisiting cells (encourage exploration)
                if (visited.contains(currR + "," + currC)) {
                    ind.cost += 50; 
                }
                visited.add(currR + "," + currC);

                if (cell.isGoal) {
                    ind.reachedGoal = true;
                    break;
                }
            }
        }

        ind.distanceToGoal = Math.abs(currR - maze.goal.row) + Math.abs(currC - maze.goal.col);

        // Fitness Formula
        if (ind.reachedGoal) {
            // Base Reward (10,000) - Cost (Lower cost is better) + Bonus for short path
            ind.fitness = 10000.0 - ind.cost + (1000.0 / ind.path.size());
        } else {
            // If failed, fitness is purely based on how close we got
            // Squared distance to punish being far away heavily
            ind.fitness = 1000.0 / (ind.distanceToGoal * ind.distanceToGoal + 1);
        }
    }

    private Individual tournamentSelection(List<Individual> pop) {
        Random rand = new Random();
        Individual best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Individual ind = pop.get(rand.nextInt(pop.size()));
            if (best == null || ind.fitness > best.fitness) {
                best = ind;
            }
        }
        return best;
    }

    private Individual crossover(Individual p1, Individual p2, int length) {
        Individual child = new Individual(length);
        Random rand = new Random();
        int crossoverPoint = rand.nextInt(length);

        for (int i = 0; i < length; i++) {
            if (i < crossoverPoint) child.genes[i] = p1.genes[i];
            else child.genes[i] = p2.genes[i];
        }
        return child;
    }

    private void mutate(Individual ind, Maze maze) {
        Random rand = new Random();
        for (int i = 0; i < ind.genes.length; i++) {
            if (rand.nextDouble() < MUTATION_RATE) {
                // Pure random mutation
                ind.genes[i] = rand.nextInt(4);
            }
        }
    }
}