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
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.transform.PomModel;
import org.apache.maven.shared.release.transform.PomPlugin;
import org.apache.maven.shared.release.transform.PomProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomTripPomViewsTest {
    private static final String POM = "<project>"
            + "<parent><groupId>g</groupId><artifactId>p</artifactId><version>1</version></parent>"
            + "<artifactId>a</artifactId>"
            + "<dependencies><dependency><groupId>g</groupId><artifactId>d</artifactId><version>1</version></dependency></dependencies>"
            + "<dependencyManagement><dependencies><dependency><groupId>g</groupId><artifactId>m</artifactId><version>2</version></dependency></dependencies></dependencyManagement>"
            + "<reporting><plugins><plugin><artifactId>r</artifactId><version>3</version></plugin></plugins></reporting>"
            + "<build>"
            + "<extensions><extension><groupId>g</groupId><artifactId>e</artifactId><version>4</version></extension></extensions>"
            + "<plugins><plugin><artifactId>b</artifactId><version>5</version>"
            + "<dependencies><dependency><groupId>g</groupId><artifactId>pd</artifactId><version>6</version></dependency></dependencies>"
            + "</plugin></plugins>"
            + "<pluginManagement><plugins><plugin><artifactId>pm</artifactId><version>7</version></plugin></plugins></pluginManagement>"
            + "</build>"
            + "<profiles><profile><id>x</id><reporting><plugins><plugin><artifactId>pr</artifactId></plugin></plugins></reporting>"
            + "<build><plugins><plugin><artifactId>xp</artifactId><version>8</version></plugin></plugins></build>"
            + "</profile></profiles>"
            + "</project>";

    private PomModel view(Document document, Editor editor) {
        return DomTripPomViews.of(new DomTripModel(document.root(), editor, new ReleaseDescriptorBuilder().build()));
    }

    @Test
    void readsTheSameElementsAsTheModel() {
        Document document = Document.of(POM);
        PomModel model = view(document, new Editor(document));

        assertEquals("1", model.getParent().getVersion());
        assertEquals("d", model.getDependencies().get(0).getArtifactId());
        assertTrue(model.hasDependencyManagement());
        assertEquals("m", model.getManagedDependencies().get(0).getArtifactId());
        assertTrue(model.hasReporting());
        assertEquals("r", model.getReportPlugins().get(0).getArtifactId());
        assertEquals("e", model.getBuild().getExtensions().get(0).getArtifactId());
        assertEquals("b", model.getBuild().getPlugins().get(0).getArtifactId());
        assertEquals("plugin", model.getBuild().getPlugins().get(0).getName());
        assertEquals(
                "pd",
                model.getBuild().getPlugins().get(0).getDependencies().get(0).getArtifactId());
        assertTrue(model.getBuild().hasPluginManagement());
        assertEquals("pm", model.getBuild().getManagedPlugins().get(0).getArtifactId());
    }

    @Test
    void profileReportingIsNotExposed() {
        Document document = Document.of(POM);
        PomProfile profile = view(document, new Editor(document)).getProfiles().get(0);

        // the DomTrip profile has never read its reporting section; the rewrite must keep ignoring it
        assertFalse(profile.hasReporting());
        assertTrue(profile.getReportPlugins().isEmpty());
        assertEquals("xp", profile.getBuild().getPlugins().get(0).getArtifactId());
    }

    @Test
    void absentSectionsAreEmpty() {
        Document document = Document.of("<project><artifactId>a</artifactId></project>");
        PomModel model = view(document, new Editor(document));

        assertNull(model.getBuild());
        assertNull(model.getParent());
        assertNull(model.getProperties());
        assertTrue(model.getProfiles().isEmpty());
        assertTrue(model.getDependencies().isEmpty());
        assertTrue(model.getManagedDependencies().isEmpty());
        assertTrue(model.getReportPlugins().isEmpty());
    }

    @Test
    void writesGoThroughToTheDocument() {
        Document document = Document.of(POM);
        PomModel model = view(document, new Editor(document));

        PomPlugin plugin = model.getBuild().getPlugins().get(0);
        plugin.setVersion("55");
        model.getParent().setVersion("11");
        model.setVersion("9");

        String xml = document.toXml();
        assertTrue(xml.contains("<version>55</version>"));
        assertTrue(xml.contains("<version>11</version>"));
        assertTrue(xml.contains("<version>9</version>"));
    }
}
