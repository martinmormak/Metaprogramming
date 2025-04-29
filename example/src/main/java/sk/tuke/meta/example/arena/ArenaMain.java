package sk.tuke.meta.example.arena;

import sk.tuke.meta.persistence.GeneratedPersistenceManager;
import sk.tuke.meta.persistence.PersistenceManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class ArenaMain {
    private static final String DB_PATH = "test.db";

    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        PersistenceManager manager = new GeneratedPersistenceManager(conn);

        // Create tables
        manager.createTables(
                Author.class,
                Book.class,
                Chapter.class,
                ExcludedColumn.class,
                MovedId.class,
                ReferenceOnMovedId.class,
                RenamedColumn.class,
                RenamedId.class,
                RenamedTable.class,
                Simple.class
        );

        // Insert and display all entities
        exampleOperations(manager);

        // Simulate transaction with rollback
        System.out.println("\n--- Transactional Test ---");
        ExampleService service = new ExampleService(manager);
        try {
            service.transactionalSave();
        } catch (Exception e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }

        conn.close();
    }

    private static void exampleOperations(PersistenceManager manager) {
        System.out.println("\n--- Inserting All Entities ---");

        Author author = new Author();
        author.setName("Agatha");
        author.setSurname("Christie");
        manager.save(author);

        Book book = new Book();
        book.setTitle("Murder on the Orient Express");
        book.setIsbn("9780007119318");
        book.setYear(1934);
        book.setPrice(14.99);
        book.setAuthor(author);
        manager.save(book);

        Chapter chapter = new Chapter();
        chapter.setTitle("The Crime");
        chapter.setBook(book);
        chapter.setAuthor(author);
        manager.save(chapter);

        ExcludedColumn excluded = new ExcludedColumn();
        excluded.setText("Hello");
        excluded.setNumber(42);
        manager.save(excluded);

        MovedId moved = new MovedId();
        moved.setText("Moved ID text");
        moved.setNumber(123);
        manager.save(moved);

        ReferenceOnMovedId ref = new ReferenceOnMovedId();
        ref.setText("Reference text");
        ref.setMoved(moved);
        manager.save(ref);

        RenamedColumn renamedCol = new RenamedColumn();
        renamedCol.setText("Renamed Text");
        renamedCol.setNumber(88);
        manager.save(renamedCol);

        RenamedId renamedId = new RenamedId();
        renamedId.setText("Renamed ID Text");
        renamedId.setNumber(77);
        manager.save(renamedId);

        RenamedTable renamedTable = new RenamedTable();
        renamedTable.setText("Renamed Table Text");
        renamedTable.setNumber(99);
        manager.save(renamedTable);

        renamedTable.setNumber(1);
        manager.save(renamedTable);

        Simple simple = new Simple();
        simple.setText("Simple Text");
        simple.setNumber(11);
        manager.save(simple);

        // Print all saved entities
        System.out.println("\n--- Saved Entities ---");
        printAll(manager, Author.class);
        printAll(manager, Book.class);
        printAll(manager, Chapter.class);
        printAll(manager, ExcludedColumn.class);
        printAll(manager, MovedId.class);
        printAll(manager, ReferenceOnMovedId.class);
        printAll(manager, RenamedColumn.class);
        printAll(manager, RenamedId.class);
        printAll(manager, RenamedTable.class);
        printAll(manager, Simple.class);

        // Optionally: delete entities (just an example, could be skipped)
        System.out.println("\n--- Deleting All Entities ---");
        manager.delete(simple);
        manager.delete(renamedTable);
        manager.delete(renamedId);
        manager.delete(renamedCol);
        manager.delete(ref);
        manager.delete(moved);
        manager.delete(excluded);
        manager.delete(chapter);
        manager.delete(book);
        manager.delete(author);
    }

    private static <T> void printAll(PersistenceManager manager, Class<T> clazz) {
        System.out.println(clazz.getSimpleName() + "s:");
        List<T> items = manager.getAll(clazz);
        for (T item : items) {
            System.out.println(" - " + item);
        }
    }
}
