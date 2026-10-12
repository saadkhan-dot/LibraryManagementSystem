# College Library Management System

A beginner-friendly Java Swing case study. Data is kept in memory while the application is running; closing the app resets it to the sample data in `Main`.

## Requirements

JDK 8 or newer. No external libraries or build tool are required.

## Compile and run

From the project root:

```sh
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out Main
```

On a machine without a graphical desktop, the service can still be compiled and the smoke checks run:

```sh
javac -cp out -d out test/LibraryServiceSmokeTest.java
java -cp out LibraryServiceSmokeTest
```

## Features

- Register, edit, and delete books and members.
- Search books by ISBN, title, or author; sort by title, author, or price.
- Issue books for 14 days, return them, and calculate a fine of 1.00 per late day.
- Review issue and return records in transaction history.
- Validate identifiers, email addresses, dates, prices, availability, and operation state.
- Includes sample books and students to explore immediately.

The app uses `ArrayList` for the working book/member lists, `HashMap` for direct book lookup and active loans, `TreeMap` indexes for ID/title order, and `LinkedList` for transaction history.
