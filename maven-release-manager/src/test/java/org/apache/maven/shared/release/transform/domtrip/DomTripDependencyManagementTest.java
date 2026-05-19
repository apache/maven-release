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
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomTripDependencyManagementTest {

    private DomTripDependencyManagement createDependencyManagement(String xml) {
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        return new DomTripDependencyManagement(document.root(), editor);
    }

    @Test
    void testGetDependenciesEmpty() {
        assertNotNull(createDependencyManagement("<dependencyManagement></dependencyManagement>")
                .getDependencies());
        assertEquals(
                0,
                createDependencyManagement("<dependencyManagement></dependencyManagement>")
                        .getDependencies()
                        .size());
    }

    @Test
    void testGetDependenciesEmptyContainer() {
        assertEquals(
                0,
                createDependencyManagement("<dependencyManagement><dependencies/></dependencyManagement>")
                        .getDependencies()
                        .size());
    }

    @Test
    void testGetDependenciesOne() {
        assertEquals(
                1,
                createDependencyManagement(
                                "<dependencyManagement><dependencies><dependency/></dependencies></dependencyManagement>")
                        .getDependencies()
                        .size());
    }

    @Test
    void testAddDependency() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createDependencyManagement("<dependencyManagement/>").addDependency(null));
    }

    @Test
    void testRemoveDependency() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createDependencyManagement("<dependencyManagement/>").removeDependency(null));
    }

    @Test
    void testSetDependencies() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> createDependencyManagement("<dependencyManagement/>").setDependencies(null));
    }
}
