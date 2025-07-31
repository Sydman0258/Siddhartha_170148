package dsa;
import java.util.*;

public class TreasureHuntGame {

    // Constants to represent outcomes
    public static final int DRAW = 0;
    public static final int MOUSE_WIN = 1;
    public static final int CAT_WIN = 2;

    // Turn identifiers
    public static final int MOUSE_TURN = 1;
    public static final int CAT_TURN = 2;

    /**
     * Solves the game using BFS + DP (bottom-up state classification).
     *
     * @param graph The undirected graph representing the game board.
     * @return Outcome of the game: 1 if mouse wins, 2 if cat wins, 0 if draw.
     */
    public int treasureGame(int[][] graph) {
        int n = graph.length;

        // dp[mouse][cat][turn] stores result of state:  
        // 0 = draw, 1 = mouse wins, 2 = cat wins, -1 = unknown (unvisited)
        int[][][] dp = new int[n][n][3];
        for (int[][] layer : dp)
            for (int[] row : layer)
                Arrays.fill(row, -1);

        // degree[mouse][cat][turn] = number of possible moves for current player from this state
        int[][][] degree = new int[n][n][3];

        // Precompute the degree (# of moves) for each state
        for (int mouse = 0; mouse < n; mouse++) {
            for (int cat = 0; cat < n; cat++) {
                degree[mouse][cat][MOUSE_TURN] = graph[mouse].length; // mouse can move to all neighbors
                // cat cannot move to node 0 (the treasure)
                int catMoves = 0;
                for (int nextCat : graph[cat]) {
                    if (nextCat != 0) catMoves++;
                }
                degree[mouse][cat][CAT_TURN] = catMoves;
            }
        }

        // Queue for BFS to process states from known terminal positions
        Queue<State> queue = new LinkedList<>();

        // Initialize terminal states (base cases):

        // 1) Mouse at 0 → Mouse wins
        for (int cat = 0; cat < n; cat++) {
            for (int turn = MOUSE_TURN; turn <= CAT_TURN; turn++) {
                dp[0][cat][turn] = MOUSE_WIN;
                queue.offer(new State(0, cat, turn, MOUSE_WIN));
            }
        }

        // 2) Cat catches mouse → Cat wins
        for (int i = 0; i < n; i++) {
            for (int turn = MOUSE_TURN; turn <= CAT_TURN; turn++) {
                if (i != 0) {
                    dp[i][i][turn] = CAT_WIN;
                    queue.offer(new State(i, i, turn, CAT_WIN));
                }
            }
        }

        // Process the queue: propagate known outcomes backward to predecessor states
        while (!queue.isEmpty()) {
            State cur = queue.poll();
            int mouse = cur.mouse;
            int cat = cur.cat;
            int turn = cur.turn;
            int result = cur.result;

            // Get all states that can lead to the current state (predecessors)
            for (State prev : getParents(graph, mouse, cat, turn)) {
                if (dp[prev.mouse][prev.cat][prev.turn] != -1) {
                    // Already known result for this predecessor
                    continue;
                }

                // If it's the current player's turn in the predecessor state:
                // - If current state is winning for the current player, then predecessor is losing for the other player.
                // - If current state is losing for the current player, predecessor might still be undecided.
                if (prev.turn == MOUSE_TURN) {
                    // Mouse turn in predecessor state
                    if (result == MOUSE_WIN) {
                        // If current state is mouse win, predecessor is mouse win because mouse can force a win
                        dp[prev.mouse][prev.cat][prev.turn] = MOUSE_WIN;
                        queue.offer(new State(prev.mouse, prev.cat, prev.turn, MOUSE_WIN));
                    } else {
                        // Decrease degree, if no more moves to avoid mouse loss, mark cat win
                        degree[prev.mouse][prev.cat][prev.turn]--;
                        if (degree[prev.mouse][prev.cat][prev.turn] == 0) {
                            dp[prev.mouse][prev.cat][prev.turn] = CAT_WIN;
                            queue.offer(new State(prev.mouse, prev.cat, prev.turn, CAT_WIN));
                        }
                    }
                } else {
                    // Cat turn in predecessor state
                    if (result == CAT_WIN) {
                        // If current state is cat win, predecessor is cat win because cat can force a win
                        dp[prev.mouse][prev.cat][prev.turn] = CAT_WIN;
                        queue.offer(new State(prev.mouse, prev.cat, prev.turn, CAT_WIN));
                    } else {
                        // Decrease degree, if no moves to avoid cat loss, mark mouse win
                        degree[prev.mouse][prev.cat][prev.turn]--;
                        if (degree[prev.mouse][prev.cat][prev.turn] == 0) {
                            dp[prev.mouse][prev.cat][prev.turn] = MOUSE_WIN;
                            queue.offer(new State(prev.mouse, prev.cat, prev.turn, MOUSE_WIN));
                        }
                    }
                }
            }
        }

        // The initial state: mouse at 1, cat at 2, mouse to move first
        return dp[1][2][MOUSE_TURN] == -1 ? DRAW : dp[1][2][MOUSE_TURN];
    }

    /**
     * Helper method to find all predecessor states that can lead to the current state.
     * Predecessor means the state from which current state can be reached in one move.
     *
     * @param graph The game graph.
     * @param mouse Current mouse position.
     * @param cat Current cat position.
     * @param turn Current turn.
     * @return List of predecessor states.
     */
    private List<State> getParents(int[][] graph, int mouse, int cat, int turn) {
        List<State> parents = new ArrayList<>();
        if (turn == MOUSE_TURN) {
            // Current turn is mouse's, so previous turn was cat's turn
            // Cat moved last, mouse position same, cat moved from a neighbor of current cat
            for (int prevCat : graph[cat]) {
                if (prevCat == 0) continue; // cat cannot move to 0
                parents.add(new State(mouse, prevCat, CAT_TURN));
            }
        } else {
            // Current turn is cat's, so previous turn was mouse's turn
            // Mouse moved last, cat position same, mouse moved from a neighbor of current mouse
            for (int prevMouse : graph[mouse]) {
                parents.add(new State(prevMouse, cat, MOUSE_TURN));
            }
        }
        return parents;
    }
    /**
     * State class to represent a game state for BFS processing.
     */
    private static class State {
        int mouse;
        int cat;
        int turn;
        int result;
        State(int mouse, int cat, int turn) {
            this.mouse = mouse;
            this.cat = cat;
            this.turn = turn;
        }
        State(int mouse, int cat, int turn, int result) {
            this.mouse = mouse;
            this.cat = cat;
            this.turn = turn;
            this.result = result;
        }  }
    // Test Example
    public static void main(String[] args) {
        TreasureHuntGame game = new TreasureHuntGame();
        int[][] graph = {
            {2, 5},      // 0
            {3},         // 1
            {0, 4, 5},   // 2
            {1, 4, 5},   // 3
            {2, 3},      // 4
            {0, 2, 3}    // 5
        };
        int result = game.treasureGame(graph);
        System.out.println("Game Result: " + result); // Expected output: 0 (Draw)
    }}
