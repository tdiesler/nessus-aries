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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AttachmentSupport implements Attachments {

    private Map<AttachmentKey<? extends Object>, Object> attachments;

    @Override
    public synchronized Set<AttachmentKey<?>> getAttachmentKeys() {
        if (attachments == null) return Collections.emptySet();
        return attachments.keySet();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> T putAttachment(AttachmentKey<T> key, T value) {
        AssertArg.notNull(key, "Null key");
        if (attachments == null) {
            attachments = new HashMap<>();
        }
        return (T) (value != null ? attachments.put(key, value) : attachments.remove(key));
    }

    @Override
    public void putAllAttachments(Attachments source) {
        source.getAttachmentKeys().forEach(key -> {
            @SuppressWarnings("unchecked")
            AttachmentKey<Object> typedKey = (AttachmentKey<Object>) key;
            putAttachment(typedKey, source.getAttachment(key));
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> T getAttachment(AttachmentKey<T> key) {
        AssertArg.notNull(key, "Null key");
        return attachments != null ? (T) attachments.get(key) : null;
    }

    @Override
    public <T> T getAttachment(AttachmentKey<T> key, T defaultValue) {
        T value = getAttachment(key);
        if (value == null) {
            value = defaultValue;
            putAttachment(key, value);
        }
        return value;
    }

    @Override
    public synchronized <T> boolean hasAttachment(AttachmentKey<T> key) {
        AssertArg.notNull(key, "Null key");
        return attachments != null ? attachments.containsKey(key) : false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> T removeAttachment(AttachmentKey<T> key) {
        AssertArg.notNull(key, "Null key");
        return attachments != null ? (T) attachments.remove(key) : null;
    }
}
