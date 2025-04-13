package sk.tuke.meta.persistence.processor;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;
import sk.tuke.meta.persistence.database.DatabaseColumn;
import sk.tuke.meta.persistence.database.DatabaseTable;
import sk.tuke.meta.persistence.entity.FKNameEntity;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("sk.tuke.meta.persistence.annotations.Table")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class TableAnnotationProcessor extends AbstractProcessor {
    private static final String TEMPLATE_PATH = "sk/tuke/meta/persistence/templates";

    private VelocityEngine velocity;

    private List<DatabaseColumn> databaseColumns = new ArrayList<>();
    private List<FKNameEntity> foreignKeyList = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        velocity = new VelocityEngine();
        velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocity.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        var tableElements = roundEnvironment.getElementsAnnotatedWith(Table.class);
        List<DatabaseTable> entities = analyzeEntities(tableElements);
        for (var entity : entities) {
            generateDAO(entity);
        }
        if (!tableElements.isEmpty()) {
            generatePersistenceManager(entities);
        }

        return true;
    }

    private List<DatabaseTable> analyzeEntities(Set<? extends Element> tableElements) {
        List<DatabaseTable> entities = new ArrayList<>();
        for (Element element : tableElements) {
            String name = element.getSimpleName().toString();
            String SQLAlias = element.getAnnotation(Table.class).name();
            String packageName = element.getEnclosingElement().toString();
            databaseColumns = new ArrayList<>();
            foreignKeyList = new ArrayList<>();
            getColumnList((TypeElement) element);
            entities.add(new DatabaseTable(name, SQLAlias, packageName, databaseColumns, foreignKeyList, false));
        }
        return entities;
    }

    private void generatePersistenceManager(List<DatabaseTable> entities) {
        try {
            var javaFile = processingEnv.getFiler().createSourceFile(
                    "sk.tuke.meta.persistence.GeneratedPersistenceManager");
            try (var writer = javaFile.openWriter()) {
                var template = velocity.getTemplate(
                        TEMPLATE_PATH + "/GeneratedPersistenceManager.java.vm");
                VelocityContext context = new VelocityContext();
                context.put("entities", entities);
                template.merge(context, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateDAO(DatabaseTable entity) {
        try {
            var javaFile = processingEnv.getFiler().createSourceFile(
                    entity.getFullDaoName());
            try (var writer = javaFile.openWriter()) {
                var template = velocity.getTemplate(TEMPLATE_PATH + "/DAO.java.vm");
                VelocityContext context = new VelocityContext();
                context.put("entity", entity);

                template.merge(context, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void getColumnList(TypeElement classElement) {
        for (VariableElement variableElement : ElementFilter.fieldsIn(classElement.getEnclosedElements())) {
            boolean isFK = false;
            Column column = variableElement.getAnnotation(Column.class);
            Id id = variableElement.getAnnotation(Id.class);
            String referencedTableName = "";

            if (column == null) continue;

            String columnName = (column != null && !column.name().isEmpty()) ? column.name() : variableElement.getSimpleName().toString();
            Class<?> columnType;

            switch (variableElement.asType().toString()) {
                case "long", "java.lang.Long":
                    columnType = long.class;
                    break;
                case "int", "java.lang.Integer":
                    columnType = Integer.class;
                    break;
                case "float", "java.lang.Float", "double", "java.lang.Double":
                    columnType = float.class;
                    break;
                case "java.lang.String":
                    columnType = String.class;
                    break;
                default:
                    columnType = Integer.class;
                    isFK = true;
                    TypeMirror targetTypeMirror = getTargetTypeMirror(column);
                    if (targetTypeMirror != null) {
                        Element targetElement = processingEnv.getTypeUtils().asElement(targetTypeMirror);
                        if (targetElement instanceof TypeElement targetClassElement) {
                            Table referencedTable = targetClassElement.getAnnotation(Table.class);
                            if (referencedTable == null) {
                                throw new ProcessorException("Referenced class " + targetClassElement.getSimpleName() + " is not annotated with @Table");
                            }
                            referencedTableName = referencedTable.name().isEmpty()
                                    ? targetClassElement.getSimpleName().toString()
                                    : referencedTable.name();
                        }
                    }
                    break;
            }

            DatabaseColumn databaseColumn = new DatabaseColumn(columnType, columnName, column, referencedTableName, id!=null);
            databaseColumns.add(databaseColumn);
            if(isFK){
                foreignKeyList.add(databaseColumn.getForeignKey());
            }
        }
    }



    private TypeMirror getTargetTypeMirror(Column column) {
        try {
            column.targetClass(); // This triggers MirroredTypeException
            return null; // Won't be reached
        } catch (MirroredTypeException e) {
            return e.getTypeMirror(); // Correct way to get the TypeMirror
        }
    }
}
