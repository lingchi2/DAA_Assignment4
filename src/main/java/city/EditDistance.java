package city;

public interface EditDistance {
    int distance(String a, String b);

    long operations();

    String name();
}
