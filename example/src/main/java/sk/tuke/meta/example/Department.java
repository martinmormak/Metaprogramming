package sk.tuke.meta.example;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;

@Table(name = "Oddelenie")
public class Department implements IDepartment {
    @Id
    @Column
    private long id;
    @Column
    private String name;
    @Column
    private String code;
    @Column
    private double floor;

    public Department() {
    }

    public Department(String name, String code, double floor) {
        this.name = name;
        this.code = code;
        this.floor = floor;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getFloor() {
        return floor;
    }

    public void setFloor(double floor) {
        this.floor = floor;
    }

    public String toString() {
        return String.format("Department %d: %s (%s)", id, name, code);
    }
}
