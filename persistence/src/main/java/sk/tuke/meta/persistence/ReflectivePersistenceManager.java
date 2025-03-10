package sk.tuke.meta.persistence;

import sk.tuke.meta.persistence.database.DatabaseTable;
import sk.tuke.meta.persistence.database.query.QueryBuilder;
import sk.tuke.meta.persistence.reflection.TableReflection;

import java.lang.reflect.Field;
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
        List<DatabaseTable> databaseTableList = tableReflection.createDatabaseTables(types);

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
        } while (!allTablesCreated && loop < types.length);
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
        DatabaseTable databaseTable = getDatabaseTable(entity.getClass());
        if(databaseTable == null) {
            return;
        }

        long id = (long) tableReflection.getFieldValue(entity, databaseTable, "id");

        try {
            if (!checkForeignKeysExists(entity, databaseTable)) {
                throw new PersistenceException("Foreign keys doesn't exists");
            }
            if (idExist(databaseTable, id)) {
                String updateQuery = queryBuilder.getUpdateQuery(databaseTable);
                PreparedStatement preparedStatement = connection.prepareStatement(updateQuery);
                tableReflection.prepareStatementWithExcludedList(tableReflection.prepareStatementWithExceptionList(entity, preparedStatement, databaseTable, List.of("id")), entity, preparedStatement, databaseTable, List.of("id"));
                preparedStatement.executeUpdate();
            } else {
                String insertQuery = queryBuilder.getInsertQuery(databaseTable);
                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                    tableReflection.prepareStatementWithExceptionList(entity, preparedStatement, databaseTable, List.of("id"));
                    preparedStatement.execute();
                    ResultSet resultSet = preparedStatement.getGeneratedKeys();
                    if (resultSet.next()) {
                        tableReflection.setField(entity, resultSet.getLong(1), "id");
                    }
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("ID field not found", e);
        }
    }

    @Override
    public void delete(Object entity) {
        DatabaseTable databaseTable = getDatabaseTable(entity.getClass());
        if(databaseTable == null) {
            return;
        }

        long id = (long) tableReflection.getFieldValue(entity, databaseTable, "id");

        if (!idExist(databaseTable, id)) {
            throw new PersistenceException("Object not found in database");
        }

        String deleteQuery = queryBuilder.getDeleteQuery(databaseTable);

        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
            tableReflection.prepareStatementWithExcludedList(entity, preparedStatement, databaseTable, List.of("id"));
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
        for (String fieldName : databaseTable.getForeignKeyList()) {
            try {
                Field field = entity.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(entity);

                DatabaseTable foreignDatabaseTable = getDatabaseTable(value.getClass());
                if (foreignDatabaseTable == null) {
                    return false;
                }

                long id = (long) tableReflection.getFieldValue(value, foreignDatabaseTable, "id");
                if (id != 0) {
                    if (!idExist(getDatabaseTable(value.getClass()), id)) {
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

    private boolean idExist(DatabaseTable databaseTable, long id) {
        String query = queryBuilder.getOneItemById(databaseTable);
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

}