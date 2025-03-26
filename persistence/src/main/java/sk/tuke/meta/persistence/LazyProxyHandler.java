package sk.tuke.meta.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

public class LazyProxyHandler<T> implements InvocationHandler {
    private final Class<T> targetClass;
    private final Supplier<T> loader;
    private T realObject;

    public LazyProxyHandler(Class<T> targetClass, Supplier<T> loader) {
        this.targetClass = targetClass;
        this.loader = loader;
    }

    public boolean isInitialized() {
        return realObject != null;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (realObject == null) {
            realObject = loader.get(); // Načítanie skutočného objektu
        }
        return method.invoke(realObject, args);
    }

    public static <T> T createProxy(Class<T> interfaceType, Class<?> targetClass, Supplier<?> loader) {
        return interfaceType.cast(
                Proxy.newProxyInstance(
                        interfaceType.getClassLoader(),
                        new Class<?>[]{interfaceType},
                        new LazyProxyHandler<>((Class<T>) targetClass, (Supplier<T>) loader)
                )
        );
    }
}
