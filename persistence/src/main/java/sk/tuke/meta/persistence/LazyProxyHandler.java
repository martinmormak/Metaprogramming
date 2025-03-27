package sk.tuke.meta.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

public class LazyProxyHandler<T> implements InvocationHandler {
    private final Class<T> targetClass;
    private final Supplier<T> loader;
    private T realObject;
    private final Long ID;

    public LazyProxyHandler(Class<T> targetClass, Long ID, Supplier<T> loader) {
        this.targetClass = targetClass;
        this.loader = loader;
        this.ID = ID;
    }

    public Object getID(){
        return ID;
    }

    public boolean isInitialized() {
        return realObject != null;
    }

    public T getRealObject() {
        if(realObject == null) {
            realObject = loader.get();
        }
        return realObject;
    }

    public Class<T> getTargetClass() {
        return targetClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (realObject == null) {
            realObject = loader.get(); // Načítanie skutočného objektu
        }
        return method.invoke(realObject, args);
    }

    public static <T> T createProxy(Class<T> interfaceType, Class<?> targetClass, Long ID, Supplier<?> loader) {
        return interfaceType.cast(
                Proxy.newProxyInstance(
                        interfaceType.getClassLoader(),
                        new Class<?>[]{interfaceType},
                        new LazyProxyHandler<>((Class<T>) targetClass, ID, (Supplier<T>) loader)
                )
        );
    }
}
