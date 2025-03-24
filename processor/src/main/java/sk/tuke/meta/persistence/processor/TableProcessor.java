package sk.tuke.meta.persistence.processor;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("sk.tuke.meta.persistence.annotations.Table")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class TableProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (annotations.isEmpty()) {
                return false;
            }
            StringBuilder SQLQueryBuilder = new StringBuilder();
            SQLQueryBuilder.append("PRAGMA foreign_keys = ON;\n\n");

            for (TypeElement annotation : annotations) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    if (element.getKind() != ElementKind.CLASS) continue;

                    TypeElement classElement = (TypeElement) element;
                    Table table = classElement.getAnnotation(Table.class);
                    String tableName = table.name().isEmpty() ? classElement.getSimpleName().toString() : table.name();

                    List<String> columns = new ArrayList<>();
                    List<String> foreignKeys = new ArrayList<>();
                    String primaryKey = null;

                    SQLQueryBuilder.append("CREATE TABLE IF NOT EXISTS \"").append(tableName).append("\" (\n");

                    for (VariableElement variableElement : ElementFilter.fieldsIn(classElement.getEnclosedElements())) {
                        Column column = variableElement.getAnnotation(Column.class);
                        Id id = variableElement.getAnnotation(Id.class);

                        if (column == null && id == null) continue;

                        String columnName = (column != null && !column.name().isEmpty()) ? column.name() : variableElement.getSimpleName().toString();
                        String columnType;

                        switch (variableElement.asType().toString()) {
                            case "long", "java.lang.Long":
                                columnType = "INTEGER";
                                break;
                            case "int", "java.lang.Integer":
                                columnType = "INTEGER";
                                break;
                            case "float", "java.lang.Float", "double", "java.lang.Double":
                                columnType = "REAL";
                                break;
                            case "java.lang.String":
                                columnType = "TEXT";
                                break;
                            default:
                                columnType = "INTEGER";
                                TypeMirror targetTypeMirror = getTargetTypeMirror(column);
                                if (targetTypeMirror != null) {
                                    Element targetElement = processingEnv.getTypeUtils().asElement(targetTypeMirror);
                                    if (targetElement instanceof TypeElement targetClassElement) {
                                        Table referencedTable = targetClassElement.getAnnotation(Table.class);
                                        if (referencedTable == null) {
                                            throw new ProcessorException("Referenced class " + targetClassElement.getSimpleName() + " is not annotated with @Table");
                                        }
                                        String referencedTableName = referencedTable.name().isEmpty()
                                                ? targetClassElement.getSimpleName().toString()
                                                : referencedTable.name();

                                        foreignKeys.add("FOREIGN KEY (\"" + columnName + "\") REFERENCES \"" + referencedTableName + "\" (ID) ON DELETE SET NULL");
                                    }
                                }
                                break;
                        }

                        if (id != null) {
                            primaryKey = columnName;
                            columnType += " PRIMARY KEY AUTOINCREMENT";
                        } else {
                            if (!column.nullable()) columnType += " NOT NULL";
                            if (column.unique()) columnType += " UNIQUE";
                        }

                        columns.add("\"" + columnName + "\" " + columnType);
                    }

                    if (primaryKey == null) {
                        throw new ProcessorException("Table " + tableName + " dont have primary key.");
                    }

                    SQLQueryBuilder.append(String.join(",\n", columns));
                    if (!foreignKeys.isEmpty()) {
                        SQLQueryBuilder.append(",\n");
                        SQLQueryBuilder.append(String.join(",\n", foreignKeys));
                    }
                    SQLQueryBuilder.append("\n);\n\n");
                }
            }

        System.out.println("SQL Query: " + SQLQueryBuilder);
        writeSqlToFile(SQLQueryBuilder.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private TypeMirror getTargetTypeMirror(Column column) {
        try {
            column.targetClass(); // This triggers MirroredTypeException
            return null; // Won't be reached
        } catch (MirroredTypeException e) {
            return e.getTypeMirror(); // Correct way to get the TypeMirror
        }
    }

    private void writeSqlToFile(String sql) {
        try {
            Filer filer = processingEnv.getFiler();
            Writer writer = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "schema.sql").openWriter();
            writer.write(sql);
            writer.close();
        } catch (IOException e) {
            throw new ProcessorException("Error while generating SQL file", e);
        }
    }
}
