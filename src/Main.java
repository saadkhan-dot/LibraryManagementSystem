import gui.LibraryGUI;
import model.Book;
import model.Librarian;
import model.Student;
import service.LibraryService;

import java.awt.GraphicsEnvironment;

/** Application entry point with immediately usable sample records. */
public class Main {
    public static void main(String[] args) throws Exception {
        LibraryService library = new LibraryService();
        library.addBook(new Book("B101", "9780134685991", "Effective Java", "Joshua Bloch", 45.50));
        library.addBook(new Book("B102", "9780596009205", "Head First Java", "Kathy Sierra", 39.99));
        library.addBook(new Book("B103", "9780132350884", "Clean Code", "Robert C. Martin", 42.00));
        library.registerStudent(new Student("S1001", "Amina Patel", "amina@example.edu"));
        library.registerStudent(new Student("S1002", "Omar Khan", "omar@example.edu"));
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Sample library data loaded. This environment has no graphical desktop; run this command on a desktop to open the Swing GUI.");
            return;
        }
        LibraryGUI.launch(library, new Librarian("Library Desk"));
    }
}
