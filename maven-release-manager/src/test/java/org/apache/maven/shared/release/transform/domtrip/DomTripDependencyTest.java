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

class DomTripDependencyTest {

    private DomTripDependency createDependency(String xml) {
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        return new DomTripDependency(document.root(), editor);
    }

    @Test
    void testGetArtifactIdMissing() {
        assertNull(createDependency("<dependency></dependency>").getArtifactId());
    }

    @Test
    void testGetArtifactIdPresent() {
        assertEquals(
                "ARTIFACTID",
                createDependency("<dependency><artifactId>ARTIFACTID</artifactId></dependency>")
                        .getArtifactId());
    }

    @Test
    void testGetGroupIdMissing() {
        assertNull(createDependency("<dependency></dependency>").getGroupId());
    }

    @Test
    void testGetGroupIdPresent() {
        assertEquals(
                "GROUPID",
                createDependency("<dependency><groupId>GROUPID</groupId></dependency>")
                        .getGroupId());
    }

    @Test
    void testGetVersionMissing() {
        assertNull(createDependency("<dependency></dependency>").getVersion());
    }

    @Test
    void testGetVersionPresent() {
        assertEquals(
                "VERSION",
                createDependency("<dependency><version>VERSION</version></dependency>")
                        .getVersion());
    }

    @Test
    void testSetVersion() {
        String xml = "<dependency><version>OLD_VERSION</version></dependency>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();
        new DomTripDependency(root, editor).setVersion("NEW_VERSION");
        assertEquals("NEW_VERSION", root.childTextTrimmed("version"));
    }

    @Test
    void testGetName() {
        assertEquals("dependency", createDependency("<dependency></dependency>").getName());
    }

    @Test
    void testIsOptional() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .isOptional());
    }

    @Test
    void testSetOptional() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .setOptional(true));
    }

    @Test
    void testAddExclusion() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .addExclusion(null));
    }

    @Test
    void testGetClassifier() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .getClassifier());
    }

    @Test
    void testGetExclusions() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .getExclusions());
    }

    @Test
    void testGetScope() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .getScope());
    }

    @Test
    void testGetSystemPath() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .getSystemPath());
    }

    @Test
    void testGetType() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .getType());
    }

    @Test
    void testRemoveExclusion() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .removeExclusion(null));
    }

    @Test
    void testSetArtifactId() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .setArtifactId(null));
    }

    @Test
    void testSetClassifier() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .setClassifier(null));
    }

    @Test
    void testSetExclusions() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .setExclusions(null));
    }

    @Test
    void testSetGroupId() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .setGroupId(null));
    }

    @Test
    void testSetScope() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .setScope(null));
    }

    @Test
    void testSetSystemPath() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .setSystemPath(null));
    }

    @Test
    void testSetType() {
        assertThrows(UnsupportedOperationException.class, () -> createDependency("<dependency/>")
                .setType(null));
    }
}
