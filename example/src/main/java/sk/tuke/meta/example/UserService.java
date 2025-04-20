package sk.tuke.meta.example;

import sk.tuke.meta.persistence.PersistenceManager;
import sk.tuke.meta.persistence.annotations.AtomicPersistenceOperation;

public class UserService {
    private final PersistenceManager manager;

    public UserService(PersistenceManager manager) {
        this.manager = manager;
    }

    @AtomicPersistenceOperation
    public void transactionalSave() {
        Department department = new Department("Transakcia", "TST", 1);
        manager.save(department);

        Person p = new Person("Janko", "Mrkvicka", 22);
        p.setIDepartment(department);
        manager.save(p);

        // Simuluj chybu
        if (true) {
            throw new RuntimeException("BOOM - rollback test");
        }

        Person another = new Person("Fero", "Hruška", 25);
        manager.save(another);
    }
}
