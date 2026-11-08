package city;

import java.util.ArrayList;
import java.util.List;

public class KmpSearch implements StringSearch {
    private long comparisons;

    public static int[] lps(String p) {
        return build(p, null);
    }

    private static int[] build(String p, KmpSearch counter) {
        int m = p.length();
        int[] lps = new int[m];
        int len = 0;
        int i = 1;
        while (i < m) {
            if (counter != null) {
                counter.comparisons++;
            }
            if (p.charAt(i) == p.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else if (len > 0) {
                len = lps[len - 1];
            } else {
                lps[i] = 0;
                i++;
            }
        }
        return lps;
    }

    @Override
    public List<Integer> search(String text, String pattern) {
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("pattern must not be empty");
        }
        comparisons = 0;
        List<Integer> result = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();
        if (m > n) {
            return result;
        }
        int[] lps = build(pattern, this);
        int i = 0;
        int j = 0;
        while (i < n) {
            comparisons++;
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == m) {
                    result.add(i - m);
                    j = lps[j - 1];
                }
            } else if (j > 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
        return result;
    }

    @Override
    public long comparisons() {
        return comparisons;
    }

    @Override
    public String name() {
        return "kmp";
    }
}
