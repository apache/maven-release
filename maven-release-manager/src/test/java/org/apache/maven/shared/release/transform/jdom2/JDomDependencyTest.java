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
package org.apache.maven.shared.release.transform.jdom2;

import java.io.StringReader;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JDomDependencyTest {
    private SAXBuilder builder = new SAXBuilder();

    @Test
    void testIsOptional() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).isOptional());
    }

    @Test
    void testSetOptional() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).setOptional(true));
    }

    @Test
    void testAddExclusion() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).addExclusion(null));
    }

    @Test
    void testGetArtifactId() throws Exception {
        String content = "<dependency></dependency>";
        Element dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertNull(new JDomDependency(dependencyElm).getArtifactId());
        content = "<dependency><artifactId>ARTIFACTID</artifactId></dependency>";
        dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertEquals("ARTIFACTID", new JDomDependency(dependencyElm).getArtifactId());
    }

    @Test
    void testGetClassifier() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).getClassifier());
    }

    @Test
    void testGetExclusions() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).getExclusions());
    }

    @Test
    void testGetGroupId() throws Exception {
        String content = "<dependency></dependency>";
        Element dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertNull(new JDomDependency(dependencyElm).getGroupId());
        content = "<dependency><groupId>GROUPID</groupId></dependency>";
        dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertEquals("GROUPID", new JDomDependency(dependencyElm).getGroupId());
    }

    @Test
    void testGetScope() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).getScope());
    }

    @Test
    void testGetSystemPath() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).getSystemPath());
    }

    @Test
    void testGetType() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).getType());
    }

    @Test
    void testGetVersion() throws Exception {
        String content = "<dependency></dependency>";
        Element dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertNull(new JDomDependency(dependencyElm).getVersion());
        content = "<dependency><version>VERSION</version></dependency>";
        dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertEquals("VERSION", new JDomDependency(dependencyElm).getVersion());
    }

    @Test
    void testRemoveExclusion() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).removeExclusion(null));
    }

    @Test
    void testSetArtifactIdString() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).setArtifactId(null));
    }

    @Test
    void testSetClassifierString() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).setClassifier(null));
    }

    @Test
    void testSetExclusions() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).setExclusions(null));
    }

    @Test
    void testSetGroupIdString() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).setGroupId(null));
    }

    @Test
    void testSetScopeString() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).setScope(null));
    }

    @Test
    void testSetSystemPathString() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).setSystemPath(null));
    }

    @Test
    void testSetTypeString() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomDependency(null).setType(null));
    }

    @Test
    void testSetVersionString() throws Exception {
        String content = "<dependency><version>OLD_VERSION</version></dependency>";
        Element dependencyElm = builder.build(new StringReader(content)).getRootElement();
        new JDomDependency(dependencyElm).setVersion("NEW_VERSION");
        assertEquals("NEW_VERSION", getVersion(dependencyElm));
    }

    @Test
    void testGetName() {
        assertEquals("dependency", new JDomDependency(null).getName());
    }

    private String getVersion(Element dependencyElm) {
        return dependencyElm.getChildTextTrim("version", dependencyElm.getNamespace());
    }
}
