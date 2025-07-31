package dsa;
public class PatternSequenceExtraction {

    /**
     * Calculates how many times the string p2 (as a subsequence) can be extracted from p1 repeated t1 times,
     * without overlapping characters and not exceeding t2 extractions.
     *
     * @param p1  The pattern string to be repeated.
     * @param t1  The number of times p1 is repeated to create the full sequence.
     * @param p2  The target subsequence we want to extract.
     * @param t2  The maximum number of extractions allowed.
     * @return    The number of times p2 can be extracted from the repeated p1 sequence (up to t2 times).
     */
    public static int maxExtracted(String p1, int t1, String p2, int t2) {
        // Generate the full sequence by repeating p1 t1 times.
        String seqA = repeatString(p1, t1);

        int count = 0;     // To count how many times we've extracted p2
        int indexA = 0;    // Current index in seqA from where we try to match p2

        // Try to extract p2 as many times as possible, or up to t2 times
        while (true) {
            // Try to find p2 as a subsequence in seqA starting from indexA
            indexA = findSubsequence(seqA, p2, indexA);
            
            // If no more matches found, break out of loop
            if (indexA == -1) break;

            count++; // One successful extraction
            
            // If we’ve reached the requested max extractions, break
            if (count == t2) break;
        }

        // Return the number of times we were able to extract p2, not exceeding t2
        return Math.min(count, t2);
    }

    /**
     * Helper function to repeat a string 'times' number of times.
     *
     * @param s      The string to repeat.
     * @param times  Number of times to repeat the string.
     * @return       The repeated string.
     */
    private static String repeatString(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s);  // Append the pattern each time
        }
        return sb.toString();
    }

    /**
     * Tries to find string p2 as a subsequence within seqA, starting at a given index.
     * Returns the index in seqA just after the last character used in the match,
     * or -1 if no complete match was found.
     *
     * @param seqA        The sequence to search in.
     * @param p2          The target subsequence to find.
     * @param startIndex  The index in seqA to start searching from.
     * @return            Index after last matched char if found, otherwise -1.
     */
    private static int findSubsequence(String seqA, String p2, int startIndex) {
        int i = startIndex; // Pointer in seqA
        int j = 0;          // Pointer in p2

        // Traverse through seqA until end or until we match all characters in p2
        while (i < seqA.length() && j < p2.length()) {
            if (seqA.charAt(i) == p2.charAt(j)) {
                j++; // Move to next char in p2 if match found
            }
            i++; // Always move to next char in seqA
        }

        // If we've matched entire p2, return position after last matched char
        return j == p2.length() ? i : -1;
    }

    public static void main(String[] args) {
        // Example 1
        String p1 = "bca";
        int t1 = 6;
        String p2 = "ba";
        int t2 = 3;

        // Should output 3 as "ba" can be extracted 3 times
        int result = maxExtracted(p1, t1, p2, t2);
        System.out.println("Max extracted times: " + result);

        // Example 2 with higher t2 limit to test max possible extraction
        t2 = 5;
        result = maxExtracted(p1, t1, p2, t2);
        System.out.println("Max extracted times with t2=5: " + result); 
    }
}
