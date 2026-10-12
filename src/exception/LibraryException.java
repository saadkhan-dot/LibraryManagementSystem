package exception;

/** Checked exception for invalid library operations and user input. */
public class LibraryException extends Exception {
    public LibraryException(String message) { super(message); }
}
