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
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JDomDependencyManagementTest {
    private SAXBuilder builder = new SAXBuilder();

    @Test
    public void testGetDependencies() throws Exception {
        String content = "<dependencyManamgement></dependencyManamgement>";
        Document document = builder.build(new StringReader(content));
        assertNotNull(new JDomDependencyManagement(document.getRootElement()).getDependencies());
        assertEquals(
                0,
                new JDomDependencyManagement(document.getRootElement())
                        .getDependencies()
                        .size());

        content = "<dependencyManamgement><dependencies/></dependencyManamgement>";
        document = builder.build(new StringReader(content));
        assertEquals(
                0,
                new JDomDependencyManagement(document.getRootElement())
                        .getDependencies()
                        .size());

        content = "<dependencyManamgement><dependencies><dependency/></dependencies></dependencyManamgement>";
        document = builder.build(new StringReader(content));
        assertEquals(
                1,
                new JDomDependencyManagement(document.getRootElement())
                        .getDependencies()
                        .size());
    }

    // All other methods throw UnsupportedOperationException

    @Test
    public void testAddDependency() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependencyManagement(null).addDependency(null));
    }

    @Test
    public void testRemoveDependency() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependencyManagement(null).addDependency(null));
    }

    @Test
    public void testSetDependenciesListOfDependency() {
        assertThrows(UnsupportedOperationException.class, () ->
            new JDomDependencyManagement(null).setDependencies(null));
    }
}
