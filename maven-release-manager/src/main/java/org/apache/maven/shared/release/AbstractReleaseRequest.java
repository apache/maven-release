package org.apache.maven.shared.release;

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

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;

/**
 * 
 * @author Robert Scholte
 * @since 2.3
 */
public abstract class AbstractReleaseRequest
{
    private ReleaseDescriptor releaseDescriptor;
    
    private List<MavenProject> reactorProjects;

    private ReleaseManagerListener releaseManagerListener;
    
    /**
     * @return the releaseDescriptor
     */
    public ReleaseDescriptor getReleaseDescriptor()
    {
        return releaseDescriptor;
    }

    /**
     * @param releaseDescriptor the releaseDescriptor to set
     */
    public void setReleaseDescriptor( ReleaseDescriptor releaseDescriptor )
    {
        this.releaseDescriptor = releaseDescriptor;
    }

    /**
     * @return the reactorProjects
     */
    public List<MavenProject> getReactorProjects()
    {
        return reactorProjects;
    }

    /**
     * @param reactorProjects the reactorProjects to set
     */
    public void setReactorProjects( List<MavenProject> reactorProjects )
    {
        this.reactorProjects = reactorProjects;
    }

    /**
     * @return the releaseManagerListener
     */
    public ReleaseManagerListener getReleaseManagerListener()
    {
        return releaseManagerListener;
    }

    /**
     * @param releaseManagerListener the releaseManagerListener to set
     */
    public void setReleaseManagerListener( ReleaseManagerListener releaseManagerListener )
    {
        this.releaseManagerListener = releaseManagerListener;
    }
}