package sk.tuke.meta.persistence.database.query;

import sk.tuke.meta.persistence.PersistenceException;
import sk.tuke.meta.persistence.database.DatabaseColumn;
import sk.tuke.meta.persistence.database.DatabaseTable;
import sk.tuke.meta.persistence.entity.FKNameEntity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public class QueryBuilder {

    public String getCreateTableQuery() {
        try (InputStream inputStream = QueryBuilder.class.getClassLoader().getResourceAsStream("schema.sql")) {  // Fixed: Get class loader instance and load resource
            if (inputStream == null) {
                throw new IllegalStateException("SQL file doesnt found!");
            }
            Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A");
            if(scanner.hasNext()) {
                return scanner.next();
            }else {
                throw new PersistenceException("SQL file doesn't found!");
            }
        } catch (IOException e) {
            throw new PersistenceException("Error loading SQL schema!", e);
        }
    }

    public String getCreateTableQuery(DatabaseTable databaseTable) {
        StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS \"" + databaseTable.getSQLAlias() + "\" (\n\"");
        List<FKNameEntity> foreignKeys = databaseTable.getForeignKeyList();
        boolean hasPrimaryKey = false;

        for (DatabaseColumn databaseColumn : databaseTable.getDatabaseColumnsList()) {

            query.append(databaseColumn.getSQLAlias()).append("\" ");

            if (databaseColumn.getType().equals(long.class)) {
                query.append("INTEGER");
            } else if (databaseColumn.getType().equals(int.class) || databaseColumn.getType().equals(Integer.class)) {
                query.append("INTEGER");
            } else if (databaseColumn.getType().equals(float.class) || databaseColumn.getType().equals(Float.class) ||
                    databaseColumn.getType().equals(double.class) || databaseColumn.getType().equals(Double.class)) {
                query.append("REAL");
            } else if (databaseColumn.getType().equals(String.class)) {
                query.append("TEXT");
            } else {
                query.append("INTEGER");
            }
            if(databaseColumn.isPrimaryKey()) {
                if(hasPrimaryKey) {
                    throw new PersistenceException("getCreateTableQuery: More ten one primary key in table " + databaseTable.getSQLAlias());
                }
                hasPrimaryKey = true;
                query.append(" PRIMARY KEY AUTOINCREMENT");
            } else {
                if(databaseColumn.isNullable()){
                    query.append(" NOT NULL");
                }
                if(databaseColumn.isUnique()){
                    query.append(" UNIQUE");

                }
            }
            query.append(",\n\"");
        }

        query.deleteCharAt(query.length() - 1);
        if (!hasPrimaryKey) {
            throw new PersistenceException("Primary keys doesn't exists");
        }

        for (FKNameEntity foreignKey : foreignKeys) {
            query.append("FOREIGN KEY (\"").append(foreignKey.getSQLAlias()).append("\") REFERENCES \"").append(foreignKey.getTargetClass()).append("\" ( ID ) ON DELETE SET NULL");
            query.append(",\n");
        }
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.append("\n);");

        return query.toString();
    }

    public String getSelectOneQuery (DatabaseTable databaseTable) {
        return "SELECT * FROM \"" + databaseTable.getSQLAlias() +"\" WHERE " + databaseTable.getPrimaryKey() + " = ?;";
    }

    public String getSelectAllQuery (DatabaseTable databaseTable) {
        return "SELECT * FROM \"" + databaseTable.getSQLAlias() +"\";";
    }

    public String getInsertQuery (DatabaseTable databaseTable) {
        String primaryKeyName = databaseTable.getPrimaryKey();
        StringBuilder query = new StringBuilder("INSERT INTO \"" + databaseTable.getSQLAlias() + "\" (\"");
        for (DatabaseColumn databaseColumn : databaseTable.getDatabaseColumnsList()) {
            if(!databaseColumn.getSQLAlias().equals(primaryKeyName)) {
                query.append(databaseColumn.getSQLAlias()).append("\", \"");
            }
        }
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.append("\") VALUES (");
        query.append("?, ".repeat(Math.max(0, databaseTable.getDatabaseColumnsList().size() - 1)));
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.append(") RETURNING \"").append(primaryKeyName).append("\"");
        return  query.toString();
    }

    public String getUpdateQuery (DatabaseTable databaseTable) {
        String primaryKeyName = databaseTable.getPrimaryKey();
        StringBuilder query = new StringBuilder("UPDATE \"" + databaseTable.getSQLAlias() + "\" SET \"");
        for (DatabaseColumn databaseColumn : databaseTable.getDatabaseColumnsList()) {
            if(!databaseColumn.getSQLAlias().equals(primaryKeyName)) {
                query.append(databaseColumn.getSQLAlias()).append("\" = ?, \"");
            }
        }
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.append(" WHERE ").append(primaryKeyName).append(" = ?");
        return  query.toString();
    }

    public String getOneItemById(DatabaseTable databaseTable) {
        return "SELECT 1 FROM \"" + databaseTable.getSQLAlias() + "\" WHERE " + databaseTable.getPrimaryKey() + " = ?";
    }

    public String getDeleteQuery (DatabaseTable databaseTable) {
        return  "DELETE FROM \"" + databaseTable.getSQLAlias() + "\" WHERE " + databaseTable.getPrimaryKey() + " = ?";
    }
}
