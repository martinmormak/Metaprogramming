package sk.tuke.meta.persistence.database;

import sk.tuke.meta.persistence.PersistenceException;
import sk.tuke.meta.persistence.annotations.Table;
import sk.tuke.meta.persistence.entity.FKNameEntity;

import java.util.*;

public class DatabaseTable {
    private final String name;
    private final String SQLAlias;
    private final List<DatabaseColumn> databaseColumnList;
    private List<FKNameEntity> foreignKeyList;
    private boolean created;

    public DatabaseTable(String name, String SQLAlias, List<DatabaseColumn> databaseColumnList, boolean created) {
        this.name = name;
        this.SQLAlias = SQLAlias;
        this.databaseColumnList = databaseColumnList;
        this.created = created;
        getForeignKeyList(databaseColumnList);
    }

    public DatabaseTable(String name, Table tableAnnotation, List<DatabaseColumn> databaseColumnList, boolean created) {
        this(name, tableAnnotation.name(), databaseColumnList, created);
    }

    public String getSQLAlias() {
        return SQLAlias==null || SQLAlias.isEmpty()?name:SQLAlias;
    }

    public List<DatabaseColumn> getDatabaseColumnList() {
        return databaseColumnList;
    }

    public Integer getForeignKeyListSize() {
        return foreignKeyList.size();
    }

    public List<FKNameEntity> getForeignKeyList() {
        return foreignKeyList;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    private void getForeignKeyList(List<DatabaseColumn> databaseColumnsList) {
        foreignKeyList = new ArrayList<>();
        for (DatabaseColumn databaseColumn : databaseColumnsList) {
            Class<?> columnType = databaseColumn.getType();
            if (!columnType.isPrimitive() && !columnType.equals(Integer.class) && !columnType.equals(Float.class)
                    && !columnType.equals(Double.class) && !columnType.equals(String.class)) {
                if(databaseColumn.getTargetClass()!=null) {
                    foreignKeyList.add(new FKNameEntity(databaseColumn.getName(), databaseColumn.getSQLAlias(), databaseColumn.getTargetClass()));
                } else {
                    foreignKeyList.add(new FKNameEntity(databaseColumn.getName(), databaseColumn.getSQLAlias(), databaseColumn.getClass()));
                }
            }
        }
    }

    public String getPrimaryKey() {
        for(DatabaseColumn databaseColumn : databaseColumnList){
            if(databaseColumn.isPrimaryKey()){
                return databaseColumn.getSQLAlias();
            }
        }
        throw new PersistenceException("Primary keys doesn't exists");
    }
}
