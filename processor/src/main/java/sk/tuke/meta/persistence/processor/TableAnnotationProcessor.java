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

            String columnName = variableElement.getSimpleName().toString();
            Class<?> columnType;

            //System.out.println("TableAnnotationProcessor - getColumnList: variableElement " + variableElement);
            //System.out.println("TableAnnotationProcessor - getColumnList: variableElement.asType() " + variableElement.asType());
            switch (variableElement.asType().toString()) {
                case "long", "java.lang.Long":
                    columnType = long.class;
                    break;
                case "int", "java.lang.Integer":
                    columnType = Integer.class;
                    break;
                case "float", "java.lang.Float":
                    columnType = float.class;
                    break;
                case "double", "java.lang.Double":
                    columnType = double.class;
                    break;
                case "string", "java.lang.String":
                    columnType = String.class;
                    break;
                default:
                    columnType = Object.class;
                    isFK = true;
                    TypeMirror targetTypeMirror = getTargetTypeMirror(column, variableElement);
                    if (targetTypeMirror != null) {
                        //System.out.println("TableAnnotationProcessor - getColumnList: targetTypeMirror " + targetTypeMirror);
                        Element targetElement = processingEnv.getTypeUtils().asElement(targetTypeMirror);
                        //System.out.println("TableAnnotationProcessor - getColumnList: targetElement " + targetElement);
                        if (targetElement instanceof TypeElement targetClassElement) {
                            //System.out.println("TableAnnotationProcessor - getColumnList: targetElement instanceof TypeElement true");
                            Table referencedTable = targetClassElement.getAnnotation(Table.class);
                            //System.out.println("TableAnnotationProcessor - getColumnList: referencedTable " + referencedTable);
                            if (referencedTable == null) {
                                throw new ProcessorException("Referenced class " + targetClassElement.getSimpleName() + " is not annotated with @Table");
                            }
                            referencedTableName = referencedTable.name().isEmpty()
                                    ? targetClassElement.getSimpleName().toString()
                                    : referencedTable.name();
                        }
                        //System.out.println("TableAnnotationProcessor - getColumnList: referencedTableName " + referencedTableName);
                        //System.out.println("TableAnnotationProcessor - getColumnList: targetTypeMirror " + targetTypeMirror);
                    }
                    break;
            }

            //System.out.println("TableAnnotationProcessor - getColumnList: columnType " + columnType);

            DatabaseColumn databaseColumn = new DatabaseColumn(columnType, columnName, column, referencedTableName, id!=null);
            databaseColumns.add(databaseColumn);
            if(isFK){
                foreignKeyList.add(databaseColumn.getForeignKey(processingEnv));
                //System.out.println("TableAnnotationProcessor - getColumnList: foreignKeyList.get(foreignKeyList.size() - 1).getPKFieldName() " + foreignKeyList.get(foreignKeyList.size() - 1).getPKFieldName());
                //System.out.println("TableAnnotationProcessor - getColumnList: foreignKeyList.get(foreignKeyList.size() - 1).getReferencedTable() " + foreignKeyList.get(foreignKeyList.size() - 1).getReferencedTable());
            }
        }
    }



    private TypeMirror getTargetTypeMirror(Column column, VariableElement field) {
        try {
            // Try to access targetClass — this will throw if not yet compiled
            Class<?> clazz = column.targetClass();
            if (clazz == void.class) {
                // targetClass was NOT set explicitly -> use field type
                return field.asType();
            }
        } catch (MirroredTypeException e) {
            if (e.getTypeMirror().toString().equals("void")) {
                // targetClass was void.class (default) -> use field type
                return field.asType();
            } else {
                // Explicit targetClass -> use the provided TypeMirror
                return e.getTypeMirror();
            }
        }
        // fallback
        return field.asType();
    }
}
