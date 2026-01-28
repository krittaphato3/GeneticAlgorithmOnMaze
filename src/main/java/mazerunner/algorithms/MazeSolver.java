package mazerunner.algorithms;

import mazerunner.models.Cell;
import java.util.List;

public interface MazeSolver {

    List<Cell> solve();

    String getName();
}
