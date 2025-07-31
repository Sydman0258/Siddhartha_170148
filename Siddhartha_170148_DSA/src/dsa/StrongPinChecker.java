package dsa;
public class StrongPinChecker {

    // Main method to compute the number of changes required to make the pin strong
    public static int strongPinChecker(String pin) {
        int n = pin.length();  // Current length of the PIN

        // Step 1: Flags to track presence of required character types
        boolean hasLower = false, hasUpper = false, hasDigit = false;

        // Step 2: Scan through each character to check which types are present
        for (char c : pin.toCharArray()) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }

        // Step 3: Count how many of the 3 required types are missing
        int missingTypes = 0;
        if (!hasLower) missingTypes++;
        if (!hasUpper) missingTypes++;
        if (!hasDigit) missingTypes++;

        // Step 4: Track lengths of repeating character sequences of length ≥ 3
        java.util.List<Integer> repeatLens = new java.util.ArrayList<>();
        int i = 2;

        while (i < n) {
            int len = 2;
            // Check if the current 3-character window is a repeat
            while (i < n && pin.charAt(i) == pin.charAt(i - 1) && pin.charAt(i - 1) == pin.charAt(i - 2)) {
                len++;
                i++;
            }
            if (len > 2) repeatLens.add(len); // Add repeat length
            else i++;
        }

        // Case 1: PIN is too short (< 6 characters)
        if (n < 6) {
            // Need to insert characters; insertions can fix both length and type issues
            return Math.max(6 - n, missingTypes);
        }

        // Case 2: PIN is within valid length (6 to 20)
        else if (n <= 20) {
            // Count how many replacements are needed for each repeat
            int replace = 0;
            for (int len : repeatLens) {
                replace += len / 3;  // Every 3 repeating chars require 1 replacement
            }
            // Return the maximum between missing types and needed replacements
            return Math.max(missingTypes, replace);
        }

        // Case 3: PIN is too long (> 20 characters)
        else {
            int delete = n - 20;      // Total deletions required to meet max length
            int replace = 0;          // Replacement counter for repeats
            int remainingDeletes = delete;

            // Create buckets for repeat patterns mod 3 (for greedy deletion optimization)
            int[] buckets = new int[3];
            for (int len : repeatLens) {
                buckets[len % 3]++;
            }

            // Prioritize deleting from sequences where it reduces replacements most:
            // 1. Delete 1 char from len%3==0 groups
            int del = Math.min(buckets[0], remainingDeletes);
            replace -= del;
            remainingDeletes -= del;

            // 2. Delete 2 chars from len%3==1 groups
            del = Math.min(buckets[1], remainingDeletes / 2);
            replace -= del;
            remainingDeletes -= del * 2;

            // 3. Delete 3+ chars from the rest
            del = remainingDeletes / 3;
            replace -= del;
            remainingDeletes -= del * 3;

            // Recalculate remaining replacements after deletions
            for (int len : repeatLens) {
                if (len >= 3) {
                    int reduced = len - Math.min(len - 2, delete); // Apply deletions
                    replace += reduced / 3;                        // Count replacements for remaining repeats
                }
            }

            // Final changes = deletions + max of (remaining replacements or missing character types)
            return delete + Math.max(missingTypes, replace);
        }
    }

    // Main method with test cases
    public static void main(String[] args) {
        System.out.println(strongPinChecker("X1!"));               // Output: 3
        System.out.println(strongPinChecker("123456"));            // Output: 2
        System.out.println(strongPinChecker("Aa1234!"));           // Output: 0 (already strong)
        System.out.println(strongPinChecker("AAAAAAAAAAAAAAAAAAAAAAA")); // Output: 11 (long & repetitive)
        System.out.println(strongPinChecker("aaabc")); // Output:2
    }
}
