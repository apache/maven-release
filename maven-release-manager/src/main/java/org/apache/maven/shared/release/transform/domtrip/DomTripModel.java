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

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Scm;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.util.CiFriendlyVersion;

/**
 * DomTrip implementation of poms PROJECT element.
 *
 * @since 3.4
 */
public class DomTripModel extends Model {
    private final Element project;
    private final Editor editor;
    private final DomTripModelBase modelBase;
    private final ReleaseDescriptor releaseDescriptor;

    public DomTripModel(Document document, Editor editor, ReleaseDescriptor releaseDescriptor) {
        this(document.root(), editor, releaseDescriptor);
    }

    public DomTripModel(Element project, Editor editor, ReleaseDescriptor releaseDescriptor) {
        this.project = project;
        this.editor = editor;
        this.releaseDescriptor = releaseDescriptor;
        this.modelBase = new DomTripModelBase(project, editor);
    }

    @Override
    public Build getBuild() {
        return modelBase.getBuild();
    }

    @Override
    public List<Dependency> getDependencies() {
        return modelBase.getDependencies();
    }

    @Override
    public DependencyManagement getDependencyManagement() {
        return modelBase.getDependencyManagement();
    }

    @Override
    public Parent getParent() {
        return project.childElement("parent")
                .map(elm -> (Parent) new DomTripParent(elm, editor))
                .orElse(null);
    }

    @Override
    public List<Profile> getProfiles() {
        return project.childElement("profiles")
                .map(profilesElm -> profilesElm
                        .childElements("profile")
                        .map(profileElm -> (Profile) new DomTripProfile(profileElm, editor))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public Properties getProperties() {
        return project.childElement("properties")
                .map(elm -> (Properties) new DomTripProperties(elm, editor))
                .orElse(null);
    }

    @Override
    public Reporting getReporting() {
        return project.childElement("reporting")
                .map(elm -> (Reporting) new DomTripReporting(elm, editor))
                .orElse(null);
    }

    @Override
    public void setScm(Scm scm) {
        if (scm == null) {
            project.childElement("scm").ifPresent(editor::removeElement);
        } else {
            Element scmRoot = editor.addElement(project, "scm");

            // Write current values to DomTrip tree
            Scm domTripScm = new DomTripScm(scmRoot, editor);
            domTripScm.setConnection(scm.getConnection());
            domTripScm.setDeveloperConnection(scm.getDeveloperConnection());
            domTripScm.setTag(scm.getTag());
            domTripScm.setUrl(scm.getUrl());
        }
    }

    @Override
    public Scm getScm() {
        return project.childElement("scm")
                .map(elm -> (Scm) new DomTripScm(elm, editor))
                .orElse(null);
    }

    @Override
    public void setVersion(String version) {
        Element versionElement = project.childElement("version").orElse(null);

        String parentVersion;
        Element parent = project.childElement("parent").orElse(null);
        if (parent != null) {
            parentVersion = parent.childTextTrimmed("version");
        } else {
            parentVersion = null;
        }

        if (versionElement == null) {
            // never add version when parent references CI friendly property
            if (!(parentVersion != null && CiFriendlyVersion.isCiFriendlyVersion(parentVersion))
                    && !version.equals(parentVersion)) {
                // we will add this after artifactId, since it was missing but different from the inherited version
                Element artifactIdElement = project.childElement("artifactId").orElse(null);
                if (artifactIdElement != null) {
                    versionElement = editor.insertElementAfter(artifactIdElement, "version", version);
                } else {
                    editor.addElement(project, "version", version);
                }
            }
        } else {
            String versionText = versionElement.textContentTrimmed();
            if (versionText != null && CiFriendlyVersion.isCiFriendlyVersion(versionText)) {
                // try to rewrite property if CI friendly expression is used
                CiFriendlyVersion.rewriteCiFriendlyProperties(version, getProperties(), releaseDescriptor);
            } else {
                DomTripUtils.rewriteValue(editor, versionElement, version);
            }
        }
    }
}
