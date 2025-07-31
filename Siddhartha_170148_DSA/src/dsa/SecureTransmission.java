package dsa;
import java.util.*;

public class SecureTransmission {
    // Graph represented as adjacency list:
    // Each node maps to a list of int arrays [neighborNode, signalStrength]
    private Map<Integer, List<int[]>> graph;

    /**
     * Constructor to initialize the network graph.
     * 
     * @param n     Number of offices (nodes) in the network.
     * @param links Array of communication links; each link represented as
     *              [officeA, officeB, signalStrength].
     */
    public SecureTransmission(int n, int[][] links) {
        graph = new HashMap<>();
        
        // Initialize the adjacency list for each office/node
        for (int i = 0; i < n; i++) {
            graph.put(i, new ArrayList<>());
        }

        // Populate the adjacency list with bidirectional edges (undirected graph)
        // Each edge stores neighbor and the strength of the communication link
        for (int[] link : links) {
            int u = link[0], v = link[1], strength = link[2];
            
            // Add edge u -> v
            graph.get(u).add(new int[] { v, strength });
            // Add edge v -> u (undirected)
            graph.get(v).add(new int[] { u, strength });
        }
    }

    /**
     * Checks if a message can be securely transmitted from sender to receiver.
     * The transmission must follow a path where every link's strength is strictly
     * less than maxStrength.
     * 
     * @param sender      The starting office/node.
     * @param receiver    The target office/node.
     * @param maxStrength Maximum allowed signal strength on any communication link
     *                    in the path.
     * @return True if such a path exists; false otherwise.
     */
    public boolean canTransmit(int sender, int receiver, int maxStrength) {
        // Keep track of visited offices to avoid infinite loops and redundant checks
        Set<Integer> visited = new HashSet<>();
        
        // Start Depth-First Search from sender
        return dfs(sender, receiver, maxStrength, visited);
    }

    /**
     * Depth-First Search helper method to explore possible paths under constraints.
     * 
     * @param current     Current office/node being explored.
     * @param target      Target office/node to reach.
     * @param maxStrength Maximum allowed signal strength.
     * @param visited     Set of offices already visited during this path exploration.
     * @return True if target is reachable via allowed edges; false otherwise.
     */
    private boolean dfs(int current, int target, int maxStrength, Set<Integer> visited) {
        // If current office is the target, path found
        if (current == target) return true;

        // Mark the current node as visited
        visited.add(current);

        // Explore neighbors connected by edges with strength less than maxStrength
        for (int[] neighbor : graph.get(current)) {
            int next = neighbor[0];       // Neighboring office
            int strength = neighbor[1];   // Signal strength of the link

            // Proceed only if link meets the strength condition and neighbor not visited yet
            if (strength < maxStrength && !visited.contains(next)) {
                // Recursive DFS call to explore from the neighbor
                if (dfs(next, target, maxStrength, visited)) {
                    // If a path to target is found, propagate true back up the recursion stack
                    return true;
                }
            }
        }

        // No valid path found from this branch
        return false;
    }

    /**
     * Main method for testing the SecureTransmission class functionality.
     */
    public static void main(String[] args) {
        // Define the communication links between offices with their strengths
        int[][] links = {
            {0, 2, 4},  // Link between office 0 and 2 with strength 4
            {2, 3, 1},  // Link between office 2 and 3 with strength 1
            {2, 1, 3},  // Link between office 2 and 1 with strength 3
            {4, 5, 5},  // Link between office 4 and 5 with strength 5
            {3, 0, 2}   // Link between office 3 and 0 with strength 2
        };

        // Initialize the SecureTransmission object with 6 offices and the above links
        SecureTransmission st = new SecureTransmission(6, links);

        // Test queries to check message transmission under various maxStrength constraints
        System.out.println(st.canTransmit(2, 3, 2)); // true
        // Explanation: Direct link 2 -> 3 has strength 1 (< 2), so transmission is possible

        System.out.println(st.canTransmit(1, 3, 3)); // false
        // Explanation: Link 1 -> 2 has strength 3 which is NOT less than 3, no valid path

        System.out.println(st.canTransmit(2, 0, 3)); // true
        // Explanation: Path 2 -> 3 -> 0 with strengths 1 and 2 (< 3) allows transmission

        System.out.println(st.canTransmit(0, 5, 6)); // false
        // Explanation: Offices 0 and 5 are in disconnected subgraphs; no path exists
    }
}
