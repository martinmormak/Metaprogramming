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
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.*;
import java.util.stream.Collectors;

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
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Table.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement classElement = (TypeElement) element;

                // Print class name (fully qualified)
                String className = processingEnv.getElementUtils().getBinaryName(classElement).toString();
                String SQLAlias = element.getAnnotation(Table.class).name();
                System.out.println("Class: " + className + ", SQLAlias: " + SQLAlias);

                // Print all fields
                for (Element enclosed : classElement.getEnclosedElements()) {
                    if (enclosed.getKind() == ElementKind.FIELD) {
                        VariableElement field = (VariableElement) enclosed;
                        String fieldName = field.getSimpleName().toString();
                        String fieldType = field.asType().toString();
                        try {
                            SQLAlias = enclosed.getAnnotation(Column.class).name();
                        } catch (NullPointerException e) {
                            SQLAlias = "";
                        }

                        System.out.println("  Field: " + fieldType + " " + fieldName + ", " + SQLAlias);
                    }
                }

                // Methods
                for (Element enclosed : classElement.getEnclosedElements()) {
                    if (enclosed.getKind() == ElementKind.METHOD) {
                        ExecutableElement method = (ExecutableElement) enclosed;
                        String methodName = method.getSimpleName().toString();
                        String returnType = method.getReturnType().toString();

                        List<? extends VariableElement> parameters = method.getParameters();
                        String paramList = parameters.stream()
                                .map(p -> p.asType().toString() + " " + p.getSimpleName())
                                .collect(Collectors.joining(", "));

                        System.out.println("  Method: " + returnType + " " + methodName + "(" + paramList + ")");
                    }
                }
            }
        }
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
            System.out.println("TableAnnotationProcessor - getColumnList: variableElement " + variableElement);
            TypeMirror typeMirror = variableElement.asType();
            String columnClass = typeMirror.toString().substring(typeMirror.toString().lastIndexOf('.') + 1);;
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
                    columnClass = "java.lang.Long";
                    break;
                case "int", "java.lang.Integer":
                    columnClass = "java.lang.Integer";
                    break;
                case "float", "java.lang.Float":
                    columnClass = "java.lang.Float";
                    break;
                case "double", "java.lang.Double":
                    columnClass = "java.lang.Double";
                    break;
                case "string", "java.lang.String":
                    columnClass = "java.lang.String";
                    break;
                default:
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

            DatabaseColumn databaseColumn = new DatabaseColumn(columnClass, columnName, column, referencedTableName, id!=null);
            System.out.println("TableAnnotationProcessor - getColumnList: databaseColumn " + databaseColumn);
            databaseColumns.add(databaseColumn);
            if(isFK){
                foreignKeyList.add(databaseColumn.getForeignKey(processingEnv));
                System.out.println("TableAnnotationProcessor - getColumnList: foreignKeyList.get(foreignKeyList.size() - 1) " + foreignKeyList.get(foreignKeyList.size() - 1));
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
