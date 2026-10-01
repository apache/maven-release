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

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.transform.ModelETL;
import org.codehaus.plexus.util.xml.XmlStreamWriter;

/**
 * DomTrip implementation for extracting, transforming and loading the Model (pom.xml).
 * <p>
 * DomTrip provides lossless XML round-tripping, preserving all formatting details
 * including comments, whitespace, attribute order, quote styles, and entity encoding.
 * This eliminates the need for the intro/outtro hacks required by the JDOM2 implementation.
 *
 * @since 3.4
 */
public class DomTripModelETL implements ModelETL {
    private ReleaseDescriptor releaseDescriptor;

    private MavenProject project;

    private Document document;

    private Editor editor;

    public void setReleaseDescriptor(ReleaseDescriptor releaseDescriptor) {
        this.releaseDescriptor = releaseDescriptor;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    @Override
    public void extract(File pomFile) throws ReleaseExecutionException {
        try {
            document = Document.of(pomFile.toPath());
            editor = new Editor(document);
        } catch (Exception e) {
            throw new ReleaseExecutionException("Error reading POM: " + e.getMessage(), e);
        }
    }

    @Override
    public void transform() {}

    @Override
    public void load(File targetFile) throws ReleaseExecutionException {
        writePom(targetFile);
    }

    @Override
    public Model getModel() {
        return new DomTripModel(document, editor, releaseDescriptor);
    }

    private void writePom(File pomFile) throws ReleaseExecutionException {
        if (releaseDescriptor.isAddSchema()) {
            Element rootElement = document.root();

            String modelVersion = project.getModelVersion();
            String pomNamespaceUri = "http://maven.apache.org/POM/" + modelVersion;
            String xsiNamespaceUri = "http://www.w3.org/2001/XMLSchema-instance";

            rootElement.namespaceDeclaration("", pomNamespaceUri);
            rootElement.namespaceDeclaration("xsi", xsiNamespaceUri);

            if (rootElement.attribute("xsi:schemaLocation") == null) {
                rootElement.attribute(
                        "xsi:schemaLocation",
                        pomNamespaceUri + " https://maven.apache.org/xsd/maven-" + modelVersion + ".xsd");
            }
        }

        try (Writer writer = new XmlStreamWriter(pomFile)) {
            writer.write(document.toXml());
        } catch (IOException e) {
            throw new ReleaseExecutionException("Error writing POM: " + e.getMessage(), e);
        }
    }
}
