package sk.tuke.meta.persistence.reflection;

import sk.tuke.meta.persistence.PersistenceException;
import sk.tuke.meta.persistence.PersistenceManager;
import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;
import sk.tuke.meta.persistence.database.DatabaseColumn;
import sk.tuke.meta.persistence.database.DatabaseTable;
import sk.tuke.meta.persistence.entity.Entity;
import sk.tuke.meta.persistence.entity.FKNameEntity;
import sk.tuke.meta.persistence.LazyProxyHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
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
        boolean isPrimaryKeyPresent = false;
        List<DatabaseColumn> databaseColumnsList = new LinkedList<>();
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                if(field.isAnnotationPresent(Id.class)){
                    if(isPrimaryKeyPresent){
                        throw new PersistenceException("More ten one primary key in table " + type.getName());
                    }
                    isPrimaryKeyPresent = true;
                    databaseColumnsList.add(new DatabaseColumn(field.getType(), field.getAnnotation(Column.class), field.getName(), true));
                } else {
                    databaseColumnsList.add(new DatabaseColumn(field.getType(), field.getAnnotation(Column.class), field.getName(), false));
                }
            }else {
                System.out.println("Column " + type.getSimpleName() + " in table " + type.getName() + " is not annotated with @Column");
            }
        }
        if(!isPrimaryKeyPresent){
            throw new PersistenceException("Not found primary key in table" + type.getName());
        }
        return databaseColumnsList;
    }

    public List<DatabaseTable> createDatabaseTables(Class<?>... types) {
        List<DatabaseTable> databaseTableList = new ArrayList<>();
        for (Class<?> type : types) {
            if(type.isAnnotationPresent(Table.class)){
                databaseTableList.add(createDatabaseTable(type));
            }else {
                throw new PersistenceException("Table " + type.getSimpleName() + " is not annotated with @Table");
            }
        }
        databaseTableList.sort(Comparator.comparing(DatabaseTable::getForeignKeyListSize));
        return databaseTableList;
    }

    public DatabaseTable createDatabaseTable(Class<?> type) {
        if(type.isAnnotationPresent(Table.class)){
            return new DatabaseTable(type.getAnnotation(Table.class), type.getSimpleName(), createDatabaseColumns(type), false);
        }
        throw new PersistenceException("Table " + type.getSimpleName() + " is not annotated with @Table");
    }

    public <T> int prepareStatementWithExceptionList(T entity, PreparedStatement preparedStatement, DatabaseTable databaseTable, List<String> exceptionList) {
        return prepareStatementWithExceptionList(1, entity, preparedStatement, databaseTable, exceptionList);
    }

    public <T> int prepareStatementWithExceptionList(int index, T entity, PreparedStatement preparedStatement, DatabaseTable databaseTable, List<String> exceptionList) {
        LinkedList<Entity> columnEntities = getColumnValues(entity,databaseTable);
        for (Entity columnEntity : columnEntities) {
            if (!exceptionList.contains(columnEntity.name())) {
                try {
                    if(!checkIfItIsFKObject(databaseTable, columnEntity)) {
                        preparedStatement.setObject(index++, columnEntity.value());
                    } else {
                        DatabaseTable foreignDatabaseTable = this.createDatabaseTable(columnEntity.value().getClass());
                        if (foreignDatabaseTable == null) {
                            throw new PersistenceException("Cannot get id from foreign database table");
                        }

                        Object foreignValue = columnEntity.value();

                        if (Proxy.isProxyClass(entity.getClass())) {
                            InvocationHandler handler = Proxy.getInvocationHandler(entity);

                            if (handler instanceof LazyProxyHandler<?>) {
                                LazyProxyHandler lazyHandler = (LazyProxyHandler) handler;
                                if (!lazyHandler.isInitialized()) {
                                    throw new PersistenceException("Lazy-loaded proxy is not initialized.");
                                }
                            }
                        }

                        long id = (long) this.getFieldValue(foreignValue, foreignDatabaseTable, "id");
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
            if (!excludedList.contains(databaseColumn.getSQLAlias())) {
                exceptionList.add(databaseColumn.getSQLAlias());
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

    /*public <T> T getInstanceFromDatabase(Class<T> type, ResultSet resultSet, DatabaseTable databaseTable) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            List<DatabaseColumn> databaseColumns = databaseTable.getDatabaseColumnList();

            for (DatabaseColumn databaseColumn : databaseColumns) {
                Field field = type.getDeclaredField(databaseColumn.name());
                field.setAccessible(true);
                Object value;

                if (field.getType() == int.class || field.getType() == Integer.class) {
                    value = resultSet.getInt(databaseColumn.getSQLAlias());
                } else if (field.getType() == long.class || field.getType() == Long.class) {
                    value = resultSet.getLong(databaseColumn.getSQLAlias());
                } else if (field.getType() == double.class || field.getType() == Double.class) {
                    value = resultSet.getDouble(databaseColumn.getSQLAlias());
                } else if (field.getType() == String.class) {
                    value = resultSet.getString(databaseColumn.getSQLAlias());
                } else {
                    value = resultSet.getObject(databaseColumn.getSQLAlias());
                    if (databaseTable.getForeignKeyList().contains(databaseColumn.getSQLAlias())) {
                        Optional<?> optionalValue = persistenceManager.get(field.getType(), resultSet.getLong(databaseColumn.getSQLAlias()));
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
    }*/

    public <T> T getInstanceFromDatabase(Class<T> type, ResultSet resultSet, DatabaseTable databaseTable) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            List<DatabaseColumn> databaseColumns = databaseTable.getDatabaseColumnList();

            for (DatabaseColumn databaseColumn : databaseColumns) {
                Field field = type.getDeclaredField(databaseColumn.name());
                field.setAccessible(true);
                Object value;

                if (field.getType() == int.class || field.getType() == Integer.class) {
                    value = resultSet.getInt(databaseColumn.getSQLAlias());
                } else if (field.getType() == long.class || field.getType() == Long.class) {
                    value = resultSet.getLong(databaseColumn.getSQLAlias());
                } else if (field.getType() == double.class || field.getType() == Double.class) {
                    value = resultSet.getDouble(databaseColumn.getSQLAlias());
                } else if (field.getType() == String.class) {
                    value = resultSet.getString(databaseColumn.getSQLAlias());
                } else {
                    value = resultSet.getObject(databaseColumn.getSQLAlias());

                    if (checkIfItIsFKObject(databaseTable, databaseColumn.getSQLAlias())) {
                        Column columnAnnotation = field.getAnnotation(Column.class);

                        if (columnAnnotation != null && columnAnnotation.lazyFetch() && field.getType().isInterface()) {
                            // Vytvorenie proxy na oneskorené načítanie
                            long foreignKeyId = resultSet.getLong(databaseColumn.getSQLAlias());
                            value = LazyProxyHandler.createProxy(
                                    field.getType(),
                                    columnAnnotation.targetClass(),
                                    () -> persistenceManager.get(columnAnnotation.targetClass(), foreignKeyId).orElse(null)
                            );
                        } else {
                            // Bežné načítanie cudzieho kľúča
                            Optional<?> optionalValue = persistenceManager.get(field.getType(), resultSet.getLong(databaseColumn.getSQLAlias()));
                            value = optionalValue.orElse(null);
                        }
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
                values.add(new Entity(column.getSQLAlias(), field.get(entity)));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new PersistenceException("No such field: " + column.name());
            }
        }
        return values;
    }

    private boolean checkIfItIsFKObject(DatabaseTable databaseTable, Entity columnEntity) {
        for(FKNameEntity fkNameEntity : databaseTable.getForeignKeyList()) {
            if(fkNameEntity.SQLAlias().contains(columnEntity.name())){
                return true;
            }
            if(fkNameEntity.javaName().contains(columnEntity.name())){
                return true;
            }
        }
        return false;
    }

    private boolean checkIfItIsFKObject(DatabaseTable databaseTable, String columnName) {
        for(FKNameEntity fkNameEntity : databaseTable.getForeignKeyList()) {
            if(fkNameEntity.SQLAlias().contains(columnName)){
                return true;
            }
            if(fkNameEntity.javaName().contains(columnName)){
                return true;
            }
        }
        return false;
    }
}

