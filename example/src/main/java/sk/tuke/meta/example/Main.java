package sk.tuke.meta.example;

import sk.tuke.meta.persistence.GeneratedPersistenceManager;
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

        PersistenceManager manager = new GeneratedPersistenceManager(conn);

        manager.createTables(Person.class, Department.class);

        exampleOperations(manager);

        conn.close();
    }

    private static void exampleOperations(PersistenceManager manager) {
        System.out.println("Save Department");
        Department department = new Department("department", "DVLP",5);
        manager.save(department);

        System.out.println("Save Hrasko");
        Person hrasko = new Person("Janko", "Hrasko", 30);
        hrasko.setIDepartment(department);
        manager.save(hrasko);

        Optional<Person> onePerson = manager.get(Person.class, hrasko.getId());

        System.out.println("List One Persons");
        if(onePerson.isPresent()) {
            System.out.println(onePerson.get());
            IDepartment onePersonIDepartment = onePerson.get().getIDepartment();
            System.out.println("- " + onePersonIDepartment.toString());
        }

        System.out.println("List All Persons");
        List<Person> persons = manager.getAll(Person.class);
        for (Person person : persons) {
            System.out.println(person);
            IDepartment personIDepartment = person.getIDepartment();
            System.out.println("- " + personIDepartment.toString());
        }
        Optional<Department> anotherDepartment = manager.get(Department.class, 100);
        System.out.println(anotherDepartment.isPresent());

        System.out.println("Update Hrasko");
        hrasko.setAge(40);
        manager.save(hrasko);
        persons = manager.getAll(Person.class);
        for (Person person : persons) {
            System.out.println(person);
            IDepartment personIDepartment = person.getIDepartment();
            System.out.println("- " + personIDepartment.toString());
        }

        System.out.println("Delete Department");
        manager.delete(department);

        persons = manager.getAll(Person.class);
        for (Person person : persons) {
            System.out.println(person);
            IDepartment personIDepartment = person.getIDepartment();
            if (personIDepartment == null) {
                System.out.println("- " + personIDepartment.toString());
            }
        }

        System.out.println("Delete Hrasko");
        manager.delete(hrasko);

        persons = manager.getAll(Person.class);
        for (Person person : persons) {
            System.out.println(person);
            IDepartment personIDepartment = person.getIDepartment();
            if (personIDepartment == null) {
                System.out.println("- " + personIDepartment.toString());
            }
        }
    }
}
