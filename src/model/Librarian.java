package model;

/** Identifies the librarian operating this single-user case-study system. */
public class Librarian {
    private final String name;

    public Librarian(String name) { this.name = name; }
    public String getName() { return name; }
}
