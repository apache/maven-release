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

class DomTripExtensionTest {

    private DomTripExtension createExtension(String xml) {
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        return new DomTripExtension(document.root(), editor);
    }

    @Test
    void testGetArtifactIdMissing() {
        assertNull(createExtension("<extension></extension>").getArtifactId());
    }

    @Test
    void testGetArtifactIdPresent() {
        assertEquals(
                "ARTIFACTID",
                createExtension("<extension><artifactId>ARTIFACTID</artifactId></extension>")
                        .getArtifactId());
    }

    @Test
    void testGetGroupIdMissing() {
        assertNull(createExtension("<extension></extension>").getGroupId());
    }

    @Test
    void testGetGroupIdPresent() {
        assertEquals(
                "GROUPID",
                createExtension("<extension><groupId>GROUPID</groupId></extension>")
                        .getGroupId());
    }

    @Test
    void testGetVersionMissing() {
        assertNull(createExtension("<extension></extension>").getVersion());
    }

    @Test
    void testGetVersionPresent() {
        assertEquals(
                "VERSION",
                createExtension("<extension><version>VERSION</version></extension>")
                        .getVersion());
    }

    @Test
    void testSetVersion() {
        String xml = "<extension><version>OLD_VERSION</version></extension>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();
        new DomTripExtension(root, editor).setVersion("NEW_VERSION");
        assertEquals("NEW_VERSION", root.childTextTrimmed("version"));
    }

    @Test
    void testGetName() {
        assertEquals("extension", createExtension("<extension></extension>").getName());
    }

    @Test
    void testSetArtifactId() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createExtension("<extension/>").setArtifactId(null));
    }

    @Test
    void testSetGroupId() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createExtension("<extension/>").setGroupId(null));
    }
}
