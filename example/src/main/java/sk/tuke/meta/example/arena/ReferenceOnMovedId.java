package sk.tuke.meta.example.arena;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;

import java.util.Objects;

@Table
public class ReferenceOnMovedId {
    @Id
    @Column
    private long id;
    @Column
    private String text;
    @Column
    private MovedId moved;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public MovedId getMoved() { return moved; }
    public void setMoved(MovedId moved) { this.moved = moved; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReferenceOnMovedId)) return false;
        ReferenceOnMovedId that = (ReferenceOnMovedId) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
