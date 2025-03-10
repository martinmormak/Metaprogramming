package sk.tuke.meta.persistence.database;

import java.util.*;

public class DatabaseTable {
    private final String name;
    private final List<DatabaseColumn> databaseColumnList;
    private List<String> foreignKeyList;
    private boolean created;

    public DatabaseTable(String name, List<DatabaseColumn> databaseColumnList, boolean created) {
        this.name = name;
        this.databaseColumnList = databaseColumnList;
        this.created = created;
        getForeignKeyList(databaseColumnList);
    }

    public String getName() {
        return name;
    }

    public List<DatabaseColumn> getDatabaseColumnList() {
        return databaseColumnList;
    }

    public Integer getForeignKeyListSize() {
        return foreignKeyList.size();
    }

    public List<String> getForeignKeyList() {
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
                foreignKeyList.add(databaseColumn.name());
            }
        }
    }
}
