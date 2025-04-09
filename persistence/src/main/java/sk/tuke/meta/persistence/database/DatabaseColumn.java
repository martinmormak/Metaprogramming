package sk.tuke.meta.persistence.database;

import sk.tuke.meta.persistence.annotations.Column;

public class DatabaseColumn {
    private final Class<?> type;
    private final String name;
    private final String columnName;
    private final boolean nullable;
    private final boolean unique;
    private final boolean lazyFetch;
    private final Class<?> targetClass;
    private final boolean isPrimaryKey;
    public DatabaseColumn(Class<?> type, String name, String columnName, boolean nullable, boolean unique,
                          boolean lazyFetch, Class<?> targetClass, boolean isPrimaryKey) {
        this.type = type;
        this.name = name;
        this.columnName = columnName;
        this.nullable = nullable;
        this.unique = unique;
        this.lazyFetch = lazyFetch;
        this.targetClass = targetClass;
        this.isPrimaryKey = isPrimaryKey;
    }

    public DatabaseColumn(Class<?> type, String name, boolean nullable, boolean unique,
                          boolean lazyFetch, Class<?> targetClass, boolean isPrimaryKey) {
        this(type, name, name, nullable, unique, lazyFetch, targetClass, isPrimaryKey);
    }


    public DatabaseColumn(Class<?> type, String name, String columnName, boolean nullable, boolean unique, boolean isPrimaryKey) {
        this(type, name, columnName, nullable, unique, false, void.class, isPrimaryKey);
    }

    public DatabaseColumn(Class<?> type, String name, boolean nullable, boolean unique, boolean isPrimaryKey) {
        this(type, name, name, nullable, unique, false, void.class, isPrimaryKey);
    }

    public DatabaseColumn(Class<?> type, String name, Column columnAnnotation, boolean isPrimaryKey) {
        this(type, name, columnAnnotation.name(), columnAnnotation.nullable(), columnAnnotation.unique(), columnAnnotation.lazyFetch(), columnAnnotation.targetClass(), isPrimaryKey);
    }

    public Class<?> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getColumnName() {
        return columnName;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isUnique() {
        return unique;
    }

    public boolean isLazyFetch() {
        return lazyFetch;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public String getSQLAlias(){
        return columnName==null || columnName.isEmpty() ?name:columnName;
    }
}
