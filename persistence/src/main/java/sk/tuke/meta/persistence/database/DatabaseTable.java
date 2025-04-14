package sk.tuke.meta.persistence.database;

import sk.tuke.meta.persistence.PersistenceException;
import sk.tuke.meta.persistence.annotations.Table;
import sk.tuke.meta.persistence.database.query.QueryBuilder;
import sk.tuke.meta.persistence.entity.FKNameEntity;

import java.util.*;

public class DatabaseTable {
    private static final QueryBuilder queryBuilder = new QueryBuilder();
    private final String name;
    private final String SQLAlias;
    private final String packageName;
    private final List<DatabaseColumn> databaseColumnsList;
    private List<FKNameEntity> foreignKeyList;
    private boolean created;

    public DatabaseTable(String name, String SQLAlias, String packageName, List<DatabaseColumn> databaseColumnsList, List<FKNameEntity> foreignKeyList, boolean created) {
        this.name = name;
        this.SQLAlias = SQLAlias;
        this.packageName = packageName;
        this.databaseColumnsList = databaseColumnsList;
        this.created = created;
        this.foreignKeyList = foreignKeyList;
    }

    public DatabaseTable(String name, String SQLAlias, String packageName, List<DatabaseColumn> databaseColumnsList, boolean created) {
        this.name = name;
        this.SQLAlias = SQLAlias;
        this.packageName = packageName;
        this.databaseColumnsList = databaseColumnsList;
        this.created = created;
        getForeignKeyList(databaseColumnsList);
    }

    public DatabaseTable(String name, String packageName, Table tableAnnotation, List<DatabaseColumn> databaseColumnsList, List<FKNameEntity> foreignKeyList, boolean created) {
        this(name, tableAnnotation.name(), packageName, databaseColumnsList, foreignKeyList, created);
    }

    public DatabaseTable(String name, String packageName, Table tableAnnotation, List<DatabaseColumn> databaseColumnsList, boolean created) {
        this(name, tableAnnotation.name(), packageName, databaseColumnsList, created);
    }

    public String getSQLAlias() {
        return SQLAlias==null || SQLAlias.isEmpty()?name:SQLAlias;
    }

    public List<DatabaseColumn> getDatabaseColumnsList() {
        return databaseColumnsList;
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
                if(databaseColumn.isLazyFetch()) {
                    foreignKeyList.add(new FKNameEntity(databaseColumn.getName(), databaseColumn.getSQLAlias(), databaseColumn.getTargetClass()));
                } else {
                    foreignKeyList.add(new FKNameEntity(databaseColumn.getName(), databaseColumn.getSQLAlias(), databaseColumn.getClass().getSimpleName()));
                }
            }
        }
    }

    public String getPrimaryKey() {
        for(DatabaseColumn databaseColumn : databaseColumnsList){
            if(databaseColumn.isPrimaryKey()){
                return databaseColumn.getSQLAlias();
            }
        }
        throw new PersistenceException("Primary keys doesn't exists");
    }

    // for refactoring

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public String getDaoName() {
        return name + "DAO";
    }

    public String getTableName() {
        return name;
    }

    public DatabaseColumn getIdColumn() {
        return new DatabaseColumn(Integer.class ,"id", "id", true, false, true);
    }

    public String getFullName() {
        return packageName + "." + name;
    }

    public String getFullDaoName() {
        return packageName + "." + getDaoName();
    }

    public String getCreateQuery() {
        try {
            return SQLReplacer(queryBuilder.getCreateTableQuery(this));
        } catch (PersistenceException e) {
            System.out.println(e.getMessage());
            return "";
        }
    }

    public String getInsertQuery() {
        try {
            return SQLReplacer(queryBuilder.getInsertQuery(this));
        } catch (PersistenceException e) {
            System.out.println(e.getMessage());
            return "";
        }
    }

    public String getUpdateQuery() {
        try {
            return SQLReplacer(queryBuilder.getUpdateQuery(this));
        } catch (PersistenceException e) {
            System.out.println(e.getMessage());
            return "";
        }
    }

    public String getOneItemById() {
        try {
            return SQLReplacer(queryBuilder.getOneItemById(this));
        } catch (PersistenceException e) {
            System.out.println(e.getMessage());
            return "";
        }
    }

    public String getSelectOneQuery() {
        try {
            return SQLReplacer(queryBuilder.getSelectOneQuery(this));
        } catch (PersistenceException e) {
            System.out.println(e.getMessage());
            return "";
        }
    }

    public String getSelectAllQuery() {
        try {
            return SQLReplacer(queryBuilder.getSelectAllQuery(this));
        } catch (PersistenceException e) {
            System.out.println(e.getMessage());
            return "";
        }
    }

    public String getDeleteQuery() {
        try {
            return SQLReplacer(queryBuilder.getDeleteQuery(this));
        } catch (PersistenceException e) {
            System.out.println(e.getMessage());
            return "DELETE FROM \\\"" + getSQLAlias() + "\\\" WHERE ID = ?";
        }
    }

    private String SQLReplacer (String sql) {
        return sql.replace("\"","\\\"").replace("\n","\" + \n\"");
    }
}
