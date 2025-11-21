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
        int[][] gScore = new int[maze.rows][maze.cols];
        int[][] fScore = new int[maze.rows][maze.cols];
        Cell[][] parent = new Cell[maze.rows][maze.cols];

        for (int r = 0; r < maze.rows; r++) {
            Arrays.fill(gScore[r], Integer.MAX_VALUE);
            Arrays.fill(fScore[r], Integer.MAX_VALUE);
        }

        PriorityQueue<Cell> pq =
                new PriorityQueue<>(Comparator.comparingInt(c -> fScore[c.row][c.col]));

        Cell start = maze.start;
        Cell goal = maze.goal;

        gScore[start.row][start.col] = 0;
        fScore[start.row][start.col] = heuristic(start, goal);
        pq.add(start);

        while (!pq.isEmpty()) {
            Cell current = pq.poll();

            if (current == goal) {
                return reconstructPath(parent, goal);
            }

            int[] dRow = {-1, 1, 0, 0};
            int[] dCol = {0, 0, -1, 1};

            for (int i = 0; i < 4; i++) {
                int newR = current.row + dRow[i];
                int newC = current.col + dCol[i];

                if (maze.isValid(newR, newC)) {
                    Cell neighbor = maze.grid[newR][newC];
                    int tentativeG = gScore[current.row][current.col] + neighbor.weight;

                    if (tentativeG < gScore[newR][newC]) {
                        parent[newR][newC] = current;
                        gScore[newR][newC] = tentativeG;
                        fScore[newR][newC] = tentativeG + heuristic(neighbor, goal);

                        pq.remove(neighbor);
                        pq.add(neighbor);
                    }
                }
            }
        }

        return new ArrayList<>();
    }

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
