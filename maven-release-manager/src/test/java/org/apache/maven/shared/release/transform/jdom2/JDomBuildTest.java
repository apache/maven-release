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

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JDomBuildTest {
    private SAXBuilder builder = new SAXBuilder();

    @Test
    void testGetExtensions() throws Exception {
        String content = "<build></build>";
        Document document = builder.build(new StringReader(content));
        assertNotNull(new JDomBuild(document.getRootElement()).getExtensions());
        assertEquals(0, new JDomBuild(document.getRootElement()).getExtensions().size());

        content = "<build><extensions/></build>";
        document = builder.build(new StringReader(content));
        assertEquals(0, new JDomBuild(document.getRootElement()).getExtensions().size());

        content = "<build><extensions><extension/></extensions></build>";
        document = builder.build(new StringReader(content));
        assertEquals(1, new JDomBuild(document.getRootElement()).getExtensions().size());
    }

    @Test
    void testGetPluginManagement() throws Exception {
        String content = "<build></build>";
        Document document = builder.build(new StringReader(content));
        assertNull(new JDomBuild(document.getRootElement()).getPluginManagement());

        content = "<build><pluginManagement/></build>";
        document = builder.build(new StringReader(content));
        assertNotNull(new JDomBuild(document.getRootElement()).getPluginManagement());
    }

    @Test
    void testGetPlugins() throws Exception {
        String content = "<build></build>";
        Document document = builder.build(new StringReader(content));
        assertNotNull(new JDomBuild(document.getRootElement()).getPlugins());
        assertEquals(0, new JDomBuild(document.getRootElement()).getPlugins().size());

        content = "<build><plugins/></build>";
        document = builder.build(new StringReader(content));
        assertEquals(0, new JDomBuild(document.getRootElement()).getPlugins().size());

        content = "<build><plugins><plugin/></plugins></build>";
        document = builder.build(new StringReader(content));
        assertEquals(1, new JDomBuild(document.getRootElement()).getPlugins().size());
    }

    // All other methods throw UnsupportedOperationException

    @Test
    void testFlushPluginMap() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).flushPluginMap());
    }

    @Test
    void testAddExtension() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).addExtension(null));
    }

    @Test
    void testGetOutputDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getOutputDirectory());
    }

    @Test
    void testGetScriptSourceDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getScriptSourceDirectory());
    }

    @Test
    void testGetSourceDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getSourceDirectory());
    }

    @Test
    void testGetTestOutputDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getTestOutputDirectory());
    }

    @Test
    void testGetTestSourceDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getTestSourceDirectory());
    }

    @Test
    void testRemoveExtension() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).removeExtension(null));
    }

    @Test
    void testSetExtensions() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setExtensions(null));
    }

    @Test
    void testSetOutputDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setOutputDirectory(null));
    }

    @Test
    void testSetScriptSourceDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setScriptSourceDirectory(null));
    }

    @Test
    void testSetSourceDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setSourceDirectory(null));
    }

    @Test
    void testSetTestOutputDirectoryString() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setTestOutputDirectory(null));
    }

    @Test
    void testSetTestSourceDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setTestSourceDirectory(null));
    }

    @Test
    void testAddFilter() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).addFilter(null));
    }

    @Test
    void testAddResource() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).addResource(null));
    }

    @Test
    void testAddTestResource() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).addTestResource(null));
    }

    @Test
    void testGetDefaultGoal() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getDefaultGoal());
    }

    @Test
    void testGetDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getDirectory());
    }

    @Test
    void testGetFilters() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getFilters());
    }

    @Test
    void testGetFinalName() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getFinalName());
    }

    @Test
    void testGetResources() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getResources());
    }

    @Test
    void testGetTestResources() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getTestResources());
    }

    @Test
    void testRemoveFilter() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).removeFilter(null));
    }

    @Test
    void testRemoveResource() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).removeResource(null));
    }

    @Test
    void testRemoveTestResource() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).removeTestResource(null));
    }

    @Test
    void testSetDefaultGoal() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setDefaultGoal(null));
    }

    @Test
    void testSetDirectory() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setDirectory(null));
    }

    @Test
    void testSetFilters() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setFilters(null));
    }

    @Test
    void testSetFinalName() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setFinalName(null));
    }

    @Test
    void testSetResources() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setResources(null));
    }

    @Test
    void testSetTestResources() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setTestResources(null));
    }

    @Test
    void testSetPluginManagement() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setPluginManagement(null));
    }

    @Test
    void testAddPlugin() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).addPlugin(null));
    }

    @Test
    void testRemovePlugin() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).removePlugin(null));
    }

    @Test
    void testSetPlugins() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).setPlugins(null));
    }

    @Test
    void testGetPluginsAsMap() {
        assertThrows(UnsupportedOperationException.class, () -> new JDomBuild(null).getPluginsAsMap());
    }
}
