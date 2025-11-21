package algorithms;

import models.Cell;
import models.Maze;
import java.util.List;

public interface PathSolver {
    // Returns the ordered list of cells from Start to Goal
    List<Cell> solve(Maze maze);
    
    // Returns the name of the algorithm (for display)
    String getName();
}