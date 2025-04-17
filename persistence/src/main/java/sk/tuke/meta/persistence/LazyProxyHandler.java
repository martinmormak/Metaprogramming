package sk.tuke.meta.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

public class LazyProxyHandler<T> implements InvocationHandler {
    private final Class<T> targetClass;
    Long id;
    PersistenceManager persistenceManager;
    private T realObject;

    public LazyProxyHandler(Class<T> targetClass, Long id, PersistenceManager persistenceManager) {
        this.targetClass = targetClass;
        this.id = id;
        this.persistenceManager = persistenceManager;
    }

    public boolean isInitialized() {
        return realObject != null;
    }

    public T getRealObject() {
        if(realObject == null) {
            realObject = persistenceManager.get(targetClass, id).orElse(null);
        }
        return realObject;
    }

    public Long getId() {
        return id;
    }

    public Class<T> getTargetClass() {
        return targetClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (realObject == null) {
            realObject = persistenceManager.get(targetClass, id).orElse(null); // Načítanie skutočného objektu
            if(realObject == null) {
                System.out.println("Cannot get object with id " + id + " from database");
                return null;
            }
        }
        return method.invoke(realObject, args);
    }

    public static <T> T createProxy(Class<T> interfaceType, Class<?> targetClass, Long id, PersistenceManager persistenceManager) {
        return interfaceType.cast(
                Proxy.newProxyInstance(
                        interfaceType.getClassLoader(),
                        new Class<?>[]{interfaceType},
                        new LazyProxyHandler<>((Class<T>) targetClass, id, persistenceManager)
                )
        );
    }
}
