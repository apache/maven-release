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

/**
 * Editable view of what a project and a profile have in common.
 *
 * @since 3.4.0
 */
public interface PomModelBase {
    /**
     * @return the {@code build} element or {@code null} if absent
     */
    PomBuildBase getBuild();

    /**
     * @return the dependencies, never {@code null}
     */
    List<MavenCoordinate> getDependencies();

    /**
     * @return the dependencies of {@code dependencyManagement}, never {@code null}
     */
    List<MavenCoordinate> getManagedDependencies();

    /**
     * @return whether a {@code dependencyManagement} element exists
     */
    boolean hasDependencyManagement();

    /**
     * @return the plugins of {@code reporting/plugins}, never {@code null}
     */
    List<MavenCoordinate> getReportPlugins();

    /**
     * @return whether a {@code reporting} element exists
     */
    boolean hasReporting();
}
