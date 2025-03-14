package sk.tuke.meta.persistence.database;

import sk.tuke.meta.persistence.annotations.Column;

public record DatabaseColumn(Class<?> type, Column annotation, String name, boolean isPrimaryKey) {
    public String getSQLAlias(){
        return annotation==null || annotation.name()==null || annotation.name().isEmpty()?name:annotation.name();
    }
}
