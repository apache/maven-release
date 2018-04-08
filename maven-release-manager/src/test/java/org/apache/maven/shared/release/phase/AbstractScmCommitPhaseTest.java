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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.junit.Test;

public class AbstractScmCommitPhaseTest
{
    protected ReleaseDescriptorBuilder createReleaseDescriptorBuilder( List<MavenProject> reactorProjects )
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        
        for ( MavenProject reactorProject : reactorProjects )
        {
            String projectKey =
                ArtifactUtils.versionlessKey( reactorProject.getGroupId(), reactorProject.getArtifactId() );
            
            builder.addProjectPomFile( projectKey, reactorProject.getFile().getPath() );
        }
        
        return builder;
    }
    
    @Test
    public void testDefaultCreatePomFiles()
        throws Exception
    {
        MavenProject project = createProject( "artifactId", "1.0-SNAPSHOT", new File( "pom.xml" ) );
        
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( Collections.singletonList( project ) );
        
        List<File> files =
            AbstractScmCommitPhase.createPomFiles( ReleaseUtils.buildReleaseDescriptor( builder ), project );

        assertEquals( "Number of created files", files.size(), 1 );
        assertTrue( files.contains( new File( "pom.xml" ) ) );
    }


    @Test
    public void testCreatePomFilesSuppressCommitBeforeTag()
        throws Exception
    {
        MavenProject project = createProject( "artifactId", "1.0-SNAPSHOT", new File( "pom.xml" ) );
                         
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( Collections.singletonList( project ) );
        builder.setSuppressCommitBeforeTagOrBranch( true );

        List<File> files =
            AbstractScmCommitPhase.createPomFiles(  ReleaseUtils.buildReleaseDescriptor( builder ), project );
        
        assertEquals( "Number of created files", files.size(), 1 );
        assertTrue( files.contains( new File( "pom.xml" ) ) );
    }

    @Test
    public void testCreatePomFilesWithReleasePom()
        throws Exception
    {
        MavenProject project = createProject( "artifactId", "1.0-SNAPSHOT", new File( "pom.xml" ) );
        
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( Collections.singletonList( project ) );
        builder.setGenerateReleasePoms( true );
        
        List<File> files =
            AbstractScmCommitPhase.createPomFiles( ReleaseUtils.buildReleaseDescriptor( builder ), project );

        assertEquals( "Number of created files", files.size(), 2 );
        assertTrue( files.contains( new File( "pom.xml" ) ) );
        assertTrue( files.contains( new File( "release-pom.xml" ) ) );
    }

    @Test
    public void testCreatePomFilesWithReleasePomAndSuppressCommitBeforeTag()
        throws Exception
    {
        MavenProject project = createProject( "artifactId", "1.0-SNAPSHOT", new File( "pom.xml" ) );

        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( Collections.singletonList( project ) );
        builder.setGenerateReleasePoms( true );
        builder.setSuppressCommitBeforeTagOrBranch( true );
        
        List<File> files =
            AbstractScmCommitPhase.createPomFiles( ReleaseUtils.buildReleaseDescriptor( builder ), project );
        assertEquals( "Number of created files", files.size(), 1 );
        assertTrue( files.contains( new File( "pom.xml" ) ) );
    }

    private static MavenProject createProject( String artifactId, String version, File file )
    {
        Model model = new Model();
        model.setGroupId( "groupId" );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        MavenProject project = new MavenProject( model );
        project.setFile( file );
        return project;
    }
}
