package sk.tuke.meta.persistence.reflection;

import sk.tuke.meta.persistence.PersistenceException;
import sk.tuke.meta.persistence.PersistenceManager;
import sk.tuke.meta.persistence.database.DatabaseColumn;
import sk.tuke.meta.persistence.database.DatabaseTable;
import sk.tuke.meta.persistence.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TableReflection {
    PersistenceManager persistenceManager;

    public TableReflection(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    private List<DatabaseColumn> createDatabaseColumns(Class<?> type) {
        List<DatabaseColumn> databaseColumnsList = new LinkedList<>();
        for (Field field : type.getDeclaredFields()) {
            databaseColumnsList.add(new DatabaseColumn(field.getType(), field.getName()));
        }
        return databaseColumnsList;
    }

    public List<DatabaseTable> createDatabaseTables(Class<?>... types) {
        List<DatabaseTable> databaseTableList = new ArrayList<>();
        for (Class<?> type : types) {
            databaseTableList.add(createDatabaseTable(type));
        }
        databaseTableList.sort(Comparator.comparing(DatabaseTable::getForeignKeyListSize));
        return databaseTableList;
    }

    public DatabaseTable createDatabaseTable(Class<?> type) {
        return new DatabaseTable(type.getSimpleName(), createDatabaseColumns(type), false);
    }

    public <T> int prepareStatementWithExceptionList(T entity, PreparedStatement preparedStatement, DatabaseTable databaseTable, List<String> exceptionList) {
        return prepareStatementWithExceptionList(1, entity, preparedStatement, databaseTable, exceptionList);
    }

    public <T> int prepareStatementWithExceptionList(int index, T entity, PreparedStatement preparedStatement, DatabaseTable databaseTable, List<String> exceptionList) {
        LinkedList<Entity> columnEntities = getColumnValues(entity,databaseTable);
        for (Entity columnEntity : columnEntities) {
            if (!exceptionList.contains(columnEntity.name())) {
                try {
                    if(!databaseTable.getForeignKeyList().contains(columnEntity.name())) {
                        preparedStatement.setObject(index++, columnEntity.value());
                    } else {
                        DatabaseTable foreignDatabaseTable = this.createDatabaseTable(columnEntity.value().getClass());
                        if (foreignDatabaseTable == null) {
                            throw new PersistenceException("Cannot get id from foreign database table");
                        }
                        long id = (long) this.getFieldValue(columnEntity.value(), foreignDatabaseTable, "id");
                        preparedStatement.setObject(index++, id);
                    }
                } catch (SQLException e) {
                    throw new PersistenceException("Cannot prepare statement", e);
                }
            }
        }
        return index;
    }

    public <T> int prepareStatementWithExcludedList(T entity, PreparedStatement preparedStatement, DatabaseTable databaseTable, List<String> excludedList) {
        return prepareStatementWithExcludedList(1, entity, preparedStatement, databaseTable, excludedList);
    }

    public <T> int prepareStatementWithExcludedList(int index, T entity, PreparedStatement preparedStatement, DatabaseTable databaseTable, List<String> excludedList) {
        List<String> exceptionList = new ArrayList<>();
        for (DatabaseColumn databaseColumn : databaseTable.getDatabaseColumnList()) {
            if (!excludedList.contains(databaseColumn.name())) {
                exceptionList.add(databaseColumn.name());
            }
        }
        return prepareStatementWithExceptionList(index, entity, preparedStatement, databaseTable, exceptionList);
    }

    public <T> Object getFieldValue(T entity, DatabaseTable databaseTable, String fieldName) {
        return getColumnValues(entity,databaseTable).stream()
                .filter(columnEntity -> columnEntity.name().equals(fieldName))
                .map(Entity::value)
                .findFirst()
                .orElse(null);
    }

    public <T> void setField(T entity, Object fieldValue, String fieldName) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);

            Class<?> fieldType = field.getType();

            if (fieldType.isPrimitive()) {
                field.set(entity, fieldValue);
            } else if (fieldValue == null || fieldType.isAssignableFrom(fieldValue.getClass())) {
                field.set(entity, fieldValue);
            } else {
                throw new RuntimeException("Field Value must be of type " + fieldType + ", but got " + fieldValue.getClass());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to set " + fieldName, e);
        }
    }

    public <T> T getInstanceFromDatabase(Class<T> type, ResultSet resultSet, DatabaseTable databaseTable) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            List<DatabaseColumn> databaseColumns = databaseTable.getDatabaseColumnList();

            for (DatabaseColumn databaseColumn : databaseColumns) {
                Field field = type.getDeclaredField(databaseColumn.name());
                field.setAccessible(true);
                Object value;

                if (field.getType() == int.class || field.getType() == Integer.class) {
                    value = resultSet.getInt(databaseColumn.name());
                } else if (field.getType() == long.class || field.getType() == Long.class) {
                    value = resultSet.getLong(databaseColumn.name());
                } else if (field.getType() == double.class || field.getType() == Double.class) {
                    value = resultSet.getDouble(databaseColumn.name());
                } else if (field.getType() == String.class) {
                    value = resultSet.getString(databaseColumn.name());
                } else {
                    value = resultSet.getObject(databaseColumn.name());
                    if (databaseTable.getForeignKeyList().contains(databaseColumn.name())) {
                        Optional<?> optionalValue = persistenceManager.get(field.getType(), resultSet.getLong(databaseColumn.name()));
                        value = optionalValue.orElse(null);
                    }
                }
                field.set(instance, value);
            }
            return instance;
        } catch (SQLException | InvocationTargetException | IllegalAccessException | InstantiationException |
                 NoSuchMethodException | NoSuchFieldException e) {
            throw new PersistenceException("Error executing SQL", e);
        }
    }

    public LinkedList<Entity> getColumnValues(Object entity, DatabaseTable databaseTable) {
        LinkedList<Entity> values = new LinkedList<>();
        for (DatabaseColumn column : databaseTable.getDatabaseColumnList()) {
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
}

