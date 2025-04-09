package sk.tuke.meta.persistence.entity;

import sk.tuke.meta.persistence.annotations.Table;

public class FKNameEntity {
    private final String javaName;
    private final String SQLAlias;
    private final Class<?> targetClass;
    private String referencedTable = "";

    public FKNameEntity (String javaName, String SQLAlias, Class<?> targetClass) {
        this.javaName = javaName;
        this.SQLAlias = SQLAlias;
        this.targetClass = targetClass;
    }

    public FKNameEntity (String javaName, String SQLAlias, Class<?> targetClass, String referencedTable) {
        this.javaName = javaName;
        this.SQLAlias = SQLAlias;
        this.targetClass = targetClass;
        this.referencedTable = referencedTable;
    }

    public String getJavaName() {
        return javaName;
    }

    public String getSQLAlias() {
        return SQLAlias;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public String getReferencedTable() {
        return referencedTable;
    }
}
