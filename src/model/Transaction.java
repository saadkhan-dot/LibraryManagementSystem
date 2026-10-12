package model;

import java.time.LocalDate;

/** Immutable record of a book issue or return. */
public class Transaction {
    private final String type;
    private final String bookId;
    private final String studentId;
    private final LocalDate date;
    private final LocalDate dueDate;
    private final double fine;

    public Transaction(String type, String bookId, String studentId, LocalDate date, LocalDate dueDate, double fine) {
        this.type = type;
        this.bookId = bookId;
        this.studentId = studentId;
        this.date = date;
        this.dueDate = dueDate;
        this.fine = fine;
    }

    public String getType() { return type; }
    public String getBookId() { return bookId; }
    public String getStudentId() { return studentId; }
    public LocalDate getDate() { return date; }
    public LocalDate getDueDate() { return dueDate; }
    public double getFine() { return fine; }
}
