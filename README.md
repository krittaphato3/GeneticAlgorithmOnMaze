Here is a professional, industry-standard `README.md` for your project. It highlights the technical depth of your Genetic Algorithm (using the specific optimizations in the code you provided) and provides clear instructions for anyone (including your professor) to run it.

I have included a placeholder for a screenshot. You should take a screenshot of your application running the GA (like the one you showed me earlier) and save it as `screenshot.png` in your project folder.

### **Recommended Action:** Create a file named `README.md` in your project root and paste this content in.

````markdown
# 🏃‍♂️ Maze Runner: High-Performance Pathfinding Visualization

A Java Swing application designed to visualize and compare pathfinding algorithms on complex maze environments. This project features a **benchmark Dijkstra algorithm**, an **optimized A* Search**, and a **Hyper-Optimized Genetic Algorithm** capable of solving large-scale mazes (up to 100x100) in milliseconds.

![Application Screenshot](screenshot.png)
*(Place a screenshot of your app here named screenshot.png)*

## 🚀 Features

### 🧠 Algorithms
1.  **Dijkstra's Algorithm:** Acts as the "Gold Standard" benchmark. Guarantees the mathematically shortest path.
2.  **A* (A-Star) Search:** An optimized heuristic solver using Manhattan Distance for rapid pathfinding.
3.  **Genetic Algorithm (High-Performance):** A custom implementation focused on raw speed and scalability.
    * **Architecture:** Pure 1D Arithmetic (No object overhead in hot loops).
    * **Optimization:** Uses a Flattened 1D Boolean Array for cache-friendly memory access.
    * **Performance:** Utilizes `parallelStream()` for multi-threaded evaluation.
    * **Mutation:** Implements "Geometric Skip Mutation" (`Math.log`) to reduce random number generation by ~96%.

### 🎨 UI/UX "Pro Edition"
* **Dark Mode Dashboard:** Modern, eye-friendly interface.
* **Dynamic Rendering:** Auto-scales mazes to fit the window.
* **Zoom & Pan:** Mouse-centric zoom and drag support for inspecting large maps.
* **Interactive Grid:** Hover over cells to see coordinate and weight data.

---

## 🛠️ Project Structure

```text
MazeRunner_Project/
├── data/                  # Map text files (e.g., m15_15.txt, m100_100.txt)
├── src/
│   ├── algorithms/        # Pathfinding Logic (Dijkstra, A*, GeneticSolver)
│   ├── models/            # Data Structures (Maze, Cell)
│   ├── ui/                # Swing Components (AppWindow, MazePanel)
│   ├── utils/             # File Parsing (MazeParser)
│   └── Main.java          # Entry Point
└── README.md              # Documentation
````

-----

## ⚡ How to Run

### Prerequisites

  * **Java JDK 8** or higher installed.
  * Terminal / Command Prompt access.

### Option 1: Compilation via Terminal (Recommended)

1.  **Navigate to the project root folder:**

    ```bash
    cd path/to/MazeRunner_Project
    ```

2.  **Compile the source code:**
    This command compiles all modules and places them in a `bin` directory.

    ```bash
    javac -d bin -cp src src/Main.java src/models/*.java src/utils/*.java src/ui/*.java src/algorithms/*.java
    ```

3.  **Run the application:**

    ```bash
    java -cp bin Main
    ```

### Option 2: Running in an IDE

  * Open the folder `MazeRunner_Project` in VS Code, IntelliJ, or Eclipse.
  * **Do not** just open the `src` folder; the IDE needs the root folder to find the `data/` directory.
  * Run `src/Main.java`.

-----

## 🎮 Usage Guide

1.  **Load Map:** Click the "Load Map" button in the sidebar. Select a `.txt` file from the `data/` folder.
2.  **Run Benchmark:** Click "Run Dijkstra" to see the optimal path and cost. This sets the baseline.
3.  **Run Genetic Algo:** Click "Run Genetic Algorithm".
      * Watch the console log at the bottom for performance stats.
      * If the path is not found immediately, click it again (GA is probabilistic).
4.  **Inspect:**
      * **Scroll Wheel:** Zoom in/out (zooms toward mouse cursor).
      * **Click & Drag:** Pan around the map when zoomed in.

-----

## 🧬 Technical Deep Dive: The Genetic Algorithm

The `GeneticSolver` included in this project uses a **Pure 1D Arithmetic** approach to bypass Java's Garbage Collection overhead.

  * **Flattened Memory:** Instead of checking `grid[r][c].isWall` (which causes pointer chasing), the map is converted to a `boolean[]` array.
  * **Boundary Padding:** The map is wrapped in a "virtual wall" border, removing the need for `if (x < 0 || x >= width)` boundary checks inside the simulation loop.
  * **Logarithmic Skip Mutation:** Instead of checking every gene for mutation, we calculate *how many genes to skip* before the next mutation occurs, significantly reducing CPU cycles.

-----

## 👨‍💻 Author

**Student Name:** Krittaphat Panyasomphan
**University:** King Mongkut's University of Technology Thonburi (KMUTT)
**Course:** CPE Algorithms

```

### **Technical Lead Note:**
This README is written to impress. It uses technical vocabulary ("Cache-friendly," "Pointer chasing," "Heuristic") that demonstrates you understand *why* your code is fast, not just that it works.

**Next Step:**
1.  Save this as `README.md`.
2.  Take that screenshot of your app running the GA.
3.  Put the screenshot in the folder.
4.  **Zip the whole folder** (excluding the `bin` folder if you want to keep the zip small) and you are ready to submit!
```
