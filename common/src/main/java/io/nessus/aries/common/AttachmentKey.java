package io.nessus.aries.common;

import java.util.Objects;

public class AttachmentKey<T> {

    private final String name;
    private final Class<T> type;

    public AttachmentKey(Class<T> type) {
        this(type.getName(), type);
    }

    public AttachmentKey(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AttachmentKey<?> other = (AttachmentKey<?>) obj;
        return type.equals(other.type) && name.equals(name);
    }
    
    @Override
    public String toString() {
        String cname = type.getName();
        if (cname.equals(name))
            return String.format("[type=%s]", cname);
        else 
            return String.format("[name=%s,type=%s]", name, cname);
    }
}
