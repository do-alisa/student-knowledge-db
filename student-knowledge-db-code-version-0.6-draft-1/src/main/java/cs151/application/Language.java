package cs151.application;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.Objects;

public class Language {
    private final StringProperty name = new SimpleStringProperty();

    public Language() {}
    public Language(String name) { this.name.set(name); }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Language)) return false;
        Language that = (Language) o;
        String a = this.getName() == null ? null : this.getName().toLowerCase();
        String b = that.getName() == null ? null : that.getName().toLowerCase();
        return Objects.equals(a, b);
    }

    @Override public int hashCode() {
        return Objects.hash(getName() == null ? null : getName().toLowerCase());
    }

    @Override public String toString() { return getName(); }
}
