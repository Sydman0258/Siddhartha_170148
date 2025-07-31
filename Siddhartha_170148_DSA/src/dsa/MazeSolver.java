package dsa;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

@SuppressWarnings("serial")
public class MazeSolver extends JFrame {
    private static final int ROWS = 20;                      // Number of rows in maze grid
    private static final int COLS = 20;                      // Number of columns in maze grid
    private static final int CELL_SIZE = 30;                 // Size of each cell in pixels

    private Cell[][] grid = new Cell[ROWS][COLS];            // 2D array representing maze cells
    private Cell startCell = null;                            // Starting point selected by user
    private Cell endCell = null;                              // Ending point selected by user
    private javax.swing.Timer animationTimer;                // Timer to animate solution path
    private final int DELAY = 50;                             // Delay in ms between animation steps

    private int score = 0;                                    // Score based on solution efficiency
    private JLabel scoreLabel;                                // Label to display score

    public MazeSolver() {
        setTitle("Maze Solver");                              // Set window title
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);      // Exit app on window close
        setSize(COLS * CELL_SIZE + 200, ROWS * CELL_SIZE + 80); // Window size including margin for score
        setLocationRelativeTo(null);                          // Center window on screen

        generateMaze();                                       // Generate maze grid

        MazePanel mazePanel = new MazePanel();                // Panel to display maze
        JPanel buttonPanel = new JPanel();                     // Panel to hold buttons

        JButton dfsButton = new JButton("Solve with DFS");    // DFS solve button
        JButton bfsButton = new JButton("Solve with BFS");    // BFS solve button
        JButton newMazeButton = new JButton("Generate New Maze"); // New maze generation button

        dfsButton.addActionListener(e -> solveMaze(true));   // Run DFS when clicked
        bfsButton.addActionListener(e -> solveMaze(false));  // Run BFS when clicked
        newMazeButton.addActionListener(e -> {                // Generate new maze on click
            startCell = null;                                  // Reset start cell
            endCell = null;                                    // Reset end cell
            score = 0;                                         // Reset score
            updateScoreLabel();                                // Update score display
            generateMaze();                                    // Generate new maze
            repaint();                                         // Repaint GUI
        });

        buttonPanel.add(dfsButton);                            // Add DFS button to panel
        buttonPanel.add(bfsButton);                            // Add BFS button
        buttonPanel.add(newMazeButton);                        // Add new maze button

        scoreLabel = new JLabel("Score: 0");                   // Initialize score label
        buttonPanel.add(scoreLabel);                           // Add score label to button panel

        add(mazePanel, BorderLayout.CENTER);                   // Add maze panel to center
        add(buttonPanel, BorderLayout.SOUTH);                  // Add buttons panel to bottom

        setVisible(true);                                      // Show the window
    }

    // Generate maze using recursive backtracking and add loops
    private void generateMaze() {
        for (int row = 0; row < ROWS; row++) {                 // Loop over all rows
            for (int col = 0; col < COLS; col++) {             // Loop over all columns
                grid[row][col] = new Cell(row, col, true);     // Initialize all cells as walls
            }
        }
        carvePassagesFrom(1, 1);                               // Carve maze starting from (1,1)
        addLoops(30);                                          // Add 30 random loops to maze
    }

    // Recursive backtracking to carve paths in maze
    private void carvePassagesFrom(int row, int col) {
        grid[row][col].wall = false;                           // Mark current cell as passage

        int[] directions = {0, 1, 2, 3};                       // Directions: 0=up,1=right,2=down,3=left
        shuffleArray(directions);                              // Shuffle directions for randomness

        for (int direction : directions) {                     // Explore all directions
            int newRow = row, newCol = col;                    // Start from current cell

            switch (direction) {
                case 0: newRow = row - 2; break;               // Move up 2 cells
                case 1: newCol = col + 2; break;               // Move right 2 cells
                case 2: newRow = row + 2; break;               // Move down 2 cells
                case 3: newCol = col - 2; break;               // Move left 2 cells
            }

            // Check bounds and if new cell is still a wall (unvisited)
            if (newRow > 0 && newRow < ROWS && newCol > 0 && newCol < COLS && grid[newRow][newCol].wall) {
                grid[(row + newRow) / 2][(col + newCol) / 2].wall = false; // Knock down wall between cells
                carvePassagesFrom(newRow, newCol);                      // Recursively carve from new cell
            }
        }
    }

    // Fisher–Yates shuffle to randomize directions array
    private void shuffleArray(int[] array) {
        Random rand = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    // Add loops by removing random walls that connect two passages
    private void addLoops(int loopCount) {
        Random rand = new Random();
        int attempts = 0;
        int added = 0;

        while (added < loopCount && attempts < loopCount * 10) {
            attempts++;

            int row = rand.nextInt(ROWS - 2) + 1;             // Avoid edges
            int col = rand.nextInt(COLS - 2) + 1;

            if (!grid[row][col].wall) continue;               // Skip if not a wall

            int passagesAround = 0;
            if (row > 0 && !grid[row - 1][col].wall) passagesAround++;
            if (row < ROWS - 1 && !grid[row + 1][col].wall) passagesAround++;
            if (col > 0 && !grid[row][col - 1].wall) passagesAround++;
            if (col < COLS - 1 && !grid[row][col + 1].wall) passagesAround++;

            if (passagesAround >= 2) {                         // Only break wall if it creates a loop
                grid[row][col].wall = false;
                added++;
            }
        }
    }

    // Solve maze using DFS or BFS based on useDFS flag
    private void solveMaze(boolean useDFS) {
        if (startCell == null || endCell == null) {            // Check start/end points selected
            JOptionPane.showMessageDialog(this, "Please select a start (left click) and end (right click) point.");
            return;
        }

        resetVisited();                                        // Reset visited flags before solving
        score = 0;                                             // Reset score before solving

        java.util.List<Cell> path = useDFS ? solveDFS() : solveBFS(); // Solve using chosen algorithm

        if (path == null) {                                    // No solution found
            JOptionPane.showMessageDialog(this, "No path found.");
            scoreLabel.setText("Score: 0");                    // Reset score display
        } else {
            animatePath(path);                                 // Animate the solution path
            score = calculateScore(path.size());               // Calculate score based on path length
            updateScoreLabel();                                // Update score label text
        }
    }

    // Simple scoring function: higher score for fewer steps (max 1000)
    private int calculateScore(int steps) {
        int maxScore = 1000;                                   // Max possible score
        int penalty = steps * 10;                              // Penalty per step
        int calculatedScore = maxScore - penalty;              // Calculate score
        return Math.max(calculatedScore, 0);                   // Ensure score not negative
    }

    // Update the score label text in GUI
    private void updateScoreLabel() {
        scoreLabel.setText("Score: " + score);
    }

    // Solve maze using DFS
    private java.util.List<Cell> solveDFS() {
        Stack<Cell> stack = new Stack<>();                     // Stack for DFS
        Map<Cell, Cell> cameFrom = new HashMap<>();            // Map to reconstruct path
        stack.push(startCell);

        while (!stack.isEmpty()) {
            Cell current = stack.pop();                         // Pop last cell added
            current.visited = true;                             // Mark visited

            if (current == endCell) break;                      // Stop when reached end

            for (Cell neighbor : getNeighbors(current)) {      // Visit all neighbors
                if (!neighbor.visited && !neighbor.wall) {
                    stack.push(neighbor);                       // Add to stack
                    cameFrom.put(neighbor, current);            // Track path
                    neighbor.visited = true;                     // Mark visited early
                }
            }
        }

        if (!cameFrom.containsKey(endCell)) return null;       // No path found

        return reconstructPath(cameFrom, endCell);             // Reconstruct path from map
    }

    // Solve maze using BFS
    private java.util.List<Cell> solveBFS() {
        Queue<Cell> queue = new LinkedList<>();                 // Queue for BFS
        Map<Cell, Cell> cameFrom = new HashMap<>();
        queue.add(startCell);
        startCell.visited = true;

        while (!queue.isEmpty()) {
            Cell current = queue.poll();                         // Dequeue next cell

            if (current == endCell) break;                       // Stop when reached end

            for (Cell neighbor : getNeighbors(current)) {       // Visit neighbors
                if (!neighbor.visited && !neighbor.wall) {
                    queue.add(neighbor);                          // Enqueue neighbor
                    cameFrom.put(neighbor, current);             // Track path
                    neighbor.visited = true;                      // Mark visited
                }
            }
        }

        if (!cameFrom.containsKey(endCell)) return null;        // No path found

        return reconstructPath(cameFrom, endCell);              // Reconstruct path
    }

    // Reconstruct path from cameFrom map
    private java.util.List<Cell> reconstructPath(Map<Cell, Cell> cameFrom, Cell end) {
        java.util.List<Cell> path = new ArrayList<>();
        Cell current = end;

        while (current != startCell) {                           // Backtrack from end to start
            path.add(current);
            current = cameFrom.get(current);
        }

        Collections.reverse(path);                               // Reverse to start->end order
        return path;
    }

    // Animate the solution path cell by cell
    private void animatePath(java.util.List<Cell> path) {
        if (animationTimer != null) animationTimer.stop();       // Stop existing animation

        Iterator<Cell> iterator = path.iterator();                // Iterator over path cells
        animationTimer = new javax.swing.Timer(DELAY, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (iterator.hasNext()) {
                    Cell cell = iterator.next();
                    cell.inPath = true;                           // Mark cell as part of path
                    repaint();                                    // Repaint GUI
                } else {
                    animationTimer.stop();                         // Stop timer when done
                    JOptionPane.showMessageDialog(null, "Maze solved! Steps: " + path.size());
                }
            }
        });

        animationTimer.start();                                   // Start animation
    }

    // Get neighbors (up, down, left, right) of a cell
    private java.util.List<Cell> getNeighbors(Cell cell) {
        java.util.List<Cell> neighbors = new ArrayList<>();
        int row = cell.row;
        int col = cell.col;

        if (row > 0) neighbors.add(grid[row - 1][col]);
        if (row < ROWS - 1) neighbors.add(grid[row + 1][col]);
        if (col > 0) neighbors.add(grid[row][col - 1]);
        if (col < COLS - 1) neighbors.add(grid[row][col + 1]);

        return neighbors;
    }

    // Reset visited and inPath flags in all cells
    private void resetVisited() {
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++) {
                grid[row][col].visited = false;
                grid[row][col].inPath = false;
            }
    }

    // Custom panel for drawing maze grid and handling mouse clicks
    private class MazePanel extends JPanel {
        public MazePanel() {
            setPreferredSize(new Dimension(COLS * CELL_SIZE, ROWS * CELL_SIZE));

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    int col = e.getX() / CELL_SIZE;             // Get clicked column
                    int row = e.getY() / CELL_SIZE;             // Get clicked row

                    if (row >= ROWS || col >= COLS) return;     // Ignore clicks outside grid

                    if (SwingUtilities.isLeftMouseButton(e)) { // Left click = set start cell
                        startCell = grid[row][col];
                        startCell.wall = false;                  // Ensure start is not a wall
                    } else if (SwingUtilities.isRightMouseButton(e)) { // Right click = set end cell
                        endCell = grid[row][col];
                        endCell.wall = false;                    // Ensure end is not a wall
                    }

                    repaint();                                   // Refresh GUI
                }
            });
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    Cell cell = grid[row][col];
                    int x = col * CELL_SIZE;
                    int y = row * CELL_SIZE;

                    if (cell.wall) g.setColor(Color.BLACK);           // Wall cells: black
                    else if (cell == startCell) g.setColor(Color.GREEN); // Start cell: green
                    else if (cell == endCell) g.setColor(Color.RED);      // End cell: red
                    else if (cell.inPath) g.setColor(Color.BLUE);         // Solution path: blue
                    else g.setColor(Color.WHITE);                          // Open path: white

                    g.fillRect(x, y, CELL_SIZE, CELL_SIZE);            // Fill cell rectangle
                    g.setColor(Color.GRAY);                             // Draw cell border in gray
                    g.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }

    // Cell class representing each maze cell
    private class Cell {
        int row, col;                                              // Position of cell
        boolean wall;                                              // True if cell is wall
        boolean visited = false;                                   // Visited flag for searches
        boolean inPath = false;                                    // True if part of solution path

        Cell(int row, int col, boolean wall) {
            this.row = row;
            this.col = col;
            this.wall = wall;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cell)) return false;
            Cell other = (Cell) o;
            return row == other.row && col == other.col;
        }

        public int hashCode() {
            return Objects.hash(row, col);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MazeSolver());        // Start GUI on event thread
    }
}
