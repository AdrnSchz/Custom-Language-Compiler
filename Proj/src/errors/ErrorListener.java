package errors;

public interface ErrorListener {
    void report(Error err);
    void abort();
}
