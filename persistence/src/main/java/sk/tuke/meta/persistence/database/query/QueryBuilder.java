package sk.tuke.meta.persistence.database.query;

import sk.tuke.meta.persistence.database.DatabaseColumn;
import sk.tuke.meta.persistence.database.DatabaseTable;

import java.util.ArrayList;
import java.util.List;

public class QueryBuilder {
    public String getCreateTableQuery(DatabaseTable databaseTable) {
        StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS " + databaseTable.getName() + " (\n");
        List<String> foreignKeys = new ArrayList<>();
        boolean hasPrimaryKey = false;

        for (DatabaseColumn databaseColumn : databaseTable.getDatabaseColumnList()) {

            query.append(databaseColumn.name()).append(" ");

            if (databaseColumn.type().equals(long.class)) {
                hasPrimaryKey = true;
                query.append("INTEGER PRIMARY KEY AUTOINCREMENT");
            } else if (databaseColumn.type().equals(int.class) || databaseColumn.type().equals(Integer.class)) {
                query.append("INTEGER");
            } else if (databaseColumn.type().equals(float.class) || databaseColumn.type().equals(Float.class) ||
                    databaseColumn.type().equals(double.class) || databaseColumn.type().equals(Double.class)) {
                query.append("REAL");
            } else if (databaseColumn.type().equals(String.class)) {
                query.append("TEXT");
            } else {
                query.append("INTEGER");
                foreignKeys.add(databaseColumn.name());
            }
            query.append(",\n");
        }

        if (!hasPrimaryKey) {
            query.append("ID INTEGER PRIMARY KEY AUTOINCREMENT,\n");
        }

        if (foreignKeys.size() == databaseTable.getDatabaseColumnList().size()) {
            System.out.println("FOREIGN KEY FOREIGN KEY SAME");
        }

        for (String foreignKey : foreignKeys) {
            query.append("FOREIGN KEY (").append(foreignKey).append(") REFERENCES ").append(foreignKey).append(" ( ID ) ON DELETE SET NULL");
            query.append(",\n");
        }
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.append("\n);");
        return query.toString();
    }

    public String getSelectOneQuery (DatabaseTable databaseTable) {
        return "SELECT * FROM " + databaseTable.getName() +" WHERE id = ?";
    }

    public String getSelectAllQuery (DatabaseTable databaseTable) {
        return "SELECT * FROM " + databaseTable.getName();
    }

    public String getInsertQuery (DatabaseTable databaseTable) {
        StringBuilder query = new StringBuilder("INSERT INTO " + databaseTable.getName() + " (");
        for (DatabaseColumn databaseColumn : databaseTable.getDatabaseColumnList()) {
            if(!databaseColumn.name().equals("id")) {
                query.append(databaseColumn.name()).append(", ");
            }
        }
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.append(") VALUES (");
        query.append("?, ".repeat(Math.max(0, databaseTable.getDatabaseColumnList().size() - 1)));
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.append(") RETURNING id");
        return  query.toString();
    }

    public String getUpdateQuery (DatabaseTable databaseTable) {
        StringBuilder query = new StringBuilder("UPDATE " + databaseTable.getName() + " SET ");
        for (DatabaseColumn databaseColumn : databaseTable.getDatabaseColumnList()) {
            if(!databaseColumn.name().equals("id")) {
                query.append(databaseColumn.name()).append(" = ?, ");
            }
        }
        query.deleteCharAt(query.length() - 1);
        query.deleteCharAt(query.length() - 1);
        query.append(" WHERE id = ?");
        return  query.toString();
    }

    public String getOneItemById(DatabaseTable databaseTable) {
        return "SELECT 1 FROM " + databaseTable.getName() + " WHERE id = ?";
    }

    public String getDeleteQuery (DatabaseTable databaseTable) {
        return  "DELETE FROM " + databaseTable.getName() + " WHERE id = ?";
    }
}
