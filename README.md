# MazeRunner

**MazeRunner** is a sophisticated Java-based pathfinding simulation and visualization tool. It serves as an interactive sandbox for exploring algorithmic efficiency, with a specialized focus on **Genetic Algorithms (GA)** alongside classic graph traversal methods like A* and Dijkstra.

## 🚀 Project Overview

MazeRunner allows users to visualize how different algorithms navigate complex grid-based environments. It provides real-time feedback on path costs, execution time, and algorithmic convergence. The project is designed for educational purposes, algorithm benchmarking, and experimentation with evolutionary computation parameters.

## 📦 Installation

### Prerequisites
*   **Java Development Kit (JDK) 8** or higher.
*   **Git** (optional, for cloning).

### Steps

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/MazeRunner.git
    cd MazeRunner
    ```

2.  **Compile the source code:**
    We use the standard Java compiler. Run the following command from the project root:
    ```bash
    # Create the binary directory
    mkdir bin
    
    # Compile
    javac -d bin -sourcepath src/main/java src/main/java/mazerunner/Main.java
    ```

## 🎮 Usage

### Running the Application
To start the MazeRunner GUI:

```bash
java -cp bin mazerunner.Main
```

### Workflow
1.  **Load Maze:** Use the sidebar to load a `.txt` maze file from `src/main/resources/mazes`.
2.  **Choose Algorithm:** Select *Genetic Algorithm*, *Dijkstra*, or *A**.
3.  **Visualize:** Click **Run Algorithm** to see the pathfinding in action. The grid will display:
    *   🟩 **Green:** Start Point
    *   🟥 **Red:** Goal Point
    *   🟨 **Yellow:** Computed Path
    *   ⬛ **Black:** Walls

## ⚙️ Configuration

MazeRunner provides extensive configuration options primarily through its Graphical User Interface (GUI). No external config files or environment variables are required.

### Genetic Algorithm Parameters
When the **Genetic Algorithm** is selected, a dedicated configuration panel appears:

| Parameter | Description | Default |
| :--- | :--- | :--- |
| **Population Size** | Number of individuals in each generation. | `1000` |
| **Max Generations** | Maximum number of evolutionary cycles. | `15000` |
| **Genome Length** | Max steps a walker can take (chromosome length). | `1000` |
| **Elitism Count** | Number of top performers preserved unchanged. | `5` |
| **Adaptive Rates** | Comma-separated mutation rates for adaptive logic. | `0.01, 0.1, 0.5, 1.0` |

### Strategy Selection
You can mix and match different evolutionary strategies:

*   **Selection:** *Tournament, Rank, Roulette Wheel*
*   **Crossover:** *Multi-Point, Uniform*
*   **Mutation:** *Inversion, Fixed Count, Random Reset, Swap*
*   **Fitness:** *Hybrid (Proximity + Cost), Proximity, Minimum Weight*
*   **Initialization:** *Heuristic, Hybrid, Random*

## 🧠 How It Works

The core of the project relies on a modular architecture:
1.  **Maze Parsing:** Text files are converted into a 2D grid of `Cell` objects.
2.  **Pathfinding Solvers:**
    *   **Deterministic (A*, Dijkstra):** Explore the grid using priority queues to find the mathematically optimal path.
    *   **Stochastic (Genetic):**
        *   **Population:** A collection of "Walkers" (paths) is created.
        *   **Selection:** The fittest walkers (closest to goal/lowest cost) are chosen.
        *   **Crossover:** Parents swap moves to create offspring.
        *   **Mutation:** Random changes are introduced to maintain diversity.
        *   **Loop:** This repeats until a solution is found or generations are exhausted.

## 🤝 Contribution Policy

We welcome contributions! Please refer to our [CONTRIBUTING.md](CONTRIBUTING.md) file for detailed guidelines on how to report bugs, suggest features, and submit pull requests.

## 📄 License Details

This project is licensed under the **MIT License**.

**You are free to:**
*   Use the code for commercial purposes.
*   Modify the code.
*   Distribute the code.
*   Sublicense the code.

**Under the following conditions:**
*   Include the original copyright notice and license in any copy of the software/source.

For the full license text, please see the [LICENSE.txt](LICENSE.txt) file.
