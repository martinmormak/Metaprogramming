package sk.tuke.meta.persistence.annotations;

public @interface Column {
    String name() default "";
    boolean nullable() default true;
    boolean unique() default false;
}
