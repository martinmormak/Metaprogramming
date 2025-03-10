package sk.tuke.meta.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface Column {
    String name() default "";
    boolean nullable() default true;
    boolean unique() default false;
}
