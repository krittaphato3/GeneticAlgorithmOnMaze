package utils;

import models.Cell;
import models.Maze;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MazeParser {

    private static final Pattern CELL_PATTERN = Pattern.compile("\"(\\d+)\"|S|G|#");

    public static Maze parseMaze(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        }

        if (lines.isEmpty()) throw new IOException("File is empty");

        int rows = lines.size();
        int cols = 0; 

        Matcher m = CELL_PATTERN.matcher(lines.get(0));
        while (m.find()) cols++;

        Maze maze = new Maze(rows, cols);

        for (int r = 0; r < rows; r++) {
            String line = lines.get(r);
            Matcher matcher = CELL_PATTERN.matcher(line);
            int c = 0;

            while (matcher.find() && c < cols) {
                Cell cell = new Cell(r, c);
                String token = matcher.group();

                if (token.equals("#")) {
                    cell.isWall = true;
                } else if (token.equals("S")) {
                    cell.isStart = true;
                    maze.start = cell;
                } else if (token.equals("G")) {
                    cell.isGoal = true;
                    maze.goal = cell;
                } else {
                    if (matcher.group(1) != null) {
                        cell.weight = Integer.parseInt(matcher.group(1));
                    }
                }
                
                maze.grid[r][c] = cell;
                c++;
            }
        }
        
        return maze;
    }
}