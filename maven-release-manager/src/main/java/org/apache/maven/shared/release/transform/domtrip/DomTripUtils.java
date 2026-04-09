/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.shared.release.transform.domtrip;

import java.util.Optional;

import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;

/**
 * Common DomTrip functions.
 *
 * @since 3.4
 */
public final class DomTripUtils {

    private DomTripUtils() {
        // noop
    }

    /**
     * Updates the text value of the given element. Preserves surrounding whitespace
     * and CDATA sections when present.
     *
     * @param editor  the editor for formatting-aware modifications
     * @param element the element to update, must not be {@code null}
     * @param value   the text string to set, must not be {@code null}
     */
    public static void rewriteValue(Editor editor, Element element, String value) {
        editor.setTextContent(element, value);
    }

    /**
     * Creates, updates or removes a child element.
     *
     * @param editor the editor for formatting-aware modifications
     * @param name   the child element name
     * @param value  the text value to set, or {@code null} to remove the element
     * @param root   the parent element
     * @return the created/updated element, or {@code null} if removed
     */
    public static Element rewriteElement(Editor editor, String name, String value, Element root) {
        Optional<Element> tagElement = root.childElement(name);
        if (tagElement.isPresent()) {
            if (value != null) {
                rewriteValue(editor, tagElement.get(), value);
                return tagElement.get();
            } else {
                editor.removeElement(tagElement.get());
                return null;
            }
        } else {
            if (value != null) {
                return editor.addElement(root, name, value);
            }
        }
        return null;
    }
}
