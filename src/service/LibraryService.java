package service;

import exception.LibraryException;
import model.Book;
import model.Student;
import model.Transaction;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Holds application rules and in-memory collections; the GUI delegates work here. */
public class LibraryService {
    public static final int LOAN_DAYS = 14;
    public static final double FINE_PER_DAY = 1.0;

    private final ArrayList<Book> books = new ArrayList<>();
    private final ArrayList<Student> students = new ArrayList<>();
    private final HashMap<String, Book> booksById = new HashMap<>();
    private final TreeMap<String, Book> booksByTitle = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final TreeMap<String, Book> booksBySortedId = new TreeMap<>();
    private final LinkedList<Transaction> history = new LinkedList<>();
    private final Map<String, LocalDate> activeLoans = new HashMap<>();


    // Give me a Book object, and I'll try to add it to the library. If something is wrong, I'll throw a LibraryException.

    public void addBook(Book book) throws LibraryException {
        validateBook(book);
        if (booksById.containsKey(book.getId())) throw new LibraryException("Book ID already exists.");
        if (booksByTitle.containsKey(titleKey(book))) throw new LibraryException("A book with this title already exists.");
        books.add(book);
        indexBook(book);
    }


//Find the existing book using its ID. Validate the new price and make sure the new title doesn't belong to another book. "
// Remove the old title index, update the book's information using setters, and finally re-index the updated book in the maps.

    public void updateBook(String id, String isbn, String title, String author, double price) throws LibraryException {
        Book book = getBook(id);
        if (price < 0 || !Double.isFinite(price)) throw new LibraryException("Price must be a valid non-negative number.");
        String newKey = title.trim() + " [" + id + "]";
        Book sameTitle = booksByTitle.get(newKey);
        if (sameTitle != null && sameTitle != book) throw new LibraryException("A book with this title already exists.");
        booksByTitle.remove(titleKey(book));
        book.setIsbn(required(isbn, "ISBN"));
        book.setTitle(required(title, "Title"));
        book.setAuthor(required(author, "Author"));
        book.setPrice(price);
        indexBook(book);
    }


    public void deleteBook(String id) throws LibraryException {
        Book book = getBook(id);
        if (!book.isAvailable()) throw new LibraryException("Return the book before deleting it.");
        books.remove(book);
        booksById.remove(book.getId());
        booksBySortedId.remove(book.getId());
        booksByTitle.remove(titleKey(book));
    }

    public void registerStudent(Student student) throws LibraryException {
        validateStudent(student);
        if (findStudent(student.getId()) != null) throw new LibraryException("Student ID already exists.");
        students.add(student);
    }

    public void updateStudent(String id, String name, String email) throws LibraryException {
        Student student = requireStudent(id);
        student.setName(required(name, "Student name"));
        student.setEmail(validateEmail(email));
    }
// It checks whether the student has any active book loan before allowing the student to be deleted.

    public void deleteStudent(String id) throws LibraryException {
        Student student = requireStudent(id);
        for (String loanKey : activeLoans.keySet()) if (loanKey.endsWith("|" + id)) throw new LibraryException("Student has an active loan.");
        students.remove(student);
    }
//The issueBook() method handles issuing a book to a student. It checks the date, verifies the book and student exist, checks whether the book is available, calculates the due date
// marks the book as unavailable, records the active loan, and creates a transaction that is stored in the history.”
    public Transaction issueBook(String bookId, String studentId, LocalDate issueDate) throws LibraryException {
        validateDate(issueDate);
        Book book = getBook(bookId);
        requireStudent(studentId);
        if (!book.isAvailable()) throw new LibraryException("Book is already issued.");
        LocalDate due = issueDate.plusDays(LOAN_DAYS);
        book.setAvailable(false);
        activeLoans.put(loanKey(bookId, studentId), due);
        Transaction transaction = new Transaction("ISSUE", bookId, studentId, issueDate, due, 0);
        history.add(transaction);
        return transaction;
    }


//The returnBook() method handles returning a book. It validates the return date, checks the book and student, finds the active loan, verifies that the return date is valid
//calculates any late fine, removes the active loan, marks the book as available again, creates a RETURN transaction, and stores it in the transaction history.”

    public Transaction returnBook(String bookId, String studentId, LocalDate returnDate) throws LibraryException {
        validateDate(returnDate);
        Book book = getBook(bookId);
        requireStudent(studentId);
        String key = loanKey(bookId, studentId);
        LocalDate due = activeLoans.get(key);
        if (due == null) throw new LibraryException("No active issue record exists for this book and student.");
        if (returnDate.isBefore(due.minusDays(LOAN_DAYS))) throw new LibraryException("Return date cannot be before the issue date.");
        double fine = Math.max(0, ChronoUnit.DAYS.between(due, returnDate)) * FINE_PER_DAY;
        activeLoans.remove(key);
        book.setAvailable(true);
        Transaction transaction = new Transaction("RETURN", bookId, studentId, returnDate, due, fine);
        history.add(transaction);
        return transaction;
    }

//The searchBooks() method searches for books based on the user's query. It converts the query to lowercase and removes extra spaces,
//then checks every book in the list. If the ISBN, title, or author contains the search text, that book is added to the result list and returned.”

    public List<Book> searchBooks(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        ArrayList<Book> result = new ArrayList<>();
        for (Book b : books) if (b.getIsbn().toLowerCase(Locale.ROOT).contains(q)
                || b.getTitle().toLowerCase(Locale.ROOT).contains(q)
                || b.getAuthor().toLowerCase(Locale.ROOT).contains(q)) result.add(b);
        return result;
    }

// The getBooksSorted() method creates a copy of the books list and sorts it based on the field selected by the user.It uses a Comparator to decide how the books should be compared.
//  The user can sort by title, author, or price. If an invalid field is given, it throws a LibraryException.”

    public List<Book> getBooksSorted(String field) throws LibraryException {
        ArrayList<Book> result = new ArrayList<>(books);
        Comparator<Book> comparator;
        switch (field.toLowerCase(Locale.ROOT)) {
            case "title": comparator = Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER); break;
            case "author": comparator = Comparator.comparing(Book::getAuthor, String.CASE_INSENSITIVE_ORDER); break;
            case "price": comparator = Comparator.comparingDouble(Book::getPrice); break;
            default: throw new LibraryException("Sort field must be title, author, or price.");
        }
        result.sort(comparator);
        return result;
    }


    public Book getBook(String id) throws LibraryException {
        validateId(id, "Book ID");
        Book book = booksById.get(id.trim());
        if (book == null) throw new LibraryException("Book not found: " + id);
        return book;
    }

    public Student getStudent(String id) throws LibraryException { return requireStudent(id); }
    public Student findStudent(String id) {
        if (id == null) return null;
        for (Student student : students) if (student.getId().equalsIgnoreCase(id.trim())) return student;
        return null;
    }
    private Student requireStudent(String id) throws LibraryException {
        validateId(id, "Student ID");
        Student student = findStudent(id);
        if (student == null) throw new LibraryException("Student not found: " + id);
        return student;
    }
    public List<Book> getBooks() { return new ArrayList<>(books); }
    public List<Student> getStudents() { return new ArrayList<>(students); }
    public List<Transaction> getHistory() { return new ArrayList<>(history); }
    public List<Book> getBooksById() { return new ArrayList<>(booksBySortedId.values()); }
    public List<Book> getBooksByTitle() { return new ArrayList<>(booksByTitle.values()); }

// These are helper methods used by the LibraryService to validate and organize data. They validate books, students, emails, dates and IDs, clean required input
// create keys for loans and titles, and index books into different maps. This keeps the main business methods like issue, return, add and update clean and organized.”
    

    private void validateBook(Book b) throws LibraryException {
        if (b == null) throw new LibraryException("Book is required.");
        validateId(b.getId(), "Book ID");
        required(b.getIsbn(), "ISBN"); required(b.getTitle(), "Title"); required(b.getAuthor(), "Author");
        if (b.getPrice() < 0 || !Double.isFinite(b.getPrice())) throw new LibraryException("Price must be a valid non-negative number.");
    }
    private void validateStudent(Student s) throws LibraryException {
        if (s == null) throw new LibraryException("Student is required.");
        validateId(s.getId(), "Student ID");
        required(s.getName(), "Student name");
        s.setEmail(validateEmail(s.getEmail()));
    }
    private String validateEmail(String email) throws LibraryException {
        String value = required(email, "Email");
        if (!value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new LibraryException("Enter a valid email address.");
        return value;
    }
    private void validateDate(LocalDate date) throws LibraryException {
        if (date == null) throw new LibraryException("Date is required.");
        if (date.isAfter(LocalDate.now())) throw new LibraryException("Date cannot be in the future.");
    }
    private void validateId(String id, String label) throws LibraryException {
        if (id == null || !id.equals(id.trim()) || !id.matches("[A-Za-z0-9_-]{2,20}")) throw new LibraryException(label + " must be 2-20 letters, digits, underscores, or hyphens, with no surrounding spaces.");
    }
    private String required(String value, String label) throws LibraryException {
        if (value == null || value.trim().isEmpty()) throw new LibraryException(label + " is required.");
        return value.trim();
    }
    private String titleKey(Book book) { return book.getTitle() + " [" + book.getId() + "]"; }
    private String loanKey(String bookId, String studentId) { return bookId.trim() + "|" + studentId.trim(); }
    private void indexBook(Book book) {
        booksById.put(book.getId(), book);
        booksBySortedId.put(book.getId(), book);
        booksByTitle.put(titleKey(book), book);
    }
}
