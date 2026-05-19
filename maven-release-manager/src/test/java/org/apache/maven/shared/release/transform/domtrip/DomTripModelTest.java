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
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DomTripModelTest {
    private final ReleaseDescriptor releaseDescriptor = new ReleaseDescriptorBuilder().build();

    private DomTripModel createModel(String xml) {
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        return new DomTripModel(document.root(), editor, releaseDescriptor);
    }

    @Test
    void testGetScmMissing() {
        assertNull(createModel("<project></project>").getScm());
    }

    @Test
    void testGetScmPresent() {
        assertNotNull(createModel("<project><scm/></project>").getScm());
    }

    @Test
    void testSetScm() {
        String xml = "<project></project>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Model model = new DomTripModel(document.root(), editor, releaseDescriptor);
        assertNull(model.getScm());

        model.setScm(new Scm());
        assertNotNull(model.getScm());

        model.setScm(null);
        assertNull(model.getScm());
    }

    @Test
    void testSetVersionNoExistingVersion() {
        String xml = "<project><artifactId>test</artifactId></project>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();
        Model model = new DomTripModel(root, editor, releaseDescriptor);
        assertNull(root.childTextTrimmed("version"));

        model.setVersion("VERSION");
        assertEquals("VERSION", root.childTextTrimmed("version"));
    }

    @Test
    void testSetVersionExisting() {
        String xml = "<project><version>OLD</version></project>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();
        Model model = new DomTripModel(root, editor, releaseDescriptor);

        model.setVersion("NEW");
        assertEquals("NEW", root.childTextTrimmed("version"));
    }

    @Test
    void testSetVersionInheritedFromParentCiFriendly() {
        String xml = "<project><parent><version>${revision}${changelist}</version></parent></project>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();
        Model model = new DomTripModel(root, editor, releaseDescriptor);
        assertNull(root.childElement("version").orElse(null));

        model.setVersion("PARENT_VERSION");
        // should not add version element when parent uses CI friendly property
        assertNull(root.childElement("version").orElse(null));
    }

    @Test
    void testSetVersionSameAsParent() {
        String xml = "<project><parent><version>PARENT_VERSION</version></parent></project>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();
        Model model = new DomTripModel(root, editor, releaseDescriptor);

        model.setVersion("PARENT_VERSION");
        // should not add version element when version equals parent version
        assertNull(root.childElement("version").orElse(null));

        model.setVersion("DIFFERENT_VERSION");
        assertEquals("DIFFERENT_VERSION", root.childTextTrimmed("version"));
    }

    @Test
    void testGetParentMissing() {
        assertNull(createModel("<project></project>").getParent());
    }

    @Test
    void testGetParentPresent() {
        assertNotNull(createModel("<project><parent><version>1.0</version></parent></project>")
                .getParent());
    }

    @Test
    void testGetProfilesEmpty() {
        assertEquals(0, createModel("<project></project>").getProfiles().size());
    }

    @Test
    void testGetProfilesPresent() {
        assertEquals(
                1,
                createModel("<project><profiles><profile/></profiles></project>")
                        .getProfiles()
                        .size());
    }

    @Test
    void testGetPropertiesMissing() {
        assertNull(createModel("<project></project>").getProperties());
    }

    @Test
    void testGetPropertiesPresent() {
        assertNotNull(createModel("<project><properties><key>value</key></properties></project>")
                .getProperties());
    }

    @Test
    void testGetReportingMissing() {
        assertNull(createModel("<project></project>").getReporting());
    }

    @Test
    void testGetReportingPresent() {
        assertNotNull(createModel("<project><reporting/></project>").getReporting());
    }

    @Test
    void testGetBuildMissing() {
        assertNull(createModel("<project></project>").getBuild());
    }

    @Test
    void testGetBuildPresent() {
        assertNotNull(createModel("<project><build/></project>").getBuild());
    }

    @Test
    void testGetDependenciesEmpty() {
        assertEquals(0, createModel("<project></project>").getDependencies().size());
    }

    @Test
    void testGetDependenciesPresent() {
        assertEquals(
                1,
                createModel("<project><dependencies><dependency/></dependencies></project>")
                        .getDependencies()
                        .size());
    }

    @Test
    void testGetDependencyManagementMissing() {
        assertNull(createModel("<project></project>").getDependencyManagement());
    }

    @Test
    void testGetDependencyManagementPresent() {
        assertNotNull(createModel("<project><dependencyManagement/></project>").getDependencyManagement());
    }
}
