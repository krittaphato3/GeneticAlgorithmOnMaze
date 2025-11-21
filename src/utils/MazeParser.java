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

    // Regex to find: "Number", S, G, or #
    // Group 1 will contain the digits if it's a number
    private static final Pattern CELL_PATTERN = Pattern.compile("\"(\\d+)\"|S|G|#");

    public static Maze parseMaze(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        
        // 1. Read all lines first to determine dimensions
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
        // We estimate cols from the first line, but we'll adjust dynamically
        int cols = 0; 

        // We need to parse the first line just to count columns accurately
        // (This is a quick pass to get dimensions)
        Matcher m = CELL_PATTERN.matcher(lines.get(0));
        while (m.find()) cols++;

        Maze maze = new Maze(rows, cols);

        // 2. Parse strictly
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
                    // It's a number like "10". Group 1 has the digits.
                    // If group(1) is null, it matched something weird, treat as wall or 0
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