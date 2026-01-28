package mazerunner.strategies.selection;

import mazerunner.models.Walker;
import java.util.List;

public interface SelectionStrategy {

    Walker select(List<Walker> population);
}
