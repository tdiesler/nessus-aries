/*-
 * #%L
 * Nessus Aries :: Common
 * %%
 * Copyright (C) 2022 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.nessus.aries.util;

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
