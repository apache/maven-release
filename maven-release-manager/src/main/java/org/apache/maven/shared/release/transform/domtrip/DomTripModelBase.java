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
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;

/**
 * DomTrip shared model base logic.
 *
 * @since 3.4
 */
public class DomTripModelBase {
    private final Element modelBase;
    private final Editor editor;

    public DomTripModelBase(Element modelBase, Editor editor) {
        this.modelBase = modelBase;
        this.editor = editor;
    }

    public Build getBuild() {
        return modelBase
                .childElement("build")
                .map(elm -> (Build) new DomTripBuild(elm, editor))
                .orElse(null);
    }

    public List<Dependency> getDependencies() {
        return modelBase
                .childElement("dependencies")
                .map(deps -> deps.childElements("dependency")
                        .map(dep -> (Dependency) new DomTripDependency(dep, editor))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public DependencyManagement getDependencyManagement() {
        return modelBase
                .childElement("dependencyManagement")
                .map(elm -> (DependencyManagement) new DomTripDependencyManagement(elm, editor))
                .orElse(null);
    }
}
