package org.apache.maven.shared.release.util;

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

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class ReleaseUtil
{
    public static final String RELEASE_POMv4 = "release-pom.xml";
    
    private static final String POMv4 = "pom.xml";

    private ReleaseUtil()
    {
    }

    public static MavenProject getRootProject( List reactorProjects )
    {
        MavenProject project = (MavenProject) reactorProjects.get( 0 );
        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject currentProject = (MavenProject) i.next();
            if ( currentProject.isExecutionRoot() )
            {
                project = currentProject;
                break;
            }
        }

        return project;
    }
    
    public static File getStandardPom( MavenProject project )
    {
        File pom = project.getFile();
        File releasePom = getReleasePom( project );
        
        if ( pom.equals( releasePom ))
        {
            pom = new File( pom.getParent(), POMv4 );
        }
        
        return pom;
    }
    
    public static File getReleasePom( MavenProject project )
    {
        return new File( project.getFile().getParent(), RELEASE_POMv4 );
    }
}
