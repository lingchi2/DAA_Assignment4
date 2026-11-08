package city;

import java.util.Random;

public final class Generator {
    public static final String DNA = "ACGT";

    private Generator() {
    }

    public static String random(Random rnd, int n, String alphabet) {
        char[] c = new char[n];
        for (int i = 0; i < n; i++) {
            c[i] = alphabet.charAt(rnd.nextInt(alphabet.length()));
        }
        return new String(c);
    }

    public static String repeat(char ch, int n) {
        char[] c = new char[n];
        java.util.Arrays.fill(c, ch);
        return new String(c);
    }

    public static String[] editPair(String kind, int n, long seed) {
        Random rnd = new Random(seed * 1000003L + n * 31L + kind.hashCode());
        switch (kind) {
            case "random":
                return new String[]{random(rnd, n, DNA), random(rnd, n, DNA)};
            case "identical": {
                String s = random(rnd, n, DNA);
                return new String[]{s, s};
            }
            case "disjoint":
                return new String[]{repeat('A', n), repeat('B', n)};
            default:
                throw new IllegalArgumentException(kind);
        }
    }

    public static String[] searchPair(String kind, int n, int m, long seed) {
        Random rnd = new Random(seed * 1000003L + n * 31L + kind.hashCode());
        switch (kind) {
            case "random":
                return new String[]{random(rnd, n, DNA), random(rnd, m, DNA)};
            case "worst_aaab":
                return new String[]{repeat('A', n), repeat('A', m - 1) + "B"};
            case "all_match":
                return new String[]{repeat('A', n), repeat('A', m)};
            default:
                throw new IllegalArgumentException(kind);
        }
    }
}
