package sk.tuke.meta.persistence.database;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;
import sk.tuke.meta.persistence.entity.FKNameEntity;

import javax.annotation.processing.ProcessingEnvironment;
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
    private final String targetClass;
    private String referencedTableName = "";
    private final boolean isPrimaryKey;
    public DatabaseColumn(Class<?> type, String name, String columnName, boolean nullable, boolean unique,
                          boolean lazyFetch, String targetClass, boolean isPrimaryKey) {
        this.type = type;
        this.name = name;
        this.columnName = columnName;
        this.nullable = nullable;
        this.unique = unique;
        this.lazyFetch = lazyFetch;
        this.targetClass = targetClass;
        this.isPrimaryKey = isPrimaryKey;
        System.out.println();
        System.out.println("DatabaseColumn: type " + type);
        System.out.println("DatabaseColumn: name " + name);
        System.out.println("DatabaseColumn: columnName " + columnName);
        System.out.println("DatabaseColumn: nullable " + nullable);
        System.out.println("DatabaseColumn: unique " + unique);
        System.out.println("DatabaseColumn: lazyFetch " + lazyFetch);
        System.out.println("DatabaseColumn: targetClass " + targetClass);
        System.out.println("DatabaseColumn: isPrimaryKey " + isPrimaryKey);
    }

    public DatabaseColumn(Class<?> type, String name, boolean nullable, boolean unique,
                          boolean lazyFetch, String targetClass, boolean isPrimaryKey) {
        this(type, name, name, nullable, unique, lazyFetch, targetClass, isPrimaryKey);
    }


    public DatabaseColumn(Class<?> type, String name, String columnName, boolean nullable, boolean unique, boolean isPrimaryKey) {
        this(type, name, columnName, nullable, unique, false, "void", isPrimaryKey);
    }

    public DatabaseColumn(Class<?> type, String name, boolean nullable, boolean unique, boolean isPrimaryKey) {
        this(type, name, name, nullable, unique, false, "void", isPrimaryKey);
    }

    public DatabaseColumn(Class<?> type, String name, Column columnAnnotation, boolean isPrimaryKey) {
        this(type, name, columnAnnotation.name(), columnAnnotation.nullable(), columnAnnotation.unique(), columnAnnotation.lazyFetch(), getTargetClass(columnAnnotation), isPrimaryKey);
    }

    public DatabaseColumn(Class<?> type, String name, Column columnAnnotation, String referencedTableName, boolean isPrimaryKey) {
        this(type, name, columnAnnotation.name(), columnAnnotation.nullable(), columnAnnotation.unique(), columnAnnotation.lazyFetch(), getTargetClass(columnAnnotation), isPrimaryKey);
        this.referencedTableName = referencedTableName;
        System.out.println("DatabaseColumn: referencedTableName " + referencedTableName);
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

    public String getTargetClass() {
        if(targetClass.contains("void")){
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return targetClass.substring(targetClass.lastIndexOf('.') + 1);
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public String getSQLAlias(){
        return columnName==null || columnName.isEmpty() ?name:columnName;
    }

    public FKNameEntity getForeignKey(ProcessingEnvironment processingEnv) {
        String pkName = "id";
        String SQLAlias = "id";
        String resolvedReferencedTableName = "";

        TypeElement targetClassElement = processingEnv
                .getElementUtils()
                .getTypeElement(targetClass);

        //System.out.println("DatabaseColumn - getForeignKey: targetClass = " + targetClass);

        if (targetClassElement != null) {
            for (Element element : targetClassElement.getEnclosedElements()) {
                //System.out.println("DatabaseColumn - getTargetClass: element " + element);
                if (element.getKind().isField() && element.getAnnotation(Id.class) != null) {
                    pkName = element.getSimpleName().toString();
                    Column column = element.getAnnotation(Column.class);
                    SQLAlias = column.name().isEmpty() ? element.getSimpleName().toString() : column.name();
                    break;
                }
            }

            Table tableAnnotation = targetClassElement.getAnnotation(Table.class);
            if (tableAnnotation != null) {
                resolvedReferencedTableName = tableAnnotation.name().isEmpty()
                        ? targetClassElement.getSimpleName().toString()
                        : tableAnnotation.name();
            }
        }

        System.out.println("DatabaseColumn - referencedTableName = " + referencedTableName);
        System.out.println("DatabaseColumn - resolvedReferencedTableName = " + resolvedReferencedTableName);

        return new FKNameEntity(name, columnName, lazyFetch, getTargetClass(), pkName, SQLAlias, resolvedReferencedTableName);
    }

    private static String getTargetClass (Column columnAnnotation){
        TypeMirror typeMirror = null;
        try {
            //System.out.println("DatabaseColumn - getTargetClass: columnAnnotation.targetClass().getSimpleName() " + columnAnnotation.targetClass().getSimpleName());
            return columnAnnotation.targetClass().getSimpleName(); // This triggers MirroredTypeException
        } catch (MirroredTypeException e) {
            //System.out.println("DatabaseColumn - getTargetClass: e " + e);
            //System.out.println("DatabaseColumn - getTargetClass: e.getTypeMirror() " + e.getTypeMirror());
            typeMirror = e.getTypeMirror(); // Correct way to get the TypeMirror
        }
        return typeMirror.toString();
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
