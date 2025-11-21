package algorithms;

import models.Cell;
import models.Maze;

import java.util.*;

public class DijkstraSolver implements PathSolver {

    @Override
    public String getName() {
        return "Dijkstra's Algorithm";
    }

    @Override
    public List<Cell> solve(Maze maze) {
        // 1. Setup structures
        // Distance map: tracks cheapest cost found so far to reach a cell
        int[][] dist = new int[maze.rows][maze.cols];
        // Parent map: tracks "where did we come from" to reconstruct path
        Cell[][] parent = new Cell[maze.rows][maze.cols];
        
        for (int[] row : dist) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        // Priority Queue stores cells, ordered by current distance cost
        // We use a custom comparator to sort by the distance in our 'dist' array
        PriorityQueue<Cell> pq = new PriorityQueue<>(Comparator.comparingInt(c -> dist[c.row][c.col]));

        // 2. Initialize Start
        Cell start = maze.start;
        Cell goal = maze.goal;
        
        dist[start.row][start.col] = 0; // Cost to start is 0
        pq.add(start);

        // 3. Main Loop
        while (!pq.isEmpty()) {
            Cell current = pq.poll();

            // If we reached the goal, stop
            if (current == goal) {
                break;
            }

            // Explore Neighbors (Up, Down, Left, Right)
            int[] dRow = {-1, 1, 0, 0};
            int[] dCol = {0, 0, -1, 1};

            for (int i = 0; i < 4; i++) {
                int newR = current.row + dRow[i];
                int newC = current.col + dCol[i];

                if (maze.isValid(newR, newC)) {
                    Cell neighbor = maze.grid[newR][newC];
                    
                    // Calculate new cost: Current cost + Neighbor's weight
                    int newDist = dist[current.row][current.col] + neighbor.weight;

                    // Relaxation: If we found a cheaper way to get to neighbor, update it
                    if (newDist < dist[newR][newC]) {
                        dist[newR][newC] = newDist;
                        parent[newR][newC] = current; // Remember path
                        
                        // Remove and re-add to update priority (Java PQ doesn't support updateKey efficiently)
                        pq.remove(neighbor); 
                        pq.add(neighbor);
                    }
                }
            }
        }

        // 4. Reconstruct Path (Backtracking)
        List<Cell> path = new ArrayList<>();
        Cell crawl = goal;
        
        // If goal is unreachable (distance is still MAX), return empty
        if (dist[goal.row][goal.col] == Integer.MAX_VALUE) {
            return path; 
        }

        while (crawl != null) {
            path.add(crawl);
            crawl = parent[crawl.row][crawl.col];
        }
        Collections.reverse(path); // Reverse to get Start -> Goal
        return path;
    }
}