package sk.tuke.meta.persistence.database;

import sk.tuke.meta.persistence.PersistenceException;
import sk.tuke.meta.persistence.annotations.Table;
import sk.tuke.meta.persistence.entity.FKNameEntity;

import java.util.*;

public class DatabaseTable {
    private final Table annotation;
    private final String name;
    private final List<DatabaseColumn> databaseColumnList;
    private List<FKNameEntity> foreignKeyList;
    private boolean created;

    public DatabaseTable(Table annotation, String name, List<DatabaseColumn> databaseColumnList, boolean created) {
        this.annotation = annotation;
        this.name = name;
        this.databaseColumnList = databaseColumnList;
        this.created = created;
        getForeignKeyList(databaseColumnList);
    }

    public String getSQLAlias() {
        return annotation==null || annotation.name()==null || annotation.name().isEmpty()?name:annotation.name();
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
            Class<?> columnType = databaseColumn.type();
            if (!columnType.isPrimitive() && !columnType.equals(Integer.class) && !columnType.equals(Float.class)
                    && !columnType.equals(Double.class) && !columnType.equals(String.class)) {
                if(databaseColumn.annotation().targetClass()!=null) {
                    foreignKeyList.add(new FKNameEntity(databaseColumn.name(), databaseColumn.getSQLAlias(), databaseColumn.annotation().targetClass()));
                } else {
                    foreignKeyList.add(new FKNameEntity(databaseColumn.name(), databaseColumn.getSQLAlias(), databaseColumn.getClass()));
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
