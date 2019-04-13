package be.scc.common;

/**
 * RuntimeException does not need to be added to method signature
 */
public class SccException extends RuntimeException {
    public SccException(String message) {
        super(message);
    }
}
