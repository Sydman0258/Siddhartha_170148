package dsa;
public class MaximizeCapital {

    /**
     * Function to find the maximum capital after selecting at most k projects.
     * It uses a brute-force greedy approach without using heaps.
     */
    public static int findMaxCapital(int k, int c, int[] revenues, int[] investments) {
        int n = revenues.length; // Total number of projects

        // Array to keep track of which projects are already used (launched)
        boolean[] used = new boolean[n];

        // Repeat the selection process up to k times
        for (int i = 0; i < k; i++) {
            int bestIdx = -1; // Index of the best project we can afford right now

            // Step 1: Loop through all projects to find the most profitable one we can afford
            for (int j = 0; j < n; j++) {
                // Check if project is unused and affordable with current capital
                if (!used[j] && investments[j] <= c) {
                    // If it's the first affordable project or it has better revenue than the current best
                    if (bestIdx == -1 || revenues[j] > revenues[bestIdx]) {
                        bestIdx = j; // Update the best project index
                    }
                }
            }

            // Step 2: If no affordable project is found, stop early
            if (bestIdx == -1) {
                break;
            }

            // Step 3: Launch the selected project
            // - Add its revenue to current capital
            // - Mark it as used so it's not reused in the next iteration
            c += revenues[bestIdx];
            used[bestIdx] = true;
        }

        // After up to k selections, return the final capital
        return c;
    }

    // Test driver method
    public static void main(String[] args) {
        // Test Case 1
        int result1 = findMaxCapital(2, 0, new int[]{2, 5, 8}, new int[]{0, 2, 3});
        System.out.println("Max Capital (Example 1): " + result1); // Output: 7

        // Test Case 2
        int result2 = findMaxCapital(3, 1, new int[]{3, 6, 10}, new int[]{1, 3, 5});
        System.out.println("Max Capital (Example 2): " + result2); // Output: 20
    }
}
