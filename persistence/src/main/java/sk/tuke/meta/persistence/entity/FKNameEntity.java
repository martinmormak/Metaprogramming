package sk.tuke.meta.persistence.entity;

public record FKNameEntity (String javaName, String SQLAlias, Class<?> targetClass) {
}
