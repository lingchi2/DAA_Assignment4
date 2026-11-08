package city;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SearchTest {
    private final List<StringSearch> all = List.of(new NaiveSearch(), new KmpSearch());

    @Test
    void lpsReferenceArrays() {
        assertArrayEquals(new int[]{0, 1, 2, 0, 1, 2, 3, 3}, KmpSearch.lps("AAACAAAA"));
        assertArrayEquals(new int[]{0, 0, 1, 2, 0, 1, 2, 3, 4}, KmpSearch.lps("ABABCABAB"));
    }

    @Test
    void referenceMatches() {
        for (StringSearch s : all) {
            assertEquals(List.of(10), s.search("ABABDABACDABABCABAB", "ABABCABAB"), s.name());
            assertEquals(List.of(0, 1, 2, 3), s.search("AAAAA", "AA"), s.name());
            assertEquals(List.of(0, 2, 4), s.search("ABABABA", "ABA"), s.name());
        }
    }

    @Test
    void noMatchReturnsEmptyList() {
        for (StringSearch s : all) {
            assertTrue(s.search("ABCDEF", "XYZ").isEmpty(), s.name());
        }
    }

    @Test
    void emptyPatternThrows() {
        for (StringSearch s : all) {
            assertThrows(IllegalArgumentException.class, () -> s.search("abc", ""), s.name());
            assertThrows(IllegalArgumentException.class, () -> s.search("", ""), s.name());
        }
    }

    @Test
    void patternLongerThanTextReturnsEmptyList() {
        for (StringSearch s : all) {
            assertTrue(s.search("ab", "abc").isEmpty(), s.name());
            assertTrue(s.search("", "a").isEmpty(), s.name());
        }
    }

    @Test
    void naiveAndKmpAgreeOnRandomTexts() {
        Random rnd = new Random(11);
        NaiveSearch naive = new NaiveSearch();
        KmpSearch kmp = new KmpSearch();
        for (int i = 0; i < 100; i++) {
            String text = Generator.random(rnd, rnd.nextInt(60), "AB");
            String pat = Generator.random(rnd, 1 + rnd.nextInt(5), "AB");
            assertEquals(naive.search(text, pat), kmp.search(text, pat), text + " " + pat);
        }
    }

    @Test
    void kmpNeverDoesMoreThanTwiceTextLengthComparisons() {
        KmpSearch kmp = new KmpSearch();
        String text = Generator.repeat('A', 5000);
        kmp.search(text, Generator.repeat('A', 99) + "B");
        assertTrue(kmp.comparisons() <= 2L * text.length() + 100);
    }

    @Test
    void naiveWorstCaseDoesAboutNTimesM() {
        NaiveSearch naive = new NaiveSearch();
        String text = Generator.repeat('A', 1000);
        naive.search(text, Generator.repeat('A', 9) + "B");
        assertEquals(991L * 10, naive.comparisons());
    }
}
