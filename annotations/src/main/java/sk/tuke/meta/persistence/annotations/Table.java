package sk.tuke.meta.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface Table {
    String name() default "";
}
