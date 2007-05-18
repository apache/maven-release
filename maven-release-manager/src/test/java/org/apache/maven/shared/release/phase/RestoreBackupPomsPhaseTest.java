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

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public class RestoreBackupPomsPhaseTest
    extends AbstractBackupPomsPhaseTest
{
    private String expectedPomFilename = "expected-pom.xml";

    ReleasePhase getReleasePhase()
        throws Exception
    {
        return (ReleasePhase) lookup( ReleasePhase.ROLE, "restore-backup-poms" );
    }

    public void testBasicPom()
        throws Exception
    {
        String projectPath = "/projects/restore-backup-poms/basic-pom";

        //copy poms so tests are valid without clean
        File sourceDir = getTestFile( "src/test/resources" + projectPath );
        File testDir = getTestFile( "target/test-classes" + projectPath );
        FileUtils.copyDirectoryStructure( sourceDir, testDir );

        String testPath = "target/test-classes" + projectPath;

        runExecuteOnProjects( testPath );
    }

    public void testMultiModulePom()
        throws Exception
    {
        String projectPath = "/projects/restore-backup-poms/pom-with-modules";

        //copy poms so tests are valid without clean
        File sourceDir = getTestFile( "src/test/resources" + projectPath );
        File testDir = getTestFile( "target/test-classes" + projectPath );
        FileUtils.copyDirectoryStructure( sourceDir, testDir );

        String testPath = "target/test-classes" + projectPath;

        runExecuteOnProjects( testPath );
    }

    private void runExecuteOnProjects( String path )
        throws Exception
    {
        List projects = getReactorProjects( getTestPath( path ) );

        ReleaseDescriptor desc = new ReleaseDescriptor();
        desc.setScmSourceUrl( "scm:svn:http://myhost/myrepo" );
        phase.execute( desc, null, projects );

        testProjectIsRestored( projects );
    }

    private void testProjectIsRestored( List reactorProjects )
        throws Exception
    {
        for ( Iterator projects = reactorProjects.iterator(); projects.hasNext(); )
        {
            MavenProject project = (MavenProject) projects.next();

            File pomFile = project.getFile();

            File expectedFile = new File( pomFile.getParentFile(), expectedPomFilename );

            assertTrue( "Check if expected file exists.", expectedFile.exists() );

            String pomContents = FileUtils.fileRead( pomFile );

            String expectedContents = FileUtils.fileRead( expectedFile );

            assertTrue( "Check if pom and backup files are identical", pomContents.equals( expectedContents ) );
        }
    }
}
