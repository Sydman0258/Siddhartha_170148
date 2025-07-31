package dsa;
import java.util.*;

public class CryptarithmSolver {

    // The three words involved in the cryptarithm equation: word1 + word2 = result
    static String word1, word2, result;

    // List to hold all unique letters that appear in the equation
    static List<Character> letters = new ArrayList<>();

    // Mapping from letters (A-Z) to assigned digits (0-9); -1 means unassigned
    static int[] charToDigit = new int[26];

    // Boolean array to mark which digits have already been assigned
    static boolean[] usedDigits = new boolean[10];

    public static void main(String[] args) {
        // Example 1: STAR + MOON = NIGHT
        word1 = "STAR";
        word2 = "MOON";
        result = "NIGHT";

        System.out.println("Trying to solve: " + word1 + " + " + word2 + " = " + result);
        if (!prepareAndSolve()) {
            System.out.println("No solution found.");
        }

        System.out.println("\n-------------------\n");

        // Example 2: CODE + BUG = DEBUG
        word1 = "CODE";
        word2 = "BUG";
        result = "DEBUG";

        System.out.println("Trying to solve: " + word1 + " + " + word2 + " = " + result);
        if (!prepareAndSolve()) {
            System.out.println("No solution found.");
        }
    }

    /**
     * Prepare the data structures and start backtracking to find a valid digit assignment.
     * @return true if a valid solution is found, false otherwise.
     */
    static boolean prepareAndSolve() {
        letters.clear();                  // Clear previous letters list
        Arrays.fill(charToDigit, -1);    // Reset all letter-to-digit assignments
        Arrays.fill(usedDigits, false);  // Reset digit usage tracker

        // Collect unique letters from all words while preserving order (LinkedHashSet)
        Set<Character> unique = new LinkedHashSet<>();
        for (char c : (word1 + word2 + result).toCharArray()) {
            unique.add(c);
        }
        letters.addAll(unique);

        // If more than 10 unique letters, impossible to assign distinct digits
        if (letters.size() > 10) return false;

        // Start backtracking from the first letter index (0)
        return backtrack(0);
    }

    /**
     * Recursive backtracking method to assign digits to letters.
     * @param idx current letter index to assign digit for
     * @return true if a solution is found downstream, false if none
     */
    static boolean backtrack(int idx) {
        // Base case: all letters assigned digits
        if (idx == letters.size()) {
            // Check if the current assignment satisfies the cryptarithm equation
            if (isValid()) {
                printSolution();  // Print the solution found
                return true;      // Signal success
            }
            return false;         // Assignment invalid, backtrack
        }

        // Try assigning digits 0 through 9 to the current letter
        for (int digit = 0; digit <= 9; digit++) {
            if (!usedDigits[digit]) {  // Skip if digit already used
                // Assign digit to letter at idx
                charToDigit[letters.get(idx) - 'A'] = digit;
                usedDigits[digit] = true;

                // Early pruning: check that no leading letter is assigned zero
                if (leadingZeroCheck(idx)) {
                    // Recurse to assign next letter
                    if (backtrack(idx + 1)) {
                        return true; // Stop as soon as one valid solution is found
                    }
                }

                // Backtrack: undo assignment
                usedDigits[digit] = false;
                charToDigit[letters.get(idx) - 'A'] = -1;
            }
        }

        return false;  // No valid digit assignment for this letter
    }

    /**
     * Ensure that the first letter of each word is not assigned zero (leading zero invalid).
     * @param currentIndex the index of the letter recently assigned
     * @return false if any leading letter has digit 0, true otherwise
     */
    static boolean leadingZeroCheck(int currentIndex) {
        // Check leading letters of word1, word2, and result
        if (charToDigit[word1.charAt(0) - 'A'] == 0) return false;
        if (charToDigit[word2.charAt(0) - 'A'] == 0) return false;
        if (charToDigit[result.charAt(0) - 'A'] == 0) return false;
        return true;
    }

    /**
     * Convert a word to its numerical value based on current digit assignments.
     * @param w word to convert
     * @return numeric value, or -1 if any letter unassigned
     */
    static long wordToNumber(String w) {
        long val = 0;
        for (char c : w.toCharArray()) {
            int d = charToDigit[c - 'A'];
            if (d == -1) return -1;  // incomplete assignment
            val = val * 10 + d;
        }
        return val;
    }

    /**
     * Check if the current digit assignment satisfies the equation word1 + word2 = result.
     * @return true if equation holds, false otherwise
     */
    static boolean isValid() {
        long num1 = wordToNumber(word1);
        long num2 = wordToNumber(word2);
        long res = wordToNumber(result);

        // If any incomplete assignment, invalid
        if (num1 == -1 || num2 == -1 || res == -1) return false;

        // Check sum equality
        return (num1 + num2) == res;
    }

    /**
     * Print the solution mapping letters to digits and the numeric values of words.
     */
    static void printSolution() {
        System.out.println("Solution found:");
        for (char c : letters) {
            System.out.printf("%c = %d\n", c, charToDigit[c - 'A']);
        }
        System.out.printf("%s = %d\n", word1, wordToNumber(word1));
        System.out.printf("%s = %d\n", word2, wordToNumber(word2));
        System.out.printf("%s = %d\n", result, wordToNumber(result));
        System.out.printf("Sum: %d + %d = %d\n", wordToNumber(word1), wordToNumber(word2), wordToNumber(result));
    }
}
