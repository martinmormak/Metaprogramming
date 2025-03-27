package sk.tuke.meta.persistence;

import sk.tuke.meta.persistence.database.DatabaseTable;
import sk.tuke.meta.persistence.database.query.QueryBuilder;
import sk.tuke.meta.persistence.entity.FKNameEntity;
import sk.tuke.meta.persistence.reflection.TableReflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

public class ReflectivePersistenceManager implements PersistenceManager {
    private final QueryBuilder queryBuilder = new QueryBuilder();
    private final TableReflection tableReflection = new TableReflection(this);
    private final Connection connection;

    public ReflectivePersistenceManager(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTables(Class<?>... types) {
        /*List<DatabaseTable> databaseTableList = tableReflection.createDatabaseTables(types);

        boolean allTablesCreated;
        int loop = 0;
        do {
            allTablesCreated = true;
            for (DatabaseTable databaseTable : databaseTableList) {
                if (!databaseTable.isCreated()) {
                    if (!createTable(databaseTable)) {
                        allTablesCreated = false;
                    }
                }
            }
            loop++;
        } while (!allTablesCreated && loop < types.length);*/
        String[] statements = queryBuilder.getCreateTableQuery().split(";");

        for (String statement : statements) {
            statement = statement.trim();
            if (!statement.isEmpty()) {
                createTable(statement + ";"); // Ensure semicolon for each statement
            }
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> type, long id) {
        DatabaseTable databaseTable = getDatabaseTable(type);
        if(databaseTable == null) {
            return Optional.empty();
        }

        String query = queryBuilder.getSelectOneQuery(databaseTable);

        if (query == null) {
            throw new PersistenceException("No such database table, query is null in get");
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(tableReflection.getInstanceFromDatabase(type, resultSet, databaseTable));
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error executing SQL", e);
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> getAll(Class<T> type) {
        DatabaseTable databaseTable = getDatabaseTable(type);
        if(databaseTable == null) {
            return List.of();
        }

        String query = queryBuilder.getSelectAllQuery(databaseTable);

        if (query == null) {
            throw new PersistenceException("No such database table, query is null in get");
        }

        List<T> instances = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                instances.add(tableReflection.getInstanceFromDatabase(type, resultSet, databaseTable));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error executing SQL", e);
        }
        return instances;
    }

    @Override
    public <T> void save(T entity) {
        try {
            System.out.println("My debug output save:" + getEntityDetails(entity));
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
        DatabaseTable databaseTable = null;
        Object realObject = null;
        if (Proxy.isProxyClass(entity.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(entity);

            if (handler instanceof LazyProxyHandler<?>) {
                LazyProxyHandler lazyHandler = (LazyProxyHandler) handler;
                if (!lazyHandler.isInitialized()) {
                    return;
                }
                databaseTable = getDatabaseTable(lazyHandler.getTargetClass());
                realObject = lazyHandler.getRealObject();
            }
        }
        if(databaseTable == null) {
            databaseTable = getDatabaseTable(entity.getClass());
            realObject = entity;
        }

        if(databaseTable == null) {
            return;
        }

        Object PK = tableReflection.getFieldValue(realObject, databaseTable, databaseTable.getPrimaryKey());

        try {
            if (!checkForeignKeysExists(realObject, databaseTable)) {
                throw new PersistenceException("Foreign keys doesn't exists");
            }
            if (PKExist(databaseTable, PK)) {
                String updateQuery = queryBuilder.getUpdateQuery(databaseTable);
                PreparedStatement preparedStatement = connection.prepareStatement(updateQuery);
                tableReflection.prepareStatementWithExcludedList(tableReflection.prepareStatementWithExceptionList(realObject, preparedStatement, databaseTable, List.of(databaseTable.getPrimaryKey())), realObject, preparedStatement, databaseTable, List.of(databaseTable.getPrimaryKey()));
                preparedStatement.executeUpdate();
            } else {
                String insertQuery = queryBuilder.getInsertQuery(databaseTable);
                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                    tableReflection.prepareStatementWithExceptionList(realObject, preparedStatement, databaseTable, List.of(databaseTable.getPrimaryKey()));
                    preparedStatement.execute();
                    ResultSet resultSet = preparedStatement.getGeneratedKeys();
                    if (resultSet.next()) {
                        tableReflection.setField(realObject, resultSet.getObject(1), databaseTable.getPrimaryKey());
                    }
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("ID field not found for entity " + entity, e);
        }
    }

    @Override
    public void delete(Object entity) {
        try {
            System.out.println("My debug output delete:" + getEntityDetails(entity));
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
        DatabaseTable databaseTable = null;
        Object PK = null;
        if (Proxy.isProxyClass(entity.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(entity);

            if (handler instanceof LazyProxyHandler lazyHandler) {
                PK = lazyHandler.getID();
                databaseTable = tableReflection.createDatabaseTable(lazyHandler.getTargetClass());
            }
        }
        if(databaseTable == null) {
            databaseTable = tableReflection.createDatabaseTable(entity.getClass());
            PK = tableReflection.getFieldValue(entity, databaseTable, databaseTable.getPrimaryKey());
        }

        if (!PKExist(databaseTable, PK)) {
            throw new PersistenceException("Object not found in database");
        }

        String deleteQuery = queryBuilder.getDeleteQuery(databaseTable);

        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
            preparedStatement.setObject(1, PK);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new PersistenceException("Error deleting entity", e);
        }
    }

    private boolean createTable(DatabaseTable databaseTable) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(queryBuilder.getCreateTableQuery(databaseTable));
            databaseTable.setCreated(true);
            return true;
        } catch (SQLException e) {
            databaseTable.setCreated(false);
            return false;
        }
    }

    private void createTable(String SQLQuery) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(SQLQuery);
        } catch (SQLException e) {
            throw new PersistenceException("Error executing SQL", e);
        }
    }

    private DatabaseTable getDatabaseTable(Class<?> objectClass) {
        try {
            return tableReflection.createDatabaseTable(objectClass);
        } catch (PersistenceException e) {
            return null;
        }
        /*StringBuilder tables = new StringBuilder();
        for(DatabaseTable databaseTable : databaseTableList){
            tables.append("name").append(databaseTable.getName()).append(" databaseColumnList").append(databaseTable.getDatabaseColumnList()).append(" foreignKeyList").append(databaseTable.getForeignKeyList()).append("\n");
        }
        return databaseTableList.stream()
                .filter(table -> table.getName().equalsIgnoreCase(tableName))
                .findFirst()
                .orElseThrow(() -> new PersistenceException("No such database table: " + tableName + " in databaseTableList\n" + tables));*/
    }

    private <T> boolean checkForeignKeysExists(T entity, DatabaseTable databaseTable) {
        for (FKNameEntity fieldName : databaseTable.getForeignKeyList()) {
            try {
                Field field = entity.getClass().getDeclaredField(fieldName.javaName());
                field.setAccessible(true);
                Object value = field.get(entity);

                DatabaseTable foreignDatabaseTable = getDatabaseTable(value.getClass());
                if (foreignDatabaseTable == null) {
                    return false;
                }

                Object PK = tableReflection.getFieldValue(value, foreignDatabaseTable, foreignDatabaseTable.getPrimaryKey());
                if (PK != null) {
                    if (!PKExist(getDatabaseTable(value.getClass()), PK)) {
                        return false;
                    }
                } else {
                    return false;
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new PersistenceException("Error checking foreign keys", e);
            }
        }
        return true;
    }

    private boolean PKExist(DatabaseTable databaseTable, Object PK) {
        String query = queryBuilder.getOneItemById(databaseTable);
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setObject(1, PK);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static String getEntityDetails(Object entity) {
        System.out.println("My debug output getEntityDetails:" + entity);
        StringBuilder sb = new StringBuilder(entity.getClass().getSimpleName() + " { ");
        for (Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                sb.append(field.getName()).append("-").append(field.getType()).append("=").append(field.get(entity)).append(", ");
            } catch (IllegalAccessException e) {
                sb.append(field.getName()).append("=ACCESS DENIED, ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

}