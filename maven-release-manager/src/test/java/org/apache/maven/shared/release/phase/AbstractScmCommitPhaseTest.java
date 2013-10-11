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
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.junit.Test;

public class AbstractScmCommitPhaseTest
{
    @Test
    public void testDefaultCreatePomFiles()
        throws Exception
    {
        List<File> files =
            AbstractScmCommitPhase.createPomFiles(  new ReleaseDescriptor(),
                                                   createProject( "artifactId", "1.0-SNAPSHOT", new File( "pom.xml" ) ) );
        assertEquals( "Number of created files", files.size(), 1 );
        assertTrue( files.contains( new File( "pom.xml" ) ) );
    }


    @Test
    public void testCreatePomFilesSuppressCommitBeforeTag()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setSuppressCommitBeforeTagOrBranch( true );
        List<File> files =
            AbstractScmCommitPhase.createPomFiles(  releaseDescriptor,
                                                   createProject( "artifactId", "1.0-SNAPSHOT", new File( "pom.xml" ) ) );
        assertEquals( "Number of created files", files.size(), 1 );
        assertTrue( files.contains( new File( "pom.xml" ) ) );
    }

    @Test
    public void testCreatePomFilesWithReleasePom()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setGenerateReleasePoms( true );
        List<File> files =
            AbstractScmCommitPhase.createPomFiles( releaseDescriptor,
                                                   createProject( "artifactId", "1.0-SNAPSHOT", new File( "pom.xml" ) ) );
        assertEquals( "Number of created files", files.size(), 2 );
        assertTrue( files.contains( new File( "pom.xml" ) ) );
        assertTrue( files.contains( new File( "release-pom.xml" ) ) );
    }

    @Test
    public void testCreatePomFilesWithReleasePomAndSuppressCommitBeforeTag()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setGenerateReleasePoms( true );
        releaseDescriptor.setSuppressCommitBeforeTagOrBranch( true );
        List<File> files =
            AbstractScmCommitPhase.createPomFiles( releaseDescriptor,
                                                   createProject( "artifactId", "1.0-SNAPSHOT", new File( "pom.xml" ) ) );
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
