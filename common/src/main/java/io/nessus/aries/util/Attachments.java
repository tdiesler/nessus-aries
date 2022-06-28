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
