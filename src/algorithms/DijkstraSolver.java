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
        int[][] dist = new int[maze.rows][maze.cols];
        Cell[][] parent = new Cell[maze.rows][maze.cols];
        
        for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE);

        PriorityQueue<Cell> pq =
                new PriorityQueue<>(Comparator.comparingInt(c -> dist[c.row][c.col]));

        Cell start = maze.start;
        Cell goal = maze.goal;

        dist[start.row][start.col] = 0;
        pq.add(start);

        while (!pq.isEmpty()) {
            Cell current = pq.poll();
            if (current == goal) break;

            int[] dRow = {-1, 1, 0, 0};
            int[] dCol = {0, 0, -1, 1};

            for (int i = 0; i < 4; i++) {
                int newR = current.row + dRow[i];
                int newC = current.col + dCol[i];

                if (maze.isValid(newR, newC)) {
                    Cell neighbor = maze.grid[newR][newC];
                    int newDist = dist[current.row][current.col] + neighbor.weight;

                    if (newDist < dist[newR][newC]) {
                        dist[newR][newC] = newDist;
                        parent[newR][newC] = current;

                        pq.remove(neighbor);
                        pq.add(neighbor);
                    }
                }
            }
        }

        List<Cell> path = new ArrayList<>();
        Cell crawl = goal;

        if (dist[goal.row][goal.col] == Integer.MAX_VALUE) return path;

        while (crawl != null) {
            path.add(crawl);
            crawl = parent[crawl.row][crawl.col];
        }

        Collections.reverse(path);
        return path;
    }
}
