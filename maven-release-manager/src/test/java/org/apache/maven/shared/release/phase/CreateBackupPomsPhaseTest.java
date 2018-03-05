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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.junit.Test;

/**
 * @author Edwin Punzalan
 */
public class CreateBackupPomsPhaseTest
    extends AbstractBackupPomsPhaseTest
{
    @Override
    ReleasePhase getReleasePhase()
        throws Exception
    {
        return (ReleasePhase) lookup( ReleasePhase.class, "create-backup-poms" );
    }

    @Test
    public void testBasicPom()
        throws Exception
    {
        String projectPath = "target/test-classes/projects/create-backup-poms/basic-pom";

        // should create backup files
        runExecuteOnProjects( projectPath );

        // should delete backup files
        runCleanOnProjects( projectPath );

        // should re-create backup files
        runSimulateOnProjects( projectPath );
    }

    @Test
    public void testMultiModulePom()
        throws Exception
    {
        String projectPath = "target/test-classes/projects/create-backup-poms/pom-with-modules";

        // should create backup files
        runExecuteOnProjects( projectPath );

        // should delete backup files
        runCleanOnProjects( projectPath );

        // should re-create backup files
        runSimulateOnProjects( projectPath );
    }

    private void runExecuteOnProjects( String path )
        throws Exception
    {
        List<MavenProject> projects = getReactorProjects( getTestPath( path ) );

        phase.execute( null, new DefaultReleaseEnvironment(), projects );

        testProjectBackups( projects, true );
    }

    private void runSimulateOnProjects( String path )
        throws Exception
    {
        List<MavenProject> projects = getReactorProjects( getTestPath( path ) );

        phase.simulate( null, new DefaultReleaseEnvironment(), projects );

        testProjectBackups( projects, true );
    }

    private void runCleanOnProjects( String path )
        throws Exception
    {
        List<MavenProject> projects = getReactorProjects( getTestPath( path ) );

        ( (ResourceGenerator) phase ).clean( projects );

        testProjectBackups( projects, false );
    }

    protected void testProjectBackups( List<MavenProject> reactorProjects, boolean created )
        throws Exception
    {
        for( Iterator<MavenProject> projects = reactorProjects.iterator(); projects.hasNext(); )
        {
            MavenProject project = projects.next();

            File pomFile = project.getFile();

            File backupFile = new File( pomFile.getAbsolutePath() + releaseBackupSuffix );

            if ( created )
            {
                assertTrue( "Check if backup file was created.", backupFile.exists() );

                String pomContents = ReleaseUtil.readXmlFile( pomFile );

                String backupContents = ReleaseUtil.readXmlFile( backupFile );

                assertTrue( "Check if pom and backup files are identical", pomContents.equals( backupContents ) );
            }
            else
            {
                assertFalse( "Check if backup file is not present", backupFile.exists() );
            }
        }
    }
}
