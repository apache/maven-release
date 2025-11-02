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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.PlexusJUnit4TestCase;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the version mapping phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class MapVersionsPhaseTest extends PlexusJUnit4TestCase {
    private AutoCloseable mocks;
    @Mock
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    @Mock
    private Prompter mockPrompter;

    private Map<String, VersionPolicy> versionPolicies;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mocks = MockitoAnnotations.openMocks(this);
        versionPolicies = lookupMap(VersionPolicy.class);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        verifyNoMoreInteractions(mockPrompter);
        mocks.close();
    }

    @Test
    public void testExecuteSnapshotMapRelease() throws Exception {
        // prepare
        MavenProject project = createProject("artifactId", "1.0-SNAPSHOT");
        when(mockPrompter.prompt(
                startsWith("What is the release version for \"" + project.getName() + "\"?"), eq("1.0")))
                .thenReturn("2.0");
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(project);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");

        verify(mockPrompter)
                .prompt(startsWith("What is the release version for \"" + project.getName() + "\"?"), eq("1.0"));
    }

    @Test
    public void testSimulateSnapshotMapReleaseVersions() throws Exception {
        // prepare
        MavenProject project = createProject("artifactId", "1.0-SNAPSHOT");
        when(mockPrompter.prompt(
                startsWith("What is the release version for \"" + project.getName() + "\"?"), eq("1.0")))
                .thenReturn("2.0");
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(project);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");
        verify(mockPrompter)
                .prompt(startsWith("What is the release version for \"" + project.getName() + "\"?"), eq("1.0"));
    }

    // MRELEASE-403: Release plugin ignores given version number
    @Test
    public void testMapReleaseVersionsInteractiveAddZeroIncremental() throws Exception {
        // prepare
        MavenProject project = createProject("artifactId", "1.0-SNAPSHOT");
        when(mockPrompter.prompt(
                startsWith("What is the release version for \"" + project.getName() + "\"?"), eq("1.0")))
                .thenReturn("1.0.0");
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(project);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.0.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");

        // prepare
        builder = new ReleaseDescriptorBuilder();

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.0.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");
        verify(mockPrompter, times(2))
                .prompt(startsWith("What is the release version for \"" + project.getName() + "\"?"), eq("1.0"));
    }

    /**
     * Test to release "SNAPSHOT" version MRELEASE-90
     */
    @Test
    public void testMapReleaseVersionsInteractiveWithSnaphotVersion() throws Exception {
        // prepare
        MavenProject project = createProject("artifactId", "SNAPSHOT");
        when(mockPrompter.prompt(
                startsWith("What is the release version for \"" + project.getName() + "\"?"), eq("1.0")))
                .thenReturn("2.0");
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(project);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");

        // prepare
        builder = new ReleaseDescriptorBuilder();

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");

        verify(mockPrompter, times(2))
                .prompt(startsWith("What is the release version for \"" + project.getName() + "\"?"), eq("1.0"));
    }

    /**
     * MRELEASE-524: ignores commandline versions in batch mode
     */
    @Test
    public void testMapReleaseVersionsNonInteractiveWithExplicitVersion() throws Exception {
        // prepare
        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "SNAPSHOT"));

        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.addReleaseVersion("groupId:artifactId", "2.0");

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");

        // prepare
        builder = new ReleaseDescriptorBuilder();
        builder.addReleaseVersion("groupId:artifactId", "2.0");

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");
    }

    /**
     * MRELEASE-1022: don't ignore command line (or release.properties) versions when auto-versioning sub-modules
     */
    @Test
    public void testMapReleaseVersionsForSubModuleWithExplicitVersion() throws Exception {
        // prepare
        MavenProject rootProject = createProject("rootArtifactId", "SNAPSHOT");
        rootProject.setExecutionRoot(true);

        final MavenProject moduleProject = createProject("artifactId", "SNAPSHOT");
        moduleProject.setParent(rootProject);

        List<MavenProject> reactorProjects = Arrays.asList(rootProject, moduleProject);

        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder()
                .setInteractive(false) // batch mode
                .setAutoVersionSubmodules(true)
                .addReleaseVersion("groupId:artifactId", "2.0");

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:rootArtifactId"),
                "Check mapped versions");
        assertEquals(
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:rootArtifactId"),
                "Check mapped versions");
        assertEquals(
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");
    }

    /**
     * MRELEASE-1022: don't ignore command line (or release.properties) versions when auto-versioning sub-modules
     */
    @Test
    public void testMapDevelopmentVersionsForSubModuleWithExplicitVersion() throws Exception {
        // prepare
        MavenProject rootProject = createProject("rootArtifactId", "1.0");
        rootProject.setExecutionRoot(true);

        final MavenProject moduleProject = createProject("artifactId", "1.0");
        moduleProject.setParent(rootProject);

        List<MavenProject> reactorProjects = Arrays.asList(rootProject, moduleProject);

        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder()
                .setInteractive(false) // batch mode
                .setAutoVersionSubmodules(true)
                .setDefaultDevelopmentVersion("1.1-SNAPSHOT")
                .addDevelopmentVersion("groupId:artifactId", "2.0-SNAPSHOT");

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:rootArtifactId"),
                "Check mapped versions");
        assertEquals(
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:rootArtifactId"),
                "Check mapped versions");
        assertEquals(
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");
    }

    @Test
    public void testExecuteSnapshotNonInteractiveMapRelease() throws Exception {
        // prepare
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.0-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");
    }

    @Test
    public void testSimulateSnapshotNonInteractiveMapReleaseVersions() throws Exception {
        // prepare
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.0-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check mapped versions");
    }

    @Test
    public void testMapDevVersionsInteractive() throws Exception {
        // prepare
        MavenProject project = createProject("artifactId", "1.0");
        when(mockPrompter.prompt(
                startsWith("What is the new development version for \"" + project.getName() + "\"?"),
                eq("1.1-SNAPSHOT")))
                .thenReturn("2.0-SNAPSHOT");
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(project);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");

        // prepare
        builder = new ReleaseDescriptorBuilder();

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");

        verify(mockPrompter, times(2))
                .prompt(
                        startsWith("What is the new development version for \"" + project.getName() + "\"?"),
                        eq("1.1-SNAPSHOT"));
    }

    /**
     * MRELEASE-760: updateWorkingCopyVersions=false still bumps up pom versions to next development version
     */
    @Test
    public void testMapDevVersionsInteractiveDoNotUpdateWorkingCopy() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);
        MavenProject project = createProject("artifactId", "1.0");

        List<MavenProject> reactorProjects = Collections.singletonList(project);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setUpdateWorkingCopyVersions(false);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");

        // prepare
        builder = new ReleaseDescriptorBuilder();
        builder.setUpdateWorkingCopyVersions(false);

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");
    }

    @Test
    public void testMapDevVersionsNonInteractive() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.0"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");

        // prepare
        builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");
    }

    /**
     * MRELEASE-524: ignores commandline versions in batch mode
     */
    @Test
    public void testMapDevVersionsNonInteractiveWithExplicitVersion() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);
        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.0"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);
        builder.addDevelopmentVersion("groupId:artifactId", "2-SNAPSHOT");

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");

        // prepare
        builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);
        builder.addDevelopmentVersion("groupId:artifactId", "2-SNAPSHOT");

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");
    }

    @Test
    public void testPrompterException() throws Exception {
        // prepare
        when(mockPrompter.prompt(isA(String.class), isA(String.class))).thenThrow(new PrompterException("..."));
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.0"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        // execute
        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Expected an exception");
        } catch (ReleaseExecutionException e) {
            assertEquals(PrompterException.class, e.getCause().getClass(), "check cause");
        }

        // prepare
        builder = new ReleaseDescriptorBuilder();

        // execute
        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Expected an exception");
        } catch (ReleaseExecutionException e) {
            assertEquals(PrompterException.class, e.getCause().getClass(), "check cause");
        }

        // verify
        verify(mockPrompter, times(2)).prompt(isA(String.class), isA(String.class));
    }

    @Test
    public void testAdjustVersionInteractive() throws Exception {
        // prepare
        MavenProject project = createProject("artifactId", "foo");

        when(mockPrompter.prompt(
                startsWith("What is the new development version for \"" + project.getName() + "\"?"),
                eq("1.1-SNAPSHOT")))
                .thenReturn("2.0-SNAPSHOT");
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(project);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");

        // prepare
        builder = new ReleaseDescriptorBuilder();

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check mapped versions");

        verify(mockPrompter, times(2))
                .prompt(
                        startsWith("What is the new development version for \"" + project.getName() + "\"?"),
                        eq("1.1-SNAPSHOT"));
    }

    @Test
    public void testAdjustVersionNonInteractive() {
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "foo"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Expected an exception");
        } catch (ReleaseExecutionException e) {
            assertEquals(
                    VersionParseException.class, e.getCause().getClass(), "check cause");
        }

        builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Expected an exception");
        } catch (ReleaseExecutionException e) {
            assertEquals(
                    VersionParseException.class, e.getCause().getClass(), "check cause");
        }
    }

    @Test
    public void testExecuteSnapshotBranchCreationDefaultDevelopmentVersionMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setDefaultDevelopmentVersion("1.1.1-SNAPSHOT");

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotBranchCreationDefaultDevelopmentVersionMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setDefaultDevelopmentVersion("1.1.1-SNAPSHOT");

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotBranchCreationDefaultDevelopmentVersionNonInteractiveMapDevelopment()
            throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setDefaultDevelopmentVersion("1.1.1-SNAPSHOT");
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotBranchCreationDefaultDevelopmentVersionNonInteractiveMapDevelopment()
            throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setDefaultDevelopmentVersion("1.1.1-SNAPSHOT");
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotBranchCreationNonInteractiveMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotBranchCreationNonInteractiveMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotDefaultDevelopmentVersionMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setDefaultDevelopmentVersion("1.1.1-SNAPSHOT");

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotDefaultDevelopmentVersionMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setDefaultDevelopmentVersion("1.1.1-SNAPSHOT");

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotDefaultDevelopmentVersionNonInteractiveMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setDefaultDevelopmentVersion("1.1.1-SNAPSHOT");
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotDefaultDevelopmentVersionNonInteractiveMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setDefaultDevelopmentVersion("1.1.1-SNAPSHOT");
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotNonInteractiveMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotNonInteractiveMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodulesNotInteractiveMapDevelopment() throws Exception {
        // verify
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodulesNotInteractiveMapDevelopment() throws Exception {
        // verify
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    @Test
    public void testExecuteReleaseAutoVersionSubmodulesNotInteractiveMapDevelopment() throws Exception {
        // verify
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    @Test
    public void testSimulateReleaseAutoVersionSubmodulesNotInteractiveMapDevelopment() throws Exception {
        // verify
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodulesNotInteractiveMapRelease() throws Exception {
        // verify
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodulesNotInteractiveMapRelease() throws Exception {
        // verify
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteReleaseAutoVersionSubmodulesNotInteractiveMapRelease() throws Exception {
        // verify
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateReleaseAutoVersionSubmodulesNotInteractiveMapRelease() throws Exception {
        // verify
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodulesBranchCreationNotInteractiveMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setBranchCreation(true);
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodulesBranchCreationNotInteractiveMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setBranchCreation(true);
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    @Test
    public void testExecuteReleaseAutoVersionSubmodulesBranchCreationNotInteractiveMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setBranchCreation(true);
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    @Test
    public void testSimulateReleaseAutoVersionSubmodulesBranchCreationNotInteractiveMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setBranchCreation(true);
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodulesBranchCreationNotInteractiveMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setBranchCreation(true);
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodulesBranchCreationNotInteractiveMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setBranchCreation(true);
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteReleaseAutoVersionSubmodulesBranchCreationNotInteractiveMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setBranchCreation(true);
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        /*
         * "By default, the POM in the new branch keeps the same version as the local working copy, and the local POM is incremented to the next revision."
         * This is true for trunk, but when branching from a tag I would expect the next SNAPSHOT version. For now keep
         * '1.2' instead of '1.3-SNAPSHOT' until further investigation.
         */
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateReleaseAutoVersionSubmodulesBranchCreationNotInteractiveMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setBranchCreation(true);
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        /*
         * "By default, the POM in the new branch keeps the same version as the local working copy, and the local POM is incremented to the next revision."
         * This is true for trunk, but when branching from a tag I would expect the next SNAPSHOT version. For now keep
         * '1.2' instead of '1.3-SNAPSHOT' until further investigation.
         */
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotBranchCreationNonInteractiveUpdateBranchVersionsMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setInteractive(false);
        builder.setUpdateBranchVersions(true);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        // updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotBranchCreationNonInteractiveUpdateBranchVersionsMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setInteractive(false);
        builder.setUpdateBranchVersions(true);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        // updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotBranchCreationDefaultReleaseVersionNonInteractiveUpdateBranchVersionsMapBranch()
            throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setDefaultReleaseVersion("2.1-SNAPSHOT");
        builder.setInteractive(false);
        builder.setUpdateBranchVersions(true);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotBranchCreationDefaultReleaseVersionNonInteractiveUpdateBranchVersionsMapBranch()
            throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setDefaultReleaseVersion("2.1-SNAPSHOT");
        builder.setInteractive(false);
        builder.setUpdateBranchVersions(true);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotBranchCreationDefaultReleaseVersionUpdateBranchVersionsMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setDefaultReleaseVersion("2.1-SNAPSHOT");
        builder.setUpdateBranchVersions(true);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotBranchCreationDefaultReleaseVersionUpdateBranchVersionsMapBranch()
            throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setDefaultReleaseVersion("2.1-SNAPSHOT");
        builder.setUpdateBranchVersions(true);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotBranchCreationUpdateBranchVersionsMapBranch() throws Exception {
        // prepare
        // updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when(mockPrompter.prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT")))
                .thenReturn("2.0-SNAPSHOT");
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateBranchVersions(true);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify(mockPrompter).prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT"));
    }

    @Test
    public void testSimulateSnapshotBranchCreationUpdateBranchVersionsMapBranch() throws Exception {
        // prepare
        // updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when(mockPrompter.prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT")))
                .thenReturn("2.0-SNAPSHOT");
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateBranchVersions(true);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify(mockPrompter).prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT"));
    }

    @Test
    public void testExecuteReleaseBranchCreationUpdateBranchVersionsUpdateVersionsToSnapshotMapBranch()
            throws Exception {
        // prepare
        // updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT (yes, one step back!)
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when(mockPrompter.prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT")))
                .thenReturn("2.1-SNAPSHOT");
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateBranchVersions(true);
        builder.setUpdateVersionsToSnapshot(true);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT (yes, one step back!)
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify(mockPrompter).prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT"));
        assertEquals(
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateReleaseBranchCreationUpdateBranchVersionsUpdateVersionsToSnapshotMapBranch()
            throws Exception {
        // prepare
        // updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT (yes, one step back!)
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when(mockPrompter.prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT")))
                .thenReturn("2.1-SNAPSHOT");
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateBranchVersions(true);
        builder.setUpdateVersionsToSnapshot(true);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT (yes, one step back!)
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify(mockPrompter).prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT"));
        assertEquals(
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotBranchCreationUpdateBranchVersionsUpdateVersionsToSnapshotMapBranch()
            throws Exception {
        // prepare
        // updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when(mockPrompter.prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT")))
                .thenReturn("2.0-SNAPSHOT");
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateBranchVersions(true);
        builder.setUpdateVersionsToSnapshot(true);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify(mockPrompter).prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT"));
    }

    @Test
    public void testSimulateSnapshotBranchCreationUpdateBranchVersionsUpdateVersionsToSnapshotMapBranch()
            throws Exception {
        // prepare
        // updateBranchVersions is set to true, so suggest the next snapshot version
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        when(mockPrompter.prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT")))
                .thenReturn("2.0-SNAPSHOT");
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateBranchVersions(true);
        builder.setUpdateVersionsToSnapshot(true);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        // org.apache.maven.release:maven-release-manager:(,2.4) > 1.2-SNAPSHOT
        // org.apache.maven.release:maven-release-manager:[2.4,) > 1.3-SNAPSHOT
        verify(mockPrompter).prompt(startsWith("What is the branch version for"), eq("1.3-SNAPSHOT"));
    }

    @Test
    public void testExecuteReleaseBranchCreationMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        /*
         * "By default, the POM in the new branch keeps the same version as the local working copy, and the local POM is incremented to the next revision."
         * This is true for trunk, but when branching from a tag I would expect the next SNAPSHOT version. For now keep
         * '1.2' instead of '1.3-SNAPSHOT' until further investigation.
         */
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateReleaseBranchCreationMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        /*
         * "By default, the POM in the new branch keeps the same version as the local working copy, and the local POM is incremented to the next revision."
         * This is true for trunk, but when branching from a tag I would expect the next SNAPSHOT version. For now keep
         * '1.2' instead of '1.3-SNAPSHOT' until further investigation.
         */
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteReleaseBranchCreationNonUpdateWorkingCopyVersionsMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateWorkingCopyVersions(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateReleaseBranchCreationNonUpdateWorkingCopyVersionsMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateWorkingCopyVersions(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteReleaseBranchCreationMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        // org.apache.maven.release:maven-release-manager:(,2.4) > true
        // org.apache.maven.release:maven-release-manager:[2.4,) > false
        builder.setInteractive(false);
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateReleaseBranchCreationMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        // org.apache.maven.release:maven-release-manager:(,2.4) > true
        // org.apache.maven.release:maven-release-manager:[2.4,) > false
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotBranchCreationMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotBranchCreationMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotBranchCreationNonUpdateWorkingCopyVersionsMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateWorkingCopyVersions(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateSnapshotBranchCreationNonUpdateWorkingCopyVersionsMapDevelopment() throws Exception {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateWorkingCopyVersions(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertEquals(
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteReleaseBranchCreationUpdateBranchVersionsMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateBranchVersions(true);
        // org.apache.maven.release:maven-release-manager:(,2.4) > true
        // org.apache.maven.release:maven-release-manager:[2.4,) > false
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testSimulateReleaseBranchCreationUpdateBranchVersionsMapBranch() throws Exception {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateBranchVersions(true);
        // org.apache.maven.release:maven-release-manager:(,2.4) > true
        // org.apache.maven.release:maven-release-manager:[2.4,) > false
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
    }

    @Test
    public void testExecuteSnapshotBranchCreationUpdateWorkingCopyVersionsMapDevelopment() throws Exception {
        // prepare
        when(mockPrompter.prompt(startsWith("What is the new working copy version for"), eq("1.3-SNAPSHOT")))
                .thenReturn("2.0-SNAPSHOT");
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateWorkingCopyVersions(true);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(mockPrompter).prompt(startsWith("What is the new working copy version for"), eq("1.3-SNAPSHOT"));
    }

    @Test
    public void testSimulateSnapshotBranchCreationUpdateWorkingCopyVersionsMapDevelopment() throws Exception {
        // prepare
        when(mockPrompter.prompt(startsWith("What is the new working copy version for"), eq("1.3-SNAPSHOT")))
                .thenReturn("2.0-SNAPSHOT");
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setBranchCreation(true);
        builder.setUpdateWorkingCopyVersions(true);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(mockPrompter).prompt(startsWith("What is the new working copy version for"), eq("1.3-SNAPSHOT"));
    }

    @Test
    public void testExecuteMultiModuleAutoVersionSubmodulesMapDevelopment() throws Exception {
        // verify
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = new ArrayList<>();
        Collections.addAll(
                reactorProjects, createProject("artifactId", "1.2-SNAPSHOT"), createProject("module1", "2.0"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertEquals(
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:module1"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:module1"),
                "Check release versions");
    }

    @Test
    public void testSimulateMultiModuleAutoVersionSubmodulesMapDevelopment() throws Exception {
        // verify
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = new ArrayList<>();
        Collections.addAll(
                reactorProjects, createProject("artifactId", "1.2-SNAPSHOT"), createProject("module1", "2.0"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertEquals(
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:module1"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:module1"),
                "Check release versions");
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodulesDefaultReleaseVersionNonInteractiveMapDevelopment()
            throws Exception {
        // verify
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2.1-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setDefaultReleaseVersion("3.0");
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "3.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodulesDefaultReleaseVersionNonInteractiveMapDevelopment()
            throws Exception {
        // verify
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2.1-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setDefaultReleaseVersion("3.0");
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "3.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodulesDefaultDevelopmentVersionNonInteractiveMapDevelopment()
            throws Exception {
        // verify
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2.1-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setDefaultDevelopmentVersion("3.0-SNAPSHOT");
        builder.setInteractive(false);

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "3.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodulesDefaultDevelopmentVersionNonInteractiveMapDevelopment()
            throws Exception {
        // verify
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.2.1-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setAutoVersionSubmodules(true);
        builder.setDefaultDevelopmentVersion("3.0-SNAPSHOT");
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "3.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    // MRELEASE-511
    @Test
    public void testUnusualVersions1() throws Exception {
        MapReleaseVersionsPhase mapReleasephase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);
        MapDevelopmentVersionsPhase mapDevelopmentphase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects =
                Collections.singletonList(createProject("artifactId", "MYB_200909-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setDefaultReleaseVersion("PPX");
        builder.setDefaultDevelopmentVersion("MYB_200909-SNAPSHOT");

        // test
        mapReleasephase.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);
        mapDevelopmentphase.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "MYB_200909-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");
        assertEquals(
                "PPX",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
    }

    // MRELEASE-269
    @Test
    public void testContinuousSnapshotCheck() throws Exception {
        // prepare
        when(mockPrompter.prompt(startsWith("What is the new development version for "), eq("1.12-SNAPSHOT")))
                .thenReturn("2.0") // wrong, expected SNAPSHOT
                .thenReturn("2.0-SNAPSHOT");
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("bar", "1.11-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(mockPrompter, times(2))
                .prompt(startsWith("What is the new development version for "), eq("1.12-SNAPSHOT"));
    }

    // MRELEASE-734
    @Test
    public void testEmptyDefaultDevelopmentVersion() throws Exception {
        // prepare
        when(mockPrompter.prompt(startsWith("What is the new development version for "), eq("1.12-SNAPSHOT")))
                .thenReturn("2.0-SNAPSHOT");
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("bar", "1.11-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setDefaultDevelopmentVersion("");

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(mockPrompter).prompt(startsWith("What is the new development version for "), eq("1.12-SNAPSHOT"));
    }

    @Test
    public void testEmptyDefaultReleaseVersion() throws Exception {
        // prepare
        when(mockPrompter.prompt(startsWith("What is the release version for "), eq("1.11")))
                .thenReturn("2.0");
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("bar", "1.11-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setDefaultReleaseVersion("");

        // test
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(mockPrompter).prompt(startsWith("What is the release version for "), eq("1.11"));
    }

    /**
     * MRELEASE-975: Test that a PolicyException is thrown when using an unknown policy version hint.
     */
    @Test
    public void testNonExistentVersionPolicy() {
        // prepare
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("artifactId", "1.0-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setProjectVersionPolicyId("UNKNOWN");

        // test
        ReleaseExecutionException e = assertThrows(
                ReleaseExecutionException.class,
                () -> phase.execute(
                        ReleaseUtils.buildReleaseDescriptor(builder),
                        new DefaultReleaseEnvironment(),
                        reactorProjects));
        assertThat(e.getCause(), CoreMatchers.instanceOf(PolicyException.class));
    }

    @Test
    public void testUpdateBranchInvalidDefaultReleaseVersionNonInteractive() {
        // prepare
        MapBranchVersionsPhase phase =
                new MapBranchVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("bar", "1.11-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setDefaultReleaseVersion("3.0");
        builder.setInteractive(false);
        builder.setUpdateBranchVersions(true);

        // test
        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);
            fail("Should fail due to invalid version");
        } catch (ReleaseExecutionException e) {
            assertEquals("3.0 is invalid, expected a snapshot", e.getMessage());
        }
    }

    @Test
    public void testUpdateReleaseInvalidDefaultReleaseVersionNonInteractive() {
        // prepare
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("bar", "1.11-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setDefaultReleaseVersion("3.0-SNAPSHOT");
        builder.setInteractive(false);

        // test
        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);
            fail("Should fail due to invalid version");
        } catch (ReleaseExecutionException e) {
            assertEquals("3.0-SNAPSHOT is invalid, expected a non-snapshot", e.getMessage());
        }
    }

    @Test
    public void testUpdateDevelopmentInvalidDefaultDevelopmentVersionNonInteractive() {
        // prepare
        MapDevelopmentVersionsPhase phase =
                new MapDevelopmentVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = Collections.singletonList(createProject("bar", "1.11-SNAPSHOT"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setDefaultDevelopmentVersion("3.0");
        builder.setInteractive(false);

        // test
        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);
            fail("Should fail due to invalid version");
        } catch (ReleaseExecutionException e) {
            assertEquals("3.0 is invalid, expected a snapshot", e.getMessage());
        }
    }

    @Test
    public void testSimulateReleaseCheckModificationExcludes() throws Exception {
        // verify
        MapReleaseVersionsPhase phase =
                new MapReleaseVersionsPhase(scmRepositoryConfigurator, mockPrompter, versionPolicies);

        List<MavenProject> reactorProjects = new ArrayList<>();
        Collections.addAll(
                reactorProjects,
                createProject("bar", "1.11-SNAPSHOT"),
                createProjectWithPomFile(
                        "artifactId", "1.2-SNAPSHOT", "src/test/resources/projects/scm-commit/multiple-poms/pom.xml"),
                createProjectWithPomFile(
                        "subproject1",
                        "2.0",
                        "src/test/resources/projects/scm-commit/multiple-poms/subproject1/pom.xml"));

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setCheckModificationExcludes(Collections.singletonList("**/subproject1/pom.xml"));
        builder.setInteractive(false);

        // test
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"),
                "Check development versions");

        assertEquals(
                "1.11",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:bar"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:bar"),
                "Check development versions");

        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:subproject1"),
                "Check release versions");
        assertNull(
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:subproject1"),
                "Check development versions");
    }

    private static MavenProject createProject(String artifactId, String version) {
        Model model = new Model();
        model.setGroupId("groupId");
        model.setArtifactId(artifactId);
        model.setVersion(version);
        MavenProject mavenProject = new MavenProject(model);
        mavenProject.setOriginalModel(model);
        return mavenProject;
    }

    private static MavenProject createProjectWithPomFile(String artifactId, String version, String pathName) {
        Model model = new Model();
        model.setGroupId("groupId");
        model.setArtifactId(artifactId);
        model.setVersion(version);
        MavenProject mavenProject = new MavenProject(model);
        mavenProject.setFile(new File(pathName));
        return mavenProject;
    }
}
