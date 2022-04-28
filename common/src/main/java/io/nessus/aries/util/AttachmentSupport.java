package io.nessus.aries.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.nessus.common.AssertArg;

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
