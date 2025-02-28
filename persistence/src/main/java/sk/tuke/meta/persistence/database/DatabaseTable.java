package sk.tuke.meta.persistence.database;

import sk.tuke.meta.persistence.PersistenceException;
import sk.tuke.meta.persistence.entity.Entity;

import java.lang.reflect.Field;
import java.util.*;

public class DatabaseTable {
    private static final List<String> SQL_KEYWORDS = Arrays.asList("SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE", "TRUNCATE", "EXEC", "UNION", "--", ";");

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

    public LinkedList<Entity> getColumnValues(Object entity) {
        LinkedList<Entity> values = new LinkedList<>();
        for (DatabaseColumn column : databaseColumnList) {
            Field field;
            try {
                field = entity.getClass().getDeclaredField(column.name());
                field.setAccessible(true);
                values.add(new Entity(column.name(), field.get(entity)));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new PersistenceException("No such field: " + column.name());
            }
        }
        return values;
    }

    public boolean checkIfContainsSQLCommands() {
        for (DatabaseColumn column : databaseColumnList) {
            if(SQL_KEYWORDS.contains(column.name())) {
                return false;
            }
        }
        return true;
    }

    public boolean checkIfContainsSQLCommands(Object entity) {
        for (DatabaseColumn column : databaseColumnList) {
            Field field;
            try {
                field = entity.getClass().getDeclaredField(column.name());
                field.setAccessible(true);
                if(SQL_KEYWORDS.contains(field.get(entity).toString().toUpperCase())) {
                    return false;
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new PersistenceException("No such field: " + column.name());
            }
        }
        return true;
    }
}
