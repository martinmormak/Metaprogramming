package sk.tuke.meta.persistence;

public interface TransactionalPersistenceManager extends PersistenceManager {
    void beginTransaction();
    void commitTransaction();
    void rollbackTransaction();
}
