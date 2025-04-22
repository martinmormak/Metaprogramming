package sk.tuke.meta.example.arena;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;

import java.util.Objects;

@Table
public class MovedId {
    @Id
    @Column
    private long pk;
    @Column
    private String text;
    @Column
    private int number;

    public long getPk() { return pk; }
    public void setPk(long pk) { this.pk = pk; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovedId)) return false;
        MovedId movedId = (MovedId) o;
        return pk == movedId.pk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pk);
    }
}
