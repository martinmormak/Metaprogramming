package sk.tuke.meta.persistence.entity;

import sk.tuke.meta.persistence.annotations.Table;

public class FKNameEntity {
    private final String javaName;
    private final String SQLAlias;
    private final boolean lazyFetch;
    private final String targetClass;
    private final String pkFieldName;
    private final String SQLFieldName;
    private String referencedTable = "";

    public FKNameEntity (String javaName, String SQLAlias, boolean lazyFetch, String targetClass, String pkFieldName, String SQLFieldName) {
        this.javaName = javaName;
        this.SQLAlias = SQLAlias;
        this.lazyFetch = lazyFetch;
        this.targetClass = targetClass;
        this.pkFieldName = pkFieldName;
        this.SQLFieldName = SQLFieldName;
    }

    public FKNameEntity (String javaName, String SQLAlias, boolean lazyFetch, String targetClass, String pkFieldName, String SQLFieldName, String referencedTable) {
        this.javaName = javaName;
        this.SQLAlias = SQLAlias;
        this.lazyFetch = lazyFetch;
        this.targetClass = targetClass;
        this.pkFieldName = pkFieldName;
        this.SQLFieldName = SQLFieldName;
        this.referencedTable = referencedTable;
    }

    public String getJavaName() {
        return javaName;
    }

    public String getSQLAlias() {
        return SQLAlias;
    }

    public boolean isLazyFetch() {
        return lazyFetch;
    }

    public String getTargetClass() {
        return targetClass.substring(targetClass.lastIndexOf('.') + 1);
    }

    public String getReferencedTable() {
        if(referencedTable == null || referencedTable.isEmpty()){
            return getTargetClass();
        }
        return referencedTable;
    }

    public String getPKFieldName() {
        return pkFieldName;
    }

    public String getSQLFieldName() {
        return SQLFieldName;
    }

    public String getFieldGetterName() {
        return "get" + javaName.substring(0, 1).toUpperCase() + javaName.substring(1);
    }

    public String getPKGetterName() {
        return "get" + pkFieldName.substring(0, 1).toUpperCase() + pkFieldName.substring(1);
    }
}
