import exception.LibraryException;
import model.Book;
import model.Student;
import model.Transaction;
import service.LibraryService;

import java.time.LocalDate;

/** Small executable smoke check for the main service workflows (no test libraries required). */
public class LibraryServiceSmokeTest {
    public static void main(String[] args) throws Exception {
        LibraryService library = new LibraryService();
        library.addBook(new Book("B1", "111", "Zeta", "Author B", 20));
        library.addBook(new Book("B2", "222", "Alpha", "Author A", 10));
        library.registerStudent(new Student("S1", "Test Student", "student@example.edu"));
        check(library.getBooks().size() == 2, "book registrati on");
        check(library.getBooksSorted("title").get(0).getTitle().equals("Alpha"), "title sorting");
        check(library.getBooksSorted("price").get(0).getPrice() == 10, "price sorting");
        check(library.searchBooks("222").size() == 1, "ISBN search");
        check(library.searchBooks("author a").size() == 1, "author search");
        library.updateBook("B2", "222", "Alpha Updated", "Author A", 11);
        check(library.getBook("B2").getTitle().equals("Alpha Updated"), "book update");

        LocalDate issue = LocalDate.of(2026, 1, 1);
        Transaction issued = library.issueBook("B1", "S1", issue);
        check(issued.getDueDate().equals(issue.plusDays(14)), "due date");
        expectFailure(() -> library.issueBook("B1", "S1", issue), "duplicate issue");
        Transaction returned = library.returnBook("B1", "S1", issue.plusDays(17));
        check(returned.getFine() == 3, "late fine");
        check(library.getHistory().size() == 2, "transaction history");
        expectFailure(() -> library.returnBook("B1", "S1", issue.plusDays(18)), "invalid return");

        library.updateStudent("S1", "Updated Student", "updated@example.edu");
        check(library.getStudent("S1").getName().equals("Updated Student"), "member update");
        library.deleteStudent("S1");
        library.deleteBook("B1");
        check(library.getBooks().size() == 1 && library.getStudents().isEmpty(), "delete operations");
        expectFailure(() -> library.addBook(new Book("B2", "333", "Duplicate ID", "Someone", 1)), "duplicate ID");
        expectFailure(() -> library.registerStudent(new Student("S2", "Bad", "not-an-email")), "email validation");
        System.out.println("LibraryService smoke checks passed.");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError("Failed: " + label);
    }
    private static void expectFailure(CheckedAction action, String label) throws Exception {
        try { action.run(); } catch (LibraryException expected) { return; }
        throw new AssertionError("Expected LibraryException: " + label);
    }
    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
}
