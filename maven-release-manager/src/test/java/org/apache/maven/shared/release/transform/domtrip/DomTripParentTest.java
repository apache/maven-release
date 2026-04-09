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

class DomTripParentTest {

    private DomTripParent createParent(String xml) {
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        return new DomTripParent(document.root(), editor);
    }

    @Test
    void testGetVersion() {
        assertEquals(
                "1.0", createParent("<parent><version>1.0</version></parent>").getVersion());
    }

    @Test
    void testGetVersionMissing() {
        assertNull(createParent("<parent></parent>").getVersion());
    }

    @Test
    void testSetVersion() {
        String xml = "<parent></parent>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();
        assertNull(root.childText("version"));

        new DomTripParent(root, editor).setVersion("VERSION");
        assertEquals("VERSION", root.childTextTrimmed("version"));

        new DomTripParent(root, editor).setVersion(null);
        assertNull(root.childElement("version").orElse(null));
    }

    @Test
    void testSetVersionUpdate() {
        String xml = "<parent><version>OLD</version></parent>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();

        new DomTripParent(root, editor).setVersion("NEW");
        assertEquals("NEW", root.childTextTrimmed("version"));
    }

    @Test
    void testGetArtifactId() {
        assertThrows(UnsupportedOperationException.class, () -> createParent("<parent/>")
                .getArtifactId());
    }

    @Test
    void testGetGroupId() {
        assertThrows(UnsupportedOperationException.class, () -> createParent("<parent/>")
                .getGroupId());
    }

    @Test
    void testGetRelativePath() {
        assertThrows(UnsupportedOperationException.class, () -> createParent("<parent/>")
                .getRelativePath());
    }

    @Test
    void testSetArtifactId() {
        assertThrows(UnsupportedOperationException.class, () -> createParent("<parent/>")
                .setArtifactId(null));
    }

    @Test
    void testSetGroupId() {
        assertThrows(UnsupportedOperationException.class, () -> createParent("<parent/>")
                .setGroupId(null));
    }

    @Test
    void testSetRelativePath() {
        assertThrows(UnsupportedOperationException.class, () -> createParent("<parent/>")
                .setRelativePath(null));
    }

    @Test
    void testGetId() {
        assertThrows(UnsupportedOperationException.class, () -> createParent("<parent/>")
                .getId());
    }
}
