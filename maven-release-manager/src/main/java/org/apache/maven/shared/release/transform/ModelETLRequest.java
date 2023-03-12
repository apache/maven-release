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

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;

/**
 * <p>ModelETLRequest class.</p>
 *
 * @author Robert Scholte
 * @since 3.0
 */
public class ModelETLRequest {

    private MavenProject project;

    private ReleaseDescriptor releaseDescriptor;

    /**
     * <p>Getter for the field <code>releaseDescriptor.lineSeparator</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getLineSeparator() {
        return releaseDescriptor.getLineSeparator();
    }

    /**
     * <p>Getter for the field <code>project</code>.</p>
     *
     * @return a {@link org.apache.maven.project.MavenProject} object
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * <p>Setter for the field <code>project</code>.</p>
     *
     * @param project a {@link org.apache.maven.project.MavenProject} object
     */
    public void setProject(MavenProject project) {
        this.project = project;
    }

    /**
     * <p>Getter for the field <code>releaseDescriptor</code>.</p>
     *
     * @return a {@link org.apache.maven.shared.release.config.ReleaseDescriptor} object
     */
    public ReleaseDescriptor getReleaseDescriptor() {
        return releaseDescriptor;
    }

    /**
     * <p>Setter for the field <code>releaseDescriptor</code>.</p>
     *
     * @param releaseDescriptor a {@link org.apache.maven.shared.release.config.ReleaseDescriptor} object
     */
    public void setReleaseDescriptor(ReleaseDescriptor releaseDescriptor) {
        this.releaseDescriptor = releaseDescriptor;
    }
}
