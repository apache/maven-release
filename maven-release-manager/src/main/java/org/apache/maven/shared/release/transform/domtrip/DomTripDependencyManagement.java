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
import java.util.stream.Collectors;

import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;

/**
 * DomTrip implementation of poms DEPENDENCYMANAGEMENT element.
 *
 * @since 3.4
 */
public class DomTripDependencyManagement extends DependencyManagement {
    private final Element dependencyManagement;
    private final Editor editor;

    public DomTripDependencyManagement(Element dependencyManagement, Editor editor) {
        this.dependencyManagement = dependencyManagement;
        this.editor = editor;
    }

    @Override
    public void addDependency(Dependency dependency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Dependency> getDependencies() {
        return dependencyManagement
                .childElement("dependencies")
                .map(deps -> deps.childElements("dependency")
                        .map(dep -> (Dependency) new DomTripDependency(dep, editor))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public void removeDependency(Dependency dependency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDependencies(List<Dependency> dependencies) {
        throw new UnsupportedOperationException();
    }
}
