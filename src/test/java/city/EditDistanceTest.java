package city;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EditDistanceTest {
    private final List<EditDistance> all = List.of(
            new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance());

    @Test
    void referenceValues() {
        for (EditDistance e : all) {
            assertEquals(3, e.distance("kitten", "sitting"), e.name());
            assertEquals(2, e.distance("flaw", "lawn"), e.name());
            assertEquals(5, e.distance("intention", "execution"), e.name());
            assertEquals(3, e.distance("", "abc"), e.name());
            assertEquals(0, e.distance("abc", "abc"), e.name());
        }
    }

    @Test
    void emptyStrings() {
        for (EditDistance e : all) {
            assertEquals(0, e.distance("", ""), e.name());
            assertEquals(4, e.distance("abcd", ""), e.name());
            assertEquals(4, e.distance("", "abcd"), e.name());
        }
    }

    @Test
    void symmetric() {
        for (EditDistance e : all) {
            assertEquals(e.distance("sunday", "saturday"), e.distance("saturday", "sunday"), e.name());
        }
    }

    @Test
    void threeVersionsAgreeOnRandomStrings() {
        Random rnd = new Random(7);
        for (int i = 0; i < 100; i++) {
            String a = Generator.random(rnd, rnd.nextInt(9), "ABC");
            String b = Generator.random(rnd, rnd.nextInt(9), "ABC");
            int r = all.get(0).distance(a, b);
            assertEquals(r, all.get(1).distance(a, b), a + " " + b);
            assertEquals(r, all.get(2).distance(a, b), a + " " + b);
        }
    }

    @Test
    void countersAreMeasuredInsideAlgorithms() {
        TableEditDistance t = new TableEditDistance();
        TwoRowEditDistance u = new TwoRowEditDistance();
        t.distance("abcde", "xyz");
        u.distance("abcde", "xyz");
        assertEquals(15, t.operations());
        assertEquals(15, u.operations());
        RecursiveEditDistance r = new RecursiveEditDistance();
        r.distance("AAAA", "BBBB");
        assertEquals(true, r.operations() > 16);
    }
}
