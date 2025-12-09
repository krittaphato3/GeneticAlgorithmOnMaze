package algorithms;

import models.Cell;
import models.Maze;
import java.util.List;

public interface PathSolver {
    List<Cell> solve(Maze maze);
    String getName();
}