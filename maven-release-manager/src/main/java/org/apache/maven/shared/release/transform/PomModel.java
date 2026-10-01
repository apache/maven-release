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
package org.apache.maven.shared.release.transform;

import java.util.List;
import java.util.Properties;

/**
 * Editable view of the {@code project} element of a POM. The DomTrip classes that edit the POM also extend the
 * Maven 3 model classes, which declare {@code getDependencies()}, {@code getProfiles()} and {@code getPlugins()}
 * with other return types, so this view is a separate object (see {@link ModelETL#getPomModel()}) instead of
 * an interface of those classes.
 *
 * @since 3.4
 */
public interface PomModel extends PomModelBase {
    @Override
    PomBuild getBuild();

    /**
     * @return the {@code parent} element or {@code null} if absent
     */
    MavenCoordinate getParent();

    /**
     * @return the profiles, never {@code null}
     */
    List<PomProfile> getProfiles();

    /**
     * @return the {@code properties} element or {@code null} if absent; writes go to the document
     */
    Properties getProperties();

    /**
     * Sets the version of the project, adding the element when the version differs from the inherited one.
     *
     * @param version the new version
     */
    void setVersion(String version);
}
