package city;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class Experiment {
    public record Row(String algorithm, String family, String input, int n, int m,
                      double timeMs, long ops, long result) {
    }

    static final long SEED = 42L;
    static final int[] EDIT_SMALL = {3, 5, 7, 9, 11, 12, 13};
    static final int[] EDIT_LARGE = {250, 500, 1000, 2000, 4000};
    static final String[] EDIT_INPUTS = {"random", "identical", "disjoint"};
    static final int[] SEARCH_N = {100_000, 200_000, 400_000, 800_000, 1_600_000};
    static final int SEARCH_M = 200;
    static final String[] SEARCH_INPUTS = {"random", "worst_aaab", "all_match"};

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : "results");
        Files.createDirectories(out.resolve("plots"));
        List<Row> rows = new ArrayList<>();
        runEdit(rows);
        runSearch(rows);
        writeCsv(rows, out.resolve("results.csv"));
        makePlots(rows, out.resolve("plots").toString());
        System.out.println("rows: " + rows.size());
    }

    static void runEdit(List<Row> rows) {
        List<EditDistance> algs = List.of(new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance());
        for (String input : EDIT_INPUTS) {
            for (int n : concat(EDIT_SMALL, EDIT_LARGE)) {
                String[] p = Generator.editPair(input, n, SEED);
                int expected = -1;
                for (EditDistance alg : algs) {
                    boolean recursive = alg instanceof RecursiveEditDistance;
                    if (recursive && n > EDIT_SMALL[EDIT_SMALL.length - 1]) {
                        continue;
                    }
                    int runs = recursive ? 3 : 5;
                    int[] res = new int[1];
                    double t = medianMs(runs, () -> {
                        res[0] = alg.distance(p[0], p[1]);
                        return res[0];
                    });
                    if (expected >= 0 && expected != res[0]) {
                        throw new IllegalStateException("mismatch " + alg.name() + " " + input + " " + n);
                    }
                    expected = res[0];
                    rows.add(new Row(alg.name(), "edit", input, n, n, t, alg.operations(), res[0]));
                    System.out.printf("edit %-9s %-9s n=%-5d %10.3f ms ops=%d%n", alg.name(), input, n, t, alg.operations());
                }
            }
        }
    }

    static void runSearch(List<Row> rows) {
        List<StringSearch> algs = List.of(new NaiveSearch(), new KmpSearch());
        for (String input : SEARCH_INPUTS) {
            for (int n : SEARCH_N) {
                String[] p = Generator.searchPair(input, n, SEARCH_M, SEED);
                List<Integer> expected = null;
                for (StringSearch alg : algs) {
                    List<?>[] res = new List<?>[1];
                    double t = medianMs(5, () -> {
                        res[0] = alg.search(p[0], p[1]);
                        return res[0].size();
                    });
                    if (expected != null && !expected.equals(res[0])) {
                        throw new IllegalStateException("mismatch " + alg.name() + " " + input + " " + n);
                    }
                    @SuppressWarnings("unchecked")
                    List<Integer> found = (List<Integer>) res[0];
                    expected = found;
                    rows.add(new Row(alg.name(), "search", input, n, SEARCH_M, t, alg.comparisons(), found.size()));
                    System.out.printf("search %-6s %-10s n=%-8d %10.3f ms cmp=%d matches=%d%n",
                            alg.name(), input, n, t, alg.comparisons(), found.size());
                }
            }
        }
    }

    static double medianMs(int runs, Supplier<Object> task) {
        task.get();
        double[] t = new double[runs];
        for (int i = 0; i < runs; i++) {
            long s = System.nanoTime();
            task.get();
            t[i] = (System.nanoTime() - s) / 1e6;
        }
        Arrays.sort(t);
        return t[runs / 2];
    }

    static int[] concat(int[] a, int[] b) {
        int[] r = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    static void writeCsv(List<Row> rows, Path file) throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file))) {
            w.println("algorithm,family,input,n,m,time_ms,ops,result");
            for (Row r : rows) {
                w.printf(java.util.Locale.ROOT, "%s,%s,%s,%d,%d,%.4f,%d,%d%n",
                        r.algorithm, r.family, r.input, r.n, r.m, r.timeMs, r.ops, r.result);
            }
        }
    }

    static PlotWriter.Series series(List<Row> rows, String name, String alg, String family, String input,
                                    java.util.function.ToDoubleFunction<Row> y, int minN, int maxN) {
        List<Row> sel = rows.stream()
                .filter(r -> r.algorithm.equals(alg) && r.family.equals(family) && r.input.equals(input)
                        && r.n >= minN && r.n <= maxN)
                .sorted((a, b) -> Integer.compare(a.n, b.n)).toList();
        double[] xs = new double[sel.size()];
        double[] ys = new double[sel.size()];
        for (int i = 0; i < xs.length; i++) {
            xs[i] = sel.get(i).n;
            ys[i] = y.applyAsDouble(sel.get(i));
        }
        return new PlotWriter.Series(name, xs, ys);
    }

    static void makePlots(List<Row> rows, String dir) throws IOException {
        int smallMax = EDIT_SMALL[EDIT_SMALL.length - 1];
        int largeMin = EDIT_LARGE[0];
        int big = Integer.MAX_VALUE;

        List<PlotWriter.Series> s1 = new ArrayList<>();
        s1.add(series(rows, "recursive, random", "recursive", "edit", "random", r -> r.ops, 0, smallMax));
        s1.add(series(rows, "recursive, disjoint", "recursive", "edit", "disjoint", r -> r.ops, 0, smallMax));
        s1.add(series(rows, "recursive, identical", "recursive", "edit", "identical", r -> r.ops, 0, smallMax));
        s1.add(series(rows, "table cells, random", "table", "edit", "random", r -> r.ops, 0, smallMax));
        PlotWriter.draw(dir + "/01_recursive_calls.png",
                "Edit Distance: recursive calls vs string length",
                "string length n (characters, both strings)", "work counter (calls or cells, log scale)",
                s1, false, true, 0, Captions.P1);

        List<PlotWriter.Series> s2 = new ArrayList<>();
        s2.add(series(rows, "table, random", "table", "edit", "random", r -> r.timeMs, largeMin, big));
        s2.add(series(rows, "two rows, random", "two_rows", "edit", "random", r -> r.timeMs, largeMin, big));
        s2.add(series(rows, "table, disjoint", "table", "edit", "disjoint", r -> r.timeMs, largeMin, big));
        s2.add(series(rows, "two rows, disjoint", "two_rows", "edit", "disjoint", r -> r.timeMs, largeMin, big));
        PlotWriter.draw(dir + "/02_edit_time_loglog.png",
                "Edit Distance: median time of table and two-row DP",
                "string length n (characters, log scale)", "median time (ms, log scale)",
                s2, true, true, 0, Captions.P2);

        List<PlotWriter.Series> s3 = new ArrayList<>();
        for (String in : EDIT_INPUTS) {
            PlotWriter.Series t = series(rows, "", "table", "edit", in, r -> r.timeMs, largeMin, big);
            PlotWriter.Series u = series(rows, "", "two_rows", "edit", in, r -> r.timeMs, largeMin, big);
            double[] ratio = new double[t.ys().length];
            for (int i = 0; i < ratio.length; i++) {
                ratio[i] = t.ys()[i] / u.ys()[i];
            }
            s3.add(new PlotWriter.Series("table / two rows, " + in, t.xs(), ratio));
        }
        PlotWriter.draw(dir + "/03_table_vs_two_rows_ratio.png",
                "Edit Distance: time ratio table / two rows",
                "string length n (characters, log scale)", "time ratio (table time / two-row time)",
                s3, true, false, 0, Captions.P3);

        List<PlotWriter.Series> s4 = new ArrayList<>();
        for (String in : SEARCH_INPUTS) {
            for (String alg : new String[]{"naive", "kmp"}) {
                s4.add(series(rows, alg + ", " + in, alg, "search", in, r -> (double) r.ops / r.n, 0, big));
            }
        }
        PlotWriter.draw(dir + "/04_search_comparisons_per_char.png",
                "String search: character comparisons per text character",
                "text length n (characters, log scale)", "comparisons / n (ratio, log scale)",
                s4, true, true, 0, Captions.P4);

        List<PlotWriter.Series> s5 = new ArrayList<>();
        for (String in : SEARCH_INPUTS) {
            for (String alg : new String[]{"naive", "kmp"}) {
                s5.add(series(rows, alg + ", " + in, alg, "search", in, r -> r.timeMs, 0, big));
            }
        }
        PlotWriter.draw(dir + "/05_search_time_loglog.png",
                "String search: median time vs text length (m = 200)",
                "text length n (characters, log scale)", "median time (ms, log scale)",
                s5, true, true, 0, Captions.P5);
    }
}
