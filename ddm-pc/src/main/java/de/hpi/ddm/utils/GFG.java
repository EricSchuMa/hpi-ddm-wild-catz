package de.hpi.ddm.utils;


// Java program to print all
// possible strings of length k

import java.util.ArrayList;
import java.util.List;

public class GFG {
    // The method that prints all
    // possible strings of length k.
    // It is mainly a wrapper over
    // recursive function printAllKLengthRec()
    public void printAllKLength(List<Character> set, int k, List<String> list)
    {
        int n = set.size();
        printAllKLengthRec(set, "", n, k, list);
    }

    // The main recursive method
    // to print all possible
    // strings of length k
    static void printAllKLengthRec(List<Character> set,
                                   String prefix,
                                   int n, int k, List<String> list)
    {

        // Base case: k is 0,
        // print prefix
        if (k == 0)
        {
            list.add(prefix);
            return;
        }

        // One by one add all characters
        // from set and recursively
        // call for k equals to k-1
        for (int i = 0; i < n; ++i)
        {

            // Next character of input added
            String newPrefix = prefix + set.get(i);

            // k is decreased, because
            // we have added a new character
            printAllKLengthRec(set, newPrefix,
                    n, k - 1, list);
        }
    }
}


