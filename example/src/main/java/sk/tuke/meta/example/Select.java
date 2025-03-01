package sk.tuke.meta.example;

public class Select {
    private long id;
    private String surname;

    public Select(){
    }

    public Select(long id, String surname) {
        this.id = id;
        this.surname = surname;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }
}
