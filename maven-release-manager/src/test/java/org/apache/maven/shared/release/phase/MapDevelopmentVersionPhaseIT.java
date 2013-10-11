package org.apache.maven.shared.release.phase;

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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.plexus.PlexusTestCase;

public class MapDevelopmentVersionPhaseIT
    extends PlexusTestCase
{
    private MapVersionsPhase mapVersionsPhase;

    @Override
    protected InputStream getCustomConfiguration()
        throws Exception
    {
        return MapVersionsPhase.class.getResourceAsStream( "/META-INF/plexus/components.xml" );
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        mapVersionsPhase = (MapVersionsPhase) lookup( ReleasePhase.class.getName(), "map-development-versions" );
    }
    
    private static MavenProject createProject( String artifactId, String version )
    {
        Model model = new Model();
        model.setGroupId( "groupId" );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        return new MavenProject( model );
    }

    public void testNoUpdateWorkingCopyVersions() throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setInteractive( false );
        releaseDescriptor.setUpdateWorkingCopyVersions( false );
        
        List<MavenProject> reactorProjects = Collections.singletonList( createProject( "artifactId", "1.0" ) );
        mapVersionsPhase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );
        
        assertEquals( "1.0", releaseDescriptor.getDevelopmentVersions().get( "groupId:artifactId" ) );
    }
}
