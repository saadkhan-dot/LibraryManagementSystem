package gui;

import exception.LibraryException;
import model.Book;
import model.Librarian;
import model.Student;
import model.Transaction;
import service.LibraryService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;

/** Swing front end. All business rules remain in LibraryService. */
public class LibraryGUI extends JFrame {
    private final LibraryService service;
    private final Librarian librarian;
    private final DefaultTableModel bookModel = model("ID", "ISBN", "Title", "Author", "Price", "Status");
    private final DefaultTableModel studentModel = model("Student ID", "Name", "Email");
    private final DefaultTableModel historyModel = model("Type", "Book ID", "Student ID", "Date", "Due Date", "Fine");
    private final JTable bookTable = new JTable(bookModel);
    private final JTable studentTable = new JTable(studentModel);
    private final JTable historyTable = new JTable(historyModel);
    private final JTextField searchField = new JTextField(18);

    public LibraryGUI(LibraryService service, Librarian librarian) {
        super("College Library Management System");
        this.service = service;
        this.librarian = librarian;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        JLabel welcome = new JLabel("  College Library Management System     Librarian: " + librarian.getName());
        add(welcome, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Books / Search / Sort", bookPanel());
        tabs.addTab("Members", studentPanel());
        tabs.addTab("Issue / Return", circulationPanel());
        tabs.addTab("Transaction History", historyPanel());
        add(tabs, BorderLayout.CENTER);
        refreshAll();
    }

    private JPanel bookPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Register Book"), edit = new JButton("Edit Selected"), delete = new JButton("Delete Selected");
        JButton search = new JButton("Search ISBN / title / author"), showAll = new JButton("Show All");
        JComboBox<String> sort = new JComboBox<>(new String[]{"No sort", "Title", "Author", "Price"});
        controls.add(add); controls.add(edit); controls.add(delete);
        controls.add(new JLabel("Search:")); controls.add(searchField); controls.add(search); controls.add(showAll);
        controls.add(new JLabel("Sort:")); controls.add(sort);
        add.addActionListener(e -> addBook());
        edit.addActionListener(e -> editBook());
        delete.addActionListener(e -> deleteBook());
        search.addActionListener(e -> showBooks(service.searchBooks(searchField.getText())));
        showAll.addActionListener(e -> refreshBooks());
        sort.addActionListener(e -> {
            String field = (String) sort.getSelectedItem();
            if (field != null && !field.equals("No sort")) safe(() -> showBooks(service.getBooksSorted(field.toLowerCase())));
            else refreshBooks();
        });
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel studentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Register Member"), edit = new JButton("Edit Selected"), delete = new JButton("Delete Selected");
        controls.add(add); controls.add(edit); controls.add(delete);
        add.addActionListener(e -> addStudent());
        edit.addActionListener(e -> editStudent());
        delete.addActionListener(e -> deleteStudent());
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel circulationPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 18));
        JTextField bookId = new JTextField(10), studentId = new JTextField(10), date = new JTextField(LocalDate.now().toString(), 12);
        JButton issue = new JButton("Issue Book"), ret = new JButton("Return Book (calculate fine)");
        panel.add(new JLabel("Book ID:")); panel.add(bookId);
        panel.add(new JLabel("Student ID:")); panel.add(studentId);
        panel.add(new JLabel("Date (YYYY-MM-DD):")); panel.add(date);
        panel.add(issue); panel.add(ret);
        issue.addActionListener(e -> safe(() -> {
            Transaction t = service.issueBook(bookId.getText(), studentId.getText(), LocalDate.parse(date.getText().trim()));
            refreshAll();
            message("Book issued. Due date: " + t.getDueDate());
        }));
        ret.addActionListener(e -> safe(() -> {
            Transaction t = service.returnBook(bookId.getText(), studentId.getText(), LocalDate.parse(date.getText().trim()));
            refreshAll();
            message(String.format("Book returned. Fine: %.2f", t.getFine()));
        }));
        return panel;
    }

    private JPanel historyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh History");
        refresh.addActionListener(e -> refreshHistory());
        panel.add(refresh, BorderLayout.SOUTH);
        return panel;
    }

    private void addBook() {
        JTextField id = field(), isbn = field(), title = field(), author = field(), price = field();
        if (!form("Register Book", new String[]{"Book ID", "ISBN", "Title", "Author", "Price"}, new JTextField[]{id,isbn,title,author,price})) return;
        safe(() -> { service.addBook(new Book(id.getText(), isbn.getText(), title.getText(), author.getText(), Double.parseDouble(price.getText().trim()))); refreshBooks(); });
    }
    private void editBook() {
        int row = bookTable.getSelectedRow(); if (row < 0) { message("Select a book first."); return; }
        String idValue = bookModel.getValueAt(row, 0).toString();
        safe(() -> {
            Book b = service.getBook(idValue);
            JTextField isbn = new JTextField(b.getIsbn()), title = new JTextField(b.getTitle()), author = new JTextField(b.getAuthor()), price = new JTextField(String.valueOf(b.getPrice()));
            if (form("Edit Book " + idValue, new String[]{"ISBN", "Title", "Author", "Price"}, new JTextField[]{isbn,title,author,price})) {
                service.updateBook(idValue, isbn.getText(), title.getText(), author.getText(), Double.parseDouble(price.getText().trim())); refreshBooks();
            }
        });
    }
    private void deleteBook() {
        int row = bookTable.getSelectedRow(); if (row < 0) { message("Select a book first."); return; }
        String id = bookModel.getValueAt(row, 0).toString();
        safe(() -> { service.deleteBook(id); refreshBooks(); });
    }
    private void addStudent() {
        JTextField id = field(), name = field(), email = field();
        if (!form("Register Member", new String[]{"Student ID", "Name", "Email"}, new JTextField[]{id,name,email})) return;
        safe(() -> { service.registerStudent(new Student(id.getText(), name.getText(), email.getText())); refreshStudents(); });
    }
    private void editStudent() {
        int row = studentTable.getSelectedRow(); if (row < 0) { message("Select a member first."); return; }
        String id = studentModel.getValueAt(row, 0).toString();
        safe(() -> {
            Student s = service.getStudent(id);
            JTextField name = new JTextField(s.getName()), email = new JTextField(s.getEmail());
            if (form("Edit Member " + id, new String[]{"Name", "Email"}, new JTextField[]{name,email})) { service.updateStudent(id, name.getText(), email.getText()); refreshStudents(); }
        });
    }
    private void deleteStudent() {
        int row = studentTable.getSelectedRow(); if (row < 0) { message("Select a member first."); return; }
        String id = studentModel.getValueAt(row, 0).toString(); safe(() -> { service.deleteStudent(id); refreshStudents(); });
    }

    private boolean form(String title, String[] labels, JTextField[] fields) {
        Object[] items = new Object[labels.length * 2];
        for (int i = 0; i < labels.length; i++) { items[i * 2] = labels[i] + ":"; items[i * 2 + 1] = fields[i]; }
        return JOptionPane.showConfirmDialog(this, items, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }
    private void safe(Action action) {
        try { action.run(); }
        catch (LibraryException | NumberFormatException | java.time.format.DateTimeParseException ex) { message("Could not complete operation: " + ex.getMessage()); }
    }
    private void refreshAll() { refreshBooks(); refreshStudents(); refreshHistory(); }
    private void refreshBooks() { showBooks(service.getBooksById()); }
    private void showBooks(List<Book> books) {
        bookModel.setRowCount(0);
        for (Book b : books) bookModel.addRow(new Object[]{b.getId(), b.getIsbn(), b.getTitle(), b.getAuthor(), String.format("%.2f", b.getPrice()), b.isAvailable() ? "Available" : "Issued"});
    }
    private void refreshStudents() {
        studentModel.setRowCount(0);
        for (Student s : service.getStudents()) studentModel.addRow(new Object[]{s.getId(), s.getName(), s.getEmail()});
    }
    private void refreshHistory() {
        historyModel.setRowCount(0);
        for (Transaction t : service.getHistory()) historyModel.addRow(new Object[]{t.getType(), t.getBookId(), t.getStudentId(), t.getDate(), t.getDueDate(), String.format("%.2f", t.getFine())});
    }
    private void message(String text) { JOptionPane.showMessageDialog(this, text); }
    private static JTextField field() { return new JTextField(18); }
    private static DefaultTableModel model(String... columns) { return new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } }; }
    @FunctionalInterface private interface Action { void run() throws LibraryException; }

    public static void launch(LibraryService service, Librarian librarian) {
        SwingUtilities.invokeLater(() -> new LibraryGUI(service, librarian).setVisible(true));
    }
}
