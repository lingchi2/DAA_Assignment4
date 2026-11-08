package city;

final class Captions {
    static final String P1 = "Disjoint strings: recursive calls grow ~5.6x per added character; identical strings need n+1 calls; the table needs n² cells.";
    static final String P2 = "Both DP versions rise steeply with n (the counter is exactly n²) and stay within a factor of 2 of each other.";
    static final String P3 = "The ratio hovers around 1 with noise, so time alone cannot show a clear winner between table and two rows.";
    static final String P4 = "Naive costs ~200 comparisons per character on worst_aaab and all_match; KMP stays at 1-2; random input hides the gap.";
    static final String P5 = "On worst_aaab naive takes hundreds of ms and KMP a few ms at n = 1.6M; on random text both are equally fast.";

    private Captions() {
    }
}
