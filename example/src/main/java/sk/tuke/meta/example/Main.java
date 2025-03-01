package sk.tuke.meta.example;

import sk.tuke.meta.persistence.PersistenceException;
import sk.tuke.meta.persistence.PersistenceManager;
import sk.tuke.meta.persistence.ReflectivePersistenceManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;

public class Main {
    public static final String DB_PATH = "test.db";

    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);

        PersistenceManager manager = new ReflectivePersistenceManager(conn);

        manager.createTables(Person.class, Department.class);

        exampleOperations(manager);

        conn.close();
    }

    private static void exampleOperations(PersistenceManager manager) {
        System.out.println("Save Department");
        Department department = new Department("department", "DVLP");
        manager.save(department);

        System.out.println("Save Hrasko");
        Person hrasko = new Person("Janko", "Hrasko", 30);
        hrasko.setDepartment(department);
        manager.save(hrasko);

        System.out.println("List All Persons");
        List<Person> persons = manager.getAll(Person.class);
        for (Person person : persons) {
            System.out.println(person);
            System.out.println("  " + person.getDepartment());
        }
        Optional<Department> anotherDepartment = manager.get(Department.class, 100);
        System.out.println(anotherDepartment.isPresent());

        System.out.println("Update Hrasko");
        hrasko.setAge(40);
        manager.save(hrasko);
        persons = manager.getAll(Person.class);
        for (Person person : persons) {
            System.out.println(person);
            System.out.println("  " + person.getDepartment());
        }

        System.out.println("Delete Department");
        manager.delete(department);

        persons = manager.getAll(Person.class);
        for (Person person : persons) {
            System.out.println(person);
            System.out.println("  " + person.getDepartment());
        }

        System.out.println("Delete Hrasko");
        manager.delete(hrasko);

        persons = manager.getAll(Person.class);
        for (Person person : persons) {
            System.out.println(person);
            System.out.println("  " + person.getDepartment());
        }

        Select select = new Select(0,"Insert");
        try {
            manager.createTables(Select.class);
        } catch (PersistenceException e){
            System.out.println("createTables " + e.getMessage());
        }

        try {
            manager.save(select);
        } catch (PersistenceException e){
            System.out.println("save " + e.getMessage());
        }

        try {
            manager.get(Select.class, 1);
        } catch (PersistenceException e){
            System.out.println("get " + e.getMessage());
        }

        try {
            manager.getAll(Select.class);
        } catch (PersistenceException e){
            System.out.println("getAll " + e.getMessage());
        }

        try {
            manager.delete(select);
        } catch (PersistenceException e){
            System.out.println("delete " + e.getMessage());
        }
    }
}
