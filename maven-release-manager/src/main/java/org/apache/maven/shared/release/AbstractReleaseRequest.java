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
package org.apache.maven.shared.release;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;

/**
 * <p>Abstract AbstractReleaseRequest class.</p>
 *
 * @author Robert Scholte
 * @since 2.3
 */
public abstract class AbstractReleaseRequest {
    private ReleaseDescriptorBuilder releaseDescriptorBuilder;

    private List<MavenProject> reactorProjects;

    private ReleaseManagerListener releaseManagerListener;

    /**
     * <p>Getter for the field <code>releaseDescriptorBuilder</code>.</p>
     *
     * @return the releaseDescriptor
     */
    public ReleaseDescriptorBuilder getReleaseDescriptorBuilder() {
        return releaseDescriptorBuilder;
    }

    /**
     * <p>Setter for the field <code>releaseDescriptorBuilder</code>.</p>
     *
     * @param releaseDescriptor the releaseDescriptor to set
     */
    public void setReleaseDescriptorBuilder(ReleaseDescriptorBuilder releaseDescriptor) {
        this.releaseDescriptorBuilder = releaseDescriptor;
    }

    /**
     * <p>Getter for the field <code>reactorProjects</code>.</p>
     *
     * @return the reactorProjects
     */
    public List<MavenProject> getReactorProjects() {
        return reactorProjects;
    }

    /**
     * <p>Setter for the field <code>reactorProjects</code>.</p>
     *
     * @param reactorProjects the reactorProjects to set
     */
    public void setReactorProjects(List<MavenProject> reactorProjects) {
        this.reactorProjects = reactorProjects;
    }

    /**
     * <p>Getter for the field <code>releaseManagerListener</code>.</p>
     *
     * @return the releaseManagerListener
     */
    public ReleaseManagerListener getReleaseManagerListener() {
        return releaseManagerListener;
    }

    /**
     * <p>Setter for the field <code>releaseManagerListener</code>.</p>
     *
     * @param releaseManagerListener the releaseManagerListener to set
     */
    public void setReleaseManagerListener(ReleaseManagerListener releaseManagerListener) {
        this.releaseManagerListener = releaseManagerListener;
    }
}
