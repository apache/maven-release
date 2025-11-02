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

public class JDomDependencyTest {
    private SAXBuilder builder = new SAXBuilder();

    @Test
    public void testIsOptional() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).isOptional());
    }

    @Test
    public void testSetOptional() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).setOptional(true));
    }

    @Test
    public void testAddExclusion() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).addExclusion(null));
    }

    @Test
    public void testGetArtifactId() throws Exception {
        String content = "<dependency></dependency>";
        Element dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertNull(new JDomDependency(dependencyElm).getArtifactId());

        content = "<dependency><artifactId>ARTIFACTID</artifactId></dependency>";
        dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertEquals("ARTIFACTID", new JDomDependency(dependencyElm).getArtifactId());
    }

    @Test
    public void testGetClassifier() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).getClassifier());
    }

    @Test
    public void testGetExclusions() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).getExclusions());
    }

    @Test
    public void testGetGroupId() throws Exception {
        String content = "<dependency></dependency>";
        Element dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertNull(new JDomDependency(dependencyElm).getGroupId());

        content = "<dependency><groupId>GROUPID</groupId></dependency>";
        dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertEquals("GROUPID", new JDomDependency(dependencyElm).getGroupId());
    }

    @Test
    public void testGetScope() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).getScope());
    }

    @Test
    public void testGetSystemPath() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).getSystemPath());
    }

    @Test
    public void testGetType() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).getType());
    }

    @Test
    public void testGetVersion() throws Exception {
        String content = "<dependency></dependency>";
        Element dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertNull(new JDomDependency(dependencyElm).getVersion());

        content = "<dependency><version>VERSION</version></dependency>";
        dependencyElm = builder.build(new StringReader(content)).getRootElement();
        assertEquals("VERSION", new JDomDependency(dependencyElm).getVersion());
    }

    @Test
    public void testRemoveExclusion() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).removeExclusion(null));
    }

    @Test
    public void testSetArtifactIdString() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).setArtifactId(null));
    }

    @Test
    public void testSetClassifierString() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).setClassifier(null));
    }

    @Test
    public void testSetExclusions() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).setExclusions(null));
    }

    @Test
    public void testSetGroupIdString() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).setGroupId(null));
    }

    @Test
    public void testSetScopeString() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).setScope(null));
    }

    @Test
    public void testSetSystemPathString() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).setSystemPath(null));
    }

    @Test
    public void testSetTypeString() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependency(null).setType(null));
    }

    @Test
    public void testSetVersionString() throws Exception {
        String content = "<dependency><version>OLD_VERSION</version></dependency>";
        Element dependencyElm = builder.build(new StringReader(content)).getRootElement();
        new JDomDependency(dependencyElm).setVersion("NEW_VERSION");
        assertEquals("NEW_VERSION", getVersion(dependencyElm));
    }

    @Test
    public void testGetName() {
        assertEquals("dependency", new JDomDependency(null).getName());
    }

    private String getVersion(Element dependencyElm) {
        return dependencyElm.getChildTextTrim("version", dependencyElm.getNamespace());
    }
}
