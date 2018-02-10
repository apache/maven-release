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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

/**
 * @author Edwin Punzalan
 */
public class RestoreBackupPomsPhaseTest
    extends AbstractBackupPomsPhaseTest
{
    private String expectedPomFilename = "expected-pom.xml";

    @Override
    ReleasePhase getReleasePhase()
        throws Exception
    {
        return lookup( ReleasePhase.class, "restore-backup-poms" );
    }

    @Test
    public void testBasicPom()
        throws Exception
    {
        String projectPath = "/projects/restore-backup-poms/basic-pom";

        // copy poms so tests are valid without clean
        File sourceDir = getTestFile( "src/test/resources" + projectPath );
        File testDir = getTestFile( "target/test-classes" + projectPath );
        FileUtils.copyDirectoryStructure( sourceDir, testDir );

        String testPath = "target/test-classes" + projectPath;

        runExecuteOnProjects( testPath );
    }

    @Test
    public void testMultiModulePom()
        throws Exception
    {
        String projectPath = "/projects/restore-backup-poms/pom-with-modules";

        // copy poms so tests are valid without clean
        File sourceDir = getTestFile( "src/test/resources" + projectPath );
        File testDir = getTestFile( "target/test-classes" + projectPath );
        FileUtils.copyDirectoryStructure( sourceDir, testDir );

        String testPath = "target/test-classes" + projectPath;

        runExecuteOnProjects( testPath );
    }

    private void runExecuteOnProjects( String path )
        throws Exception
    {
        List<MavenProject> projects = getReactorProjects( getTestPath( path ) );

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm:svn:http://myhost/myrepo" );
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), projects );

        testProjectIsRestored( projects );
    }

    private void testProjectIsRestored( List<MavenProject> reactorProjects )
        throws Exception
    {
        for ( MavenProject project : reactorProjects )
        {
            File pomFile = project.getFile();

            File expectedFile = new File( pomFile.getParentFile(), expectedPomFilename );

            assertTrue( "Check if expected file exists.", expectedFile.exists() );

            String pomContents = ReleaseUtil.readXmlFile( pomFile );

            String expectedContents = ReleaseUtil.readXmlFile( expectedFile );

            assertTrue( "Check if pom and backup files are identical", pomContents.equals( expectedContents ) );
        }
    }
}
