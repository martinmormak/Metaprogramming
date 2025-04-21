package sk.tuke.meta.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class DAOPersistenceManager implements PersistenceManager {
    private final Connection connection;
    private final Map<Class<?>, EntityDAO<?>> daos = new LinkedHashMap<>();

    public DAOPersistenceManager(Connection connection) {
        this.connection = connection;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityDAO<T> getDAO(Class<T> type) {
        // Types are checked in put DAO method to match properly,
        // so the cast should be OK
        return (EntityDAO<T>) daos.get(type);
    }

    protected <T> void putDAO(Class<T> type, EntityDAO<T> dao) {
        daos.put(type, dao);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void createTables(Class<?>... types) {
        for (var dao : daos.values()) {
            dao.createTable();
        }
    }


    @Override
    public <T> Optional<T> get(Class<T> type, long id) {
        return getDAO(type).get(id);
    }

    @Override
    public <T> List<T> getAll(Class<T> type) {
        return getDAO(type).getAll();
    }

    @Override
    public void save(Object entity) {
        // TODO: What if we would receive a Proxy?
        Class<?> entityClass = resolveRealClass(entity);
        getDAO(entityClass).save(entity);
    }

    @Override
    public void delete(Object entity) {
        Class<?> entityClass = resolveRealClass(entity);
        getDAO(entityClass).delete(entity);
    }

    private Class<?> resolveRealClass(Object entity) {
        Class<?> entityClass = entity.getClass();
        if (Proxy.isProxyClass(entityClass)) {
            InvocationHandler handler = Proxy.getInvocationHandler(entity);
            if (handler instanceof LazyProxyHandler lazyHandler) {
                return lazyHandler.getTargetClass(); // <-- You must add this method
            }
        }
        return entityClass;
    }
}
