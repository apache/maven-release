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
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
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
    @Mock
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    @Mock
    private Prompter mockPrompter;

    private Map<String, VersionPolicy> versionPolicies;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        versionPolicies = lookupMap(VersionPolicy.class);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        verifyNoMoreInteractions(mockPrompter);
    }

    @Test
    public void testExecuteSnapshot_MapRelease() throws Exception {
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
                "Check mapped versions",
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));

        verify(mockPrompter)
                .prompt(startsWith("What is the release version for \"" + project.getName() + "\"?"), eq("1.0"));
    }

    @Test
    public void testSimulateSnapshot_MapReleaseVersions() throws Exception {
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
                "Check mapped versions",
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
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
                "Check mapped versions",
                "1.0.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));

        // prepare
        builder = new ReleaseDescriptorBuilder();

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "Check mapped versions",
                "1.0.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
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
                "Check mapped versions",
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));

        // prepare
        builder = new ReleaseDescriptorBuilder();

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "Check mapped versions",
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));

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
                "Check mapped versions",
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));

        // prepare
        builder = new ReleaseDescriptorBuilder();
        builder.addReleaseVersion("groupId:artifactId", "2.0");

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "Check mapped versions",
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
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
                "Check mapped versions",
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:rootArtifactId"));
        assertEquals(
                "Check mapped versions",
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "Check mapped versions",
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:rootArtifactId"));
        assertEquals(
                "Check mapped versions",
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
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
                "Check mapped versions",
                "1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:rootArtifactId"));
        assertEquals(
                "Check mapped versions",
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "Check mapped versions",
                "1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:rootArtifactId"));
        assertEquals(
                "Check mapped versions",
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotNonInteractive_MapRelease() throws Exception {
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
                "Check mapped versions",
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotNonInteractive_MapReleaseVersions() throws Exception {
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
                "Check mapped versions",
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
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
                "Check mapped versions",
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));

        // prepare
        builder = new ReleaseDescriptorBuilder();

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "Check mapped versions",
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));

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
                "Check mapped versions",
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));

        // prepare
        builder = new ReleaseDescriptorBuilder();
        builder.setUpdateWorkingCopyVersions(false);

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "Check mapped versions",
                "1.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
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
                "Check mapped versions",
                "1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));

        // prepare
        builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "Check mapped versions",
                "1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
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
                "Check mapped versions",
                "2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));

        // prepare
        builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);
        builder.addDevelopmentVersion("groupId:artifactId", "2-SNAPSHOT");

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "Check mapped versions",
                "2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
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
            assertEquals("check cause", PrompterException.class, e.getCause().getClass());
        }

        // prepare
        builder = new ReleaseDescriptorBuilder();

        // execute
        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Expected an exception");
        } catch (ReleaseExecutionException e) {
            assertEquals("check cause", PrompterException.class, e.getCause().getClass());
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
                "Check mapped versions",
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));

        // prepare
        builder = new ReleaseDescriptorBuilder();

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "Check mapped versions",
                "2.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));

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
                    "check cause", VersionParseException.class, e.getCause().getClass());
        }

        builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Expected an exception");
        } catch (ReleaseExecutionException e) {
            assertEquals(
                    "check cause", VersionParseException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testExecuteSnapshotBranchCreation_DefaultDevelopmentVersion_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotBranchCreation_DefaultDevelopmentVersion_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotBranchCreation_DefaultDevelopmentVersion_NonInteractive_MapDevelopment()
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotBranchCreation_DefaultDevelopmentVersion_NonInteractive_MapDevelopment()
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotBranchCreation_NonInteractive_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotBranchCreation_NonInteractive_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotDefaultDevelopmentVersion_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotDefaultDevelopmentVersion_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotDefaultDevelopmentVersion_NonInteractive_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotDefaultDevelopmentVersion_NonInteractive_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.1.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotNonInteractive_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotNonInteractive_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodules_NotInteractive_MapDevelopment() throws Exception {
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
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodules_NotInteractive_MapDevelopment() throws Exception {
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
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteReleaseAutoVersionSubmodules_NotInteractive_MapDevelopment() throws Exception {
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
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateReleaseAutoVersionSubmodules_NotInteractive_MapDevelopment() throws Exception {
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
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodules_NotInteractive_MapRelease() throws Exception {
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
                "Check release versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodules_NotInteractive_MapRelease() throws Exception {
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
                "Check release versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteReleaseAutoVersionSubmodules_NotInteractive_MapRelease() throws Exception {
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
                "Check release versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateReleaseAutoVersionSubmodules_NotInteractive_MapRelease() throws Exception {
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
                "Check release versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment()
            throws Exception {
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
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment()
            throws Exception {
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
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment()
            throws Exception {
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
                "Check development versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapDevelopment()
            throws Exception {
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
                "Check development versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch() throws Exception {
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
                "Check release versions",
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch() throws Exception {
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
                "Check release versions",
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch() throws Exception {
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
                "Check release versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateReleaseAutoVersionSubmodules_BranchCreation_NotInteractive_MapBranch() throws Exception {
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
                "Check release versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotBranchCreation_NonInteractive_UpdateBranchVersions_MapBranch() throws Exception {
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
                "Check release versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotBranchCreation_NonInteractive_UpdateBranchVersions_MapBranch() throws Exception {
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
                "Check release versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotBranchCreation_DefaultReleaseVersion_NonInteractive_UpdateBranchVersions_MapBranch()
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
                "Check release versions",
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotBranchCreation_DefaultReleaseVersion_NonInteractive_UpdateBranchVersions_MapBranch()
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
                "Check release versions",
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotBranchCreation_DefaultReleaseVersion_UpdateBranchVersions_MapBranch()
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
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "Check release versions",
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotBranchCreation_DefaultReleaseVersion_UpdateBranchVersions_MapBranch()
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
                "Check release versions",
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotBranchCreation_UpdateBranchVersions_MapBranch() throws Exception {
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
    public void testSimulateSnapshotBranchCreation_UpdateBranchVersions_MapBranch() throws Exception {
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
    public void testExecuteReleaseBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch()
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
                "Check release versions",
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateReleaseBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch()
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
                "Check release versions",
                "2.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch()
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
    public void testSimulateSnapshotBranchCreation_UpdateBranchVersions_UpdateVersionsToSnapshot_MapBranch()
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
    public void testExecuteReleaseBranchCreation_MapBranch() throws Exception {
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
                "Check release versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateReleaseBranchCreation_MapBranch() throws Exception {
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
                "Check release versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteReleaseBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateReleaseBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteReleaseBranchCreation_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateReleaseBranchCreation_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotBranchCreation_MapBranch() throws Exception {
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
                "Check release versions",
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotBranchCreation_MapBranch() throws Exception {
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
                "Check release versions",
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotBranchCreation_NonUpdateWorkingCopyVersions_MapDevelopment() throws Exception {
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
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "1.2-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteReleaseBranchCreation_UpdateBranchVersions_MapBranch() throws Exception {
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
                "Check release versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateReleaseBranchCreation_UpdateBranchVersions_MapBranch() throws Exception {
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
                "Check release versions",
                "1.2",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check development versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotBranchCreation_UpdateWorkingCopyVersions_MapDevelopment() throws Exception {
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
    public void testSimulateSnapshotBranchCreation_UpdateWorkingCopyVersions_MapDevelopment() throws Exception {
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
    public void testExecuteMultiModuleAutoVersionSubmodules__MapDevelopment() throws Exception {
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
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:module1"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:module1"));
    }

    @Test
    public void testSimulateMultiModuleAutoVersionSubmodules__MapDevelopment() throws Exception {
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
                "Check development versions",
                "1.3-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertEquals(
                "Check development versions",
                "2.0",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:module1"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:module1"));
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodules_DefaultReleaseVersion_NonInteractive_MapDevelopment()
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
                "Check development versions",
                "3.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodules_DefaultReleaseVersion_NonInteractive_MapDevelopment()
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
                "Check development versions",
                "3.1-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testExecuteSnapshotAutoVersionSubmodules_DefaultDevelopmentVersion_NonInteractive_MapDevelopment()
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
                "Check development versions",
                "3.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
    }

    @Test
    public void testSimulateSnapshotAutoVersionSubmodules_DefaultDevelopmentVersion_NonInteractive_MapDevelopment()
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
                "Check development versions",
                "3.0-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertNull(
                "Check release versions",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
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
                "Check development versions",
                "MYB_200909-SNAPSHOT",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
        assertEquals(
                "Check release versions",
                "PPX",
                ReleaseUtils.buildReleaseDescriptor(builder).getProjectReleaseVersion("groupId:artifactId"));
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
    public void testUpdateBranchInvalidDefaultReleaseVersion_NonInteractive() {
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
    public void testUpdateReleaseInvalidDefaultReleaseVersion_NonInteractive() {
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
    public void testUpdateDevelopmentInvalidDefaultDevelopmentVersion_NonInteractive() {
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

    private static MavenProject createProject(String artifactId, String version) {
        Model model = new Model();
        model.setGroupId("groupId");
        model.setArtifactId(artifactId);
        model.setVersion(version);
        return new MavenProject(model);
    }
}
