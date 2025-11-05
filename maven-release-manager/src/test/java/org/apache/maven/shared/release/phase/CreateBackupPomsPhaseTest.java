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
package org.apache.maven.shared.release.phase;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestPath;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Edwin Punzalan
 */
@PlexusTest
class CreateBackupPomsPhaseTest extends AbstractBackupPomsPhaseTest {

    @Inject
    @Named("create-backup-poms")
    private ReleasePhase phase;

    @Test
    void testBasicPom() throws Exception {
        String projectPath = "target/test-classes/projects/create-backup-poms/basic-pom";

        // should create backup files
        runExecuteOnProjects(projectPath);

        // should delete backup files
        runCleanOnProjects(projectPath);

        // should re-create backup files
        runSimulateOnProjects(projectPath);
    }

    @Test
    void testMultiModulePom() throws Exception {
        String projectPath = "target/test-classes/projects/create-backup-poms/pom-with-modules";

        // should create backup files
        runExecuteOnProjects(projectPath);

        // should delete backup files
        runCleanOnProjects(projectPath);

        // should re-create backup files
        runSimulateOnProjects(projectPath);
    }

    private void runExecuteOnProjects(String path) throws Exception {
        List<MavenProject> projects = getReactorProjects(getTestPath(path));

        phase.execute(null, new DefaultReleaseEnvironment(), projects);

        testProjectBackups(projects, true);
    }

    private void runSimulateOnProjects(String path) throws Exception {
        List<MavenProject> projects = getReactorProjects(getTestPath(path));

        phase.simulate(null, new DefaultReleaseEnvironment(), projects);

        testProjectBackups(projects, true);
    }

    private void runCleanOnProjects(String path) throws Exception {
        List<MavenProject> projects = getReactorProjects(getTestPath(path));

        ((ResourceGenerator) phase).clean(projects);

        testProjectBackups(projects, false);
    }

    protected void testProjectBackups(List<MavenProject> reactorProjects, boolean created) throws Exception {
        for (Iterator<MavenProject> projects = reactorProjects.iterator(); projects.hasNext(); ) {
            MavenProject project = projects.next();

            File pomFile = project.getFile();

            File backupFile = new File(pomFile.getAbsolutePath() + releaseBackupSuffix);

            if (created) {
                assertTrue(backupFile.exists(), "Check if backup file was created.");

                String pomContents = ReleaseUtil.readXmlFile(pomFile);

                String backupContents = ReleaseUtil.readXmlFile(backupFile);

                assertTrue(pomContents.equals(backupContents), "Check if pom and backup files are identical");
            } else {
                assertFalse(backupFile.exists(), "Check if backup file is not present");
            }
        }
    }
}
