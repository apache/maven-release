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

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomTripScmTest {

    private DomTripScm createScm(String xml) {
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        return new DomTripScm(document.root(), editor);
    }

    @Test
    void testGetConnection() {
        assertThrows(
                UnsupportedOperationException.class, () -> createScm("<scm/>").getConnection());
    }

    @Test
    void testGetDeveloperConnection() {
        assertThrows(
                UnsupportedOperationException.class, () -> createScm("<scm/>").getDeveloperConnection());
    }

    @Test
    void testGetTag() {
        assertThrows(
                UnsupportedOperationException.class, () -> createScm("<scm/>").getTag());
    }

    @Test
    void testGetUrl() {
        assertThrows(
                UnsupportedOperationException.class, () -> createScm("<scm/>").getUrl());
    }

    @Test
    void testSetConnection() {
        String xml = "<scm></scm>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();

        assertNull(root.childText("connection"));

        new DomTripScm(root, editor).setConnection("CONNECTION");
        assertEquals("CONNECTION", root.childTextTrimmed("connection"));

        new DomTripScm(root, editor).setConnection(null);
        assertNull(root.childElement("connection").orElse(null));
    }

    @Test
    void testSetDeveloperConnection() {
        String xml = "<scm></scm>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();

        assertNull(root.childText("developerConnection"));

        new DomTripScm(root, editor).setDeveloperConnection("DEVELOPERCONNECTION");
        assertEquals("DEVELOPERCONNECTION", root.childTextTrimmed("developerConnection"));

        new DomTripScm(root, editor).setDeveloperConnection(null);
        assertNull(root.childElement("developerConnection").orElse(null));
    }

    @Test
    void testSetTag() {
        String xml = "<scm></scm>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();

        assertNull(root.childText("tag"));

        new DomTripScm(root, editor).setTag("TAG");
        assertEquals("TAG", root.childTextTrimmed("tag"));

        new DomTripScm(root, editor).setTag(null);
        assertNull(root.childElement("tag").orElse(null));
    }

    @Test
    void testSetUrl() {
        String xml = "<scm></scm>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();

        assertNull(root.childText("url"));

        new DomTripScm(root, editor).setUrl("URL");
        assertEquals("URL", root.childTextTrimmed("url"));

        new DomTripScm(root, editor).setUrl(null);
        assertNull(root.childElement("url").orElse(null));
    }

    @Test
    void testSetConnectionUpdate() {
        String xml = "<scm><connection>OLD</connection></scm>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();

        new DomTripScm(root, editor).setConnection("NEW");
        assertEquals("NEW", root.childTextTrimmed("connection"));
    }
}
