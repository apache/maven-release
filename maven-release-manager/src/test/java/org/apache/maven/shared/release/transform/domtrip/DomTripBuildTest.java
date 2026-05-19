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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomTripBuildTest {

    private DomTripBuild createBuild(String xml) {
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        return new DomTripBuild(document.root(), editor);
    }

    @Test
    void testGetExtensionsEmpty() {
        assertNotNull(createBuild("<build></build>").getExtensions());
        assertEquals(0, createBuild("<build></build>").getExtensions().size());
    }

    @Test
    void testGetExtensionsEmptyContainer() {
        assertEquals(
                0, createBuild("<build><extensions/></build>").getExtensions().size());
    }

    @Test
    void testGetExtensionsOne() {
        assertEquals(
                1,
                createBuild("<build><extensions><extension/></extensions></build>")
                        .getExtensions()
                        .size());
    }

    @Test
    void testGetPluginManagementMissing() {
        assertNull(createBuild("<build></build>").getPluginManagement());
    }

    @Test
    void testGetPluginManagementPresent() {
        assertNotNull(createBuild("<build><pluginManagement/></build>").getPluginManagement());
    }

    @Test
    void testGetPluginsEmpty() {
        assertNotNull(createBuild("<build></build>").getPlugins());
        assertEquals(0, createBuild("<build></build>").getPlugins().size());
    }

    @Test
    void testGetPluginsEmptyContainer() {
        assertEquals(0, createBuild("<build><plugins/></build>").getPlugins().size());
    }

    @Test
    void testGetPluginsOne() {
        assertEquals(
                1,
                createBuild("<build><plugins><plugin/></plugins></build>")
                        .getPlugins()
                        .size());
    }

    @Test
    void testFlushPluginMap() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").flushPluginMap());
    }

    @Test
    void testAddExtension() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").addExtension(null));
    }

    @Test
    void testGetOutputDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getOutputDirectory());
    }

    @Test
    void testGetScriptSourceDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getScriptSourceDirectory());
    }

    @Test
    void testGetSourceDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getSourceDirectory());
    }

    @Test
    void testGetTestOutputDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getTestOutputDirectory());
    }

    @Test
    void testGetTestSourceDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getTestSourceDirectory());
    }

    @Test
    void testRemoveExtension() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").removeExtension(null));
    }

    @Test
    void testSetExtensions() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setExtensions(null));
    }

    @Test
    void testSetOutputDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setOutputDirectory(null));
    }

    @Test
    void testSetScriptSourceDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setScriptSourceDirectory(null));
    }

    @Test
    void testSetSourceDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setSourceDirectory(null));
    }

    @Test
    void testSetTestOutputDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setTestOutputDirectory(null));
    }

    @Test
    void testSetTestSourceDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setTestSourceDirectory(null));
    }

    @Test
    void testAddFilter() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").addFilter(null));
    }

    @Test
    void testAddResource() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").addResource(null));
    }

    @Test
    void testAddTestResource() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").addTestResource(null));
    }

    @Test
    void testGetDefaultGoal() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getDefaultGoal());
    }

    @Test
    void testGetDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getDirectory());
    }

    @Test
    void testGetFilters() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getFilters());
    }

    @Test
    void testGetFinalName() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getFinalName());
    }

    @Test
    void testGetResources() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getResources());
    }

    @Test
    void testGetTestResources() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getTestResources());
    }

    @Test
    void testRemoveFilter() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").removeFilter(null));
    }

    @Test
    void testRemoveResource() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").removeResource(null));
    }

    @Test
    void testRemoveTestResource() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").removeTestResource(null));
    }

    @Test
    void testSetDefaultGoal() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setDefaultGoal(null));
    }

    @Test
    void testSetDirectory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setDirectory(null));
    }

    @Test
    void testSetFilters() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setFilters(null));
    }

    @Test
    void testSetFinalName() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setFinalName(null));
    }

    @Test
    void testSetResources() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setResources(null));
    }

    @Test
    void testSetTestResources() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setTestResources(null));
    }

    @Test
    void testSetPluginManagement() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setPluginManagement(null));
    }

    @Test
    void testAddPlugin() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").addPlugin(null));
    }

    @Test
    void testRemovePlugin() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").removePlugin(null));
    }

    @Test
    void testSetPlugins() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").setPlugins(null));
    }

    @Test
    void testGetPluginsAsMap() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createBuild("<build/>").getPluginsAsMap());
    }
}
