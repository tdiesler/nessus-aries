package io.nessus.aries.common;

import java.util.Set;

public interface Attachments  {

    /**
     * Get the set of available keys.
     */
    Set<AttachmentKey<?>> getAttachmentKeys();
    
    /**
     * Attach an arbirtary object with this element.
     * @return The previously attachment object or null
     */
    <T> T putAttachment(AttachmentKey<T> key, T value);

    /**
     * Copy all attachments from sourse to target.
     */
    void putAllAttachments(Attachments source);

    /**
     * True if there is an attached object for a given key
     */
    <T> boolean hasAttachment(AttachmentKey<T> key);

    /**
     * Get the attached object for a given key
     * @return The attached object or null
     */
    <T> T getAttachment(AttachmentKey<T> key);

    /**
     * If not attached already, create the attachment withthe given default
     * @return The attached object
     */
    <T> T getAttachment(AttachmentKey<T> key, T defaultValue);

    /**
     * Remove an attached object for a given key
     * @return The attached object or null
     */
    <T> T removeAttachment(AttachmentKey<T> key);
}
