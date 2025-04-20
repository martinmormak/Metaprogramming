package sk.tuke.meta.persistence.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AtomicPersistenceOperation {
}