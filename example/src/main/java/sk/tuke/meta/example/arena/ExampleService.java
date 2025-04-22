package sk.tuke.meta.example.arena;

import sk.tuke.meta.persistence.PersistenceManager;
import sk.tuke.meta.persistence.annotations.AtomicPersistenceOperation;
import sk.tuke.meta.persistence.annotations.Table;

public class ExampleService {
    private final PersistenceManager manager;

    public ExampleService(PersistenceManager manager) {
        this.manager = manager;
    }

    @AtomicPersistenceOperation
    public void transactionalSave() {
        Author author = new Author();
        author.setName("George");
        author.setSurname("Orwell");
        manager.save(author);

        Book book = new Book();
        book.setTitle("1984");
        book.setIsbn("123456789");
        book.setYear(1949);
        book.setPrice(24.99);
        book.setAuthor(author);
        manager.save(book);

        Chapter chapter = new Chapter();
        chapter.setTitle("Big Brother");
        chapter.setAuthor(author);
        chapter.setBook(book);
        manager.save(chapter);

        throw new RuntimeException("Simulated failure for rollback.");
    }
}