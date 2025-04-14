package sk.tuke.meta.persistence.entity;

import sk.tuke.meta.persistence.annotations.Table;

public class FKNameEntity {
    private final String javaName;
    private final String SQLAlias;
    private final String targetClass;
    private String referencedTable = "";

    public FKNameEntity (String javaName, String SQLAlias, String targetClass) {
        this.javaName = javaName;
        this.SQLAlias = SQLAlias;
        this.targetClass = targetClass;
    }

    public FKNameEntity (String javaName, String SQLAlias, String targetClass, String referencedTable) {
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

    public String getTargetClass() {
        return targetClass.substring(targetClass.lastIndexOf('.') + 1);
    }

    public String getReferencedTable() {
        return referencedTable;
    }
}
