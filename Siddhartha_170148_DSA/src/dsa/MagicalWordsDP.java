package dsa;
public class MagicalWordsDP {

    // Main method to compute the maximum product of two non-overlapping palindromic substrings
    public static int maxMagicalPower(String s) {
        int n = s.length();

        // Step 1: Create a DP table where dp[i][j] = true if substring s[i..j] is a palindrome
        boolean[][] dp = new boolean[n][n];

        // Fill the DP table by checking every substring length from 1 to n
        for (int len = 1; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;  // End index of substring

                if (s.charAt(i) == s.charAt(j)) {
                    if (len <= 2) {
                        // Substrings of length 1 or 2 are palindromes if both ends match
                        dp[i][j] = true;
                    } else {
                        // For longer substrings, check if inner substring is also a palindrome
                        dp[i][j] = dp[i + 1][j - 1];
                    }
                }
            }
        }

        // Step 2: Compute leftMax[i] = length of the longest palindrome ending at or before index i
        int[] leftMax = new int[n];

        for (int end = 0; end < n; end++) {
            for (int start = 0; start <= end; start++) {
                // If substring s[start..end] is a palindrome
                if (dp[start][end]) {
                    int len = end - start + 1;
                    leftMax[end] = Math.max(leftMax[end], len);
                }
            }
        }

        // Convert leftMax to a prefix maximum array
        // Ensures that leftMax[i] holds the maximum palindrome length ending anywhere from 0 to i
        for (int i = 1; i < n; i++) {
            leftMax[i] = Math.max(leftMax[i], leftMax[i - 1]);
        }

        // Step 3: Compute rightMax[i] = length of the longest palindrome starting at or after index i
        int[] rightMax = new int[n];

        for (int start = n - 1; start >= 0; start--) {
            for (int end = start; end < n; end++) {
                // If substring s[start..end] is a palindrome
                if (dp[start][end]) {
                    int len = end - start + 1;
                    rightMax[start] = Math.max(rightMax[start], len);
                }
            }
        }

        // Convert rightMax to a suffix maximum array
        // Ensures that rightMax[i] holds the max palindrome starting anywhere from i to n-1
        for (int i = n - 2; i >= 0; i--) {
            rightMax[i] = Math.max(rightMax[i], rightMax[i + 1]);
        }

        // Step 4: Try all possible split points between index i and i+1
        // and calculate product of leftMax[i] * rightMax[i+1]
        int maxProduct = 0;

        for (int i = 0; i < n - 1; i++) {
            int product = leftMax[i] * rightMax[i + 1];
            maxProduct = Math.max(maxProduct, product); // Keep track of the max product
        }

        return maxProduct;
    }

    // Example test cases
    public static void main(String[] args) {
        // Case 1: "xyzyx" (5) and "a" or "c" (1) => 5 * 1 = 5
        System.out.println("Output 1: " + maxMagicalPower("xyzyxabc")); // 5

        // Case 2: "level" (5) and "racecar" (7) => 5 * 7 = 35
        System.out.println("Output 2: " + maxMagicalPower("levelwowracecar")); // 35

        // Mixed palindromes: Check longest combination of non-overlapping ones
        System.out.println("Output 3: " + maxMagicalPower("abacdcxyzyxcdd")); // Custom case

        // Test with even palindromes like "abba" and "cc"
        System.out.println("Output 4: " + maxMagicalPower("abbaaccddcc")); // Should catch "abba" and "cc"
    }
}
