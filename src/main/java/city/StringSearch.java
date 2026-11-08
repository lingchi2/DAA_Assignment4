package city;

import java.util.List;

public interface StringSearch {
    List<Integer> search(String text, String pattern);

    long comparisons();

    String name();
}
