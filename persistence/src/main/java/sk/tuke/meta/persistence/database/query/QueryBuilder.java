package sk.tuke.meta.persistence.database.query;

import sk.tuke.meta.persistence.PersistenceException;
import sk.tuke.meta.persistence.database.DatabaseColumn;
import sk.tuke.meta.persistence.database.DatabaseTable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
        List<String> foreignKeys = new ArrayList<>();
        boolean hasPrimaryKey = false;

        for (DatabaseColumn databaseColumn : databaseTable.getDatabaseColumnList()) {

            query.append(databaseColumn.getSQLAlias()).append("\" ");

            if (databaseColumn.type().equals(long.class)) {
                query.append("INTEGER");
            } else if (databaseColumn.type().equals(int.class) || databaseColumn.type().equals(Integer.class)) {
                query.append("INTEGER");
            } else if (databaseColumn.type().equals(float.class) || databaseColumn.type().equals(Float.class) ||
                    databaseColumn.type().equals(double.class) || databaseColumn.type().equals(Double.class)) {
                query.append("REAL");
            } else if (databaseColumn.type().equals(String.class)) {
                query.append("TEXT");
            } else {
                query.append("INTEGER");
                foreignKeys.add(databaseColumn.getSQLAlias());
            }
            if(databaseColumn.isPrimaryKey()) {
                if(hasPrimaryKey) {
                    throw new PersistenceException("More ten one primary key in table " + databaseTable.getSQLAlias());
                }
                hasPrimaryKey = true;
                query.append(" PRIMARY KEY AUTOINCREMENT");
            } else {
                if(databaseColumn.annotation().nullable()){
                    query.append(" NOT NULL");
                }
                if(databaseColumn.annotation().unique()){
                    query.append(" UNIQUE");

                }
            }
            query.append(",\n\"");
        }

        query.deleteCharAt(query.length() - 1);
        if (!hasPrimaryKey) {
            throw new PersistenceException("Primary keys doesn't exists");
        }

        for (String foreignKey : foreignKeys) {
            query.append("FOREIGN KEY (\"").append(foreignKey).append("\") REFERENCES \"").append(foreignKey).append("\" ( ID ) ON DELETE SET NULL");
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
        for (DatabaseColumn databaseColumn : databaseTable.getDatabaseColumnList()) {
            if(!databaseColumn.getSQLAlias().equals(primaryKeyName)) {
                query.append(databaseColumn.getSQLAlias()).append("\", \"");
            }
        }
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.append("\") VALUES (");
        query.append("?, ".repeat(Math.max(0, databaseTable.getDatabaseColumnList().size() - 1)));
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.append(") RETURNING ").append(primaryKeyName);
        return  query.toString();
    }

    public String getUpdateQuery (DatabaseTable databaseTable) {
        String primaryKeyName = databaseTable.getPrimaryKey();
        StringBuilder query = new StringBuilder("UPDATE \"" + databaseTable.getSQLAlias() + "\" SET \"");
        for (DatabaseColumn databaseColumn : databaseTable.getDatabaseColumnList()) {
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
