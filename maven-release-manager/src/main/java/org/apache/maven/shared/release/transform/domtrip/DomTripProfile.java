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

import java.util.List;

import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Profile;

/**
 * DomTrip implementation of poms PROFILE element.
 *
 * @since 3.4
 */
public class DomTripProfile extends Profile {
    private final DomTripModelBase modelBase;

    public DomTripProfile(Element profile, Editor editor) {
        this.modelBase = new DomTripModelBase(profile, editor);
    }

    @Override
    public BuildBase getBuild() {
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
}
