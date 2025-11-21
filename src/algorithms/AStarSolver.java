package algorithms;

import models.Cell;
import models.Maze;

import java.util.*;

public class AStarSolver implements PathSolver {

    @Override
    public String getName() {
        return "A* Search";
    }

    @Override
    public List<Cell> solve(Maze maze) {
        // 1. Setup Structures
        // gScore: Cost from Start to current node (Same as Dijkstra 'dist')
        int[][] gScore = new int[maze.rows][maze.cols];
        // fScore: gScore + Heuristic (Estimated total cost to Goal)
        int[][] fScore = new int[maze.rows][maze.cols];
        
        Cell[][] parent = new Cell[maze.rows][maze.cols];

        // Initialize scores to Infinity
        for (int r = 0; r < maze.rows; r++) {
            Arrays.fill(gScore[r], Integer.MAX_VALUE);
            Arrays.fill(fScore[r], Integer.MAX_VALUE);
        }

        // Priority Queue orders by fScore (Lowest estimated cost first)
        PriorityQueue<Cell> pq = new PriorityQueue<>(Comparator.comparingInt(c -> fScore[c.row][c.col]));

        // 2. Initialize Start
        Cell start = maze.start;
        Cell goal = maze.goal;

        gScore[start.row][start.col] = 0;
        fScore[start.row][start.col] = heuristic(start, goal);
        
        pq.add(start);

        // 3. Main Loop
        while (!pq.isEmpty()) {
            Cell current = pq.poll();

            if (current == goal) {
                return reconstructPath(parent, goal);
            }

            // Explore Neighbors
            int[] dRow = {-1, 1, 0, 0};
            int[] dCol = {0, 0, -1, 1};

            for (int i = 0; i < 4; i++) {
                int newR = current.row + dRow[i];
                int newC = current.col + dCol[i];

                if (maze.isValid(newR, newC)) {
                    Cell neighbor = maze.grid[newR][newC];

                    // Tentative gScore = current cost + neighbor weight
                    int tentativeG = gScore[current.row][current.col] + neighbor.weight;

                    // If we found a cheaper path to this neighbor
                    if (tentativeG < gScore[newR][newC]) {
                        parent[newR][newC] = current;
                        gScore[newR][newC] = tentativeG;
                        fScore[newR][newC] = tentativeG + heuristic(neighbor, goal);

                        // Update PQ
                        pq.remove(neighbor); // Remove old version if exists
                        pq.add(neighbor);    // Add with new priority
                    }
                }
            }
        }

        return new ArrayList<>(); // No path found
    }

    // The Heuristic: Manhattan Distance (Standard for grids)
    // Returns |x1 - x2| + |y1 - y2|
    private int heuristic(Cell a, Cell b) {
        return Math.abs(a.row - b.row) + Math.abs(a.col - b.col);
    }

    private List<Cell> reconstructPath(Cell[][] parent, Cell current) {
        List<Cell> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = parent[current.row][current.col];
        }
        Collections.reverse(path);
        return path;
    }
}