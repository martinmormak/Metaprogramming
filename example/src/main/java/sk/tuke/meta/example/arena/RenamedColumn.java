package sk.tuke.meta.example.arena;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;

import java.util.Objects;

@Table
public class RenamedColumn {
    @Id
    @Column
    private long id;
    @Column(name = "new_column")
    private String text;
    @Column
    private int number;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RenamedColumn)) return false;
        RenamedColumn that = (RenamedColumn) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
