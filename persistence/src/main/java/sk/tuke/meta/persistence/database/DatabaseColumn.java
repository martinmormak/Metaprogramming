package sk.tuke.meta.persistence.database;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Table;
import sk.tuke.meta.persistence.entity.FKNameEntity;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

public class DatabaseColumn {
    private final Class<?> type;
    private final String name;
    private final String columnName;
    private final boolean nullable;
    private final boolean unique;
    private final boolean lazyFetch;
    private final Class<?> targetClass;
    private String referencedTableName = "";
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
        this(type, name, columnAnnotation.name(), columnAnnotation.nullable(), columnAnnotation.unique(), columnAnnotation.lazyFetch(), getTargetClass(columnAnnotation), isPrimaryKey);
    }

    public DatabaseColumn(Class<?> type, String name, Column columnAnnotation, String referencedTableName, boolean isPrimaryKey) {
        this(type, name, columnAnnotation.name(), columnAnnotation.nullable(), columnAnnotation.unique(), columnAnnotation.lazyFetch(), getTargetClass(columnAnnotation), isPrimaryKey);
        this.referencedTableName = referencedTableName;
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

    public FKNameEntity getForeignKey() {
        return new FKNameEntity(name, columnName, targetClass, referencedTableName);
    }

    private static Class<?> getTargetClass (Column columnAnnotation){
        TypeMirror typeMirror = null;
        try {
            return columnAnnotation.targetClass(); // This triggers MirroredTypeException
        } catch (MirroredTypeException e) {
            typeMirror = e.getTypeMirror(); // Correct way to get the TypeMirror
        }
        Class<?> targetClass = void.class;
        if (typeMirror != null && !typeMirror.toString().equals("void")) {
            try {
                targetClass = Class.forName(typeMirror.toString());
            } catch (ClassNotFoundException e) {
                targetClass = Object.class; // best effort
            }
        }
        return targetClass;
    }


    // for refactoring

    public String getFieldName() {
        return name;
    }

    public String getSetterName() {
        return "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public String getGetterName() {
        return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
