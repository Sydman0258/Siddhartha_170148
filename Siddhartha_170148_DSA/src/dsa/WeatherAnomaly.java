package dsa;
public class WeatherAnomaly {

    /**
     * Brute-force method to count the number of valid periods (subarrays)
     * where the sum of temperature changes is within the specified range [lowThreshold, highThreshold].
     * 
     * Time Complexity: O(n²)
     * Space Complexity: O(1)
     * 
     * @param temperature_changes Array of daily temperature changes.
     * @param lowThreshold Lower bound of the acceptable total temperature change.
     * @param highThreshold Upper bound of the acceptable total temperature change.
     * @return Number of valid periods that fall within the given threshold range.
     */
    public static int countValidPeriodsBruteForce(int[] temperature_changes, int lowThreshold, int highThreshold) {
        int count = 0;  // To keep track of the number of valid subarrays (periods)

        // Outer loop: iterate over every possible starting index of the subarray
        for (int start = 0; start < temperature_changes.length; start++) {
            int sum = 0;  // Initialize sum for the current subarray starting at index 'start'

            // Inner loop: iterate from 'start' to the end of the array to form subarrays
            for (int end = start; end < temperature_changes.length; end++) {
                sum += temperature_changes[end];  // Add current element to the running sum

                // Check if the current sum lies within the [lowThreshold, highThreshold] range
                if (sum >= lowThreshold && sum <= highThreshold) {
                    count++;  // Valid subarray found, increment count
                }
            }
        }

        return count;  // Return total number of valid periods
    }

    public static void main(String[] args) {
        // Test case 1
        int[] arr1 = {3, -1, -4, 6, 2};
        int low1 = 2, high1 = 5;
        System.out.println("Brute force result (Example 1): " + countValidPeriodsBruteForce(arr1, low1, high1));  // Output: 7

        // Test case 2
        int[] arr2 = {-2, 3, 8, -5, 7};
        int low2 = -1, high2 = 2;
        System.out.println("Brute force result (Example 2): " + countValidPeriodsBruteForce(arr2, low2, high2));  // Output: 2
    }
}
