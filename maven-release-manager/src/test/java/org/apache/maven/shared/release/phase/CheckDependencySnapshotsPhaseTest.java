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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the dependency snapshot check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@PlexusTest
class CheckDependencySnapshotsPhaseTest extends AbstractReleaseTestCase {
    private static final String NO = "no";

    private static final String YES = "yes";

    private static final List<String> YES_NO_ARRAY = Arrays.asList(YES, NO);

    private static final String DEFAULT_CHOICE = "1";

    private static final List<String> CHOICE_ARRAY = Arrays.asList("0", DEFAULT_CHOICE, "2", "3");

    @Inject
    private CheckDependencySnapshotsPhase phase;

    @Test
    void testNoSnapshotDependencies() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        List<MavenProject> reactorProjects = createDescriptorFromProjects("no-snapshot-dependencies");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testNoSnapshotRangeDependencies() throws Exception {

        List<MavenProject> reactorProjects = createDescriptorFromProjects("no-snapshot-range-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createMockPrompter(YES, DEFAULT_CHOICE, new VersionPair("1.1", "1.2-SNAPSHOT")));

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.setPrompter(createMockPrompter(YES, DEFAULT_CHOICE, new VersionPair("1.1", "1.2-SNAPSHOT")));

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    // MRELEASE-985
    @Test
    void testSnapshotDependenciesInProjectAndResolveFromCommandLine() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("internal-snapshot-dependencies-no-reactor");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);
        builder.addDependencyReleaseVersion("groupId:test", "1.0");
        builder.addDependencyDevelopmentVersion("groupId:test", "1.1");

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);
            assertTrue(true);
        } catch (ReleaseFailureException e) {
            fail("There should be no failed execution");
        }

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);
            assertTrue(true);
        } catch (ReleaseFailureException e) {
            fail("There should be no failed execution");
        }
    }

    @Test
    void testSnapshotDependenciesInProjectOnly() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("internal-snapshot-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotReleasePluginNonInteractive() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("snapshot-release-plugin");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);
        builder.setInteractive(false);

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotReleasePluginInteractiveDeclined() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("snapshot-release-plugin");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createMockPrompterWithSnapshotReleasePlugin(NO, NO));

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        phase.setPrompter(createMockPrompterWithSnapshotReleasePlugin(NO, NO));

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotReleasePluginInteractiveAcceptedForExecution() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("snapshot-release-plugin");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createYesMockPrompter());

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(true);
    }

    @Test
    void testSnapshotReleasePluginInteractiveAcceptedForSimulation() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("snapshot-release-plugin");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createYesMockPrompter());

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(true);
    }

    @Test
    void testSnapshotReleasePluginInteractiveInvalid() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("snapshot-release-plugin");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createMockPrompterWithSnapshotReleasePlugin("donkey", NO));

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        phase.setPrompter(createMockPrompterWithSnapshotReleasePlugin("donkey", NO));

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotReleasePluginInteractiveException() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("snapshot-release-plugin");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        Prompter mockPrompter = mock(Prompter.class);
        when(mockPrompter.prompt(anyString(), eq(YES_NO_ARRAY), eq(NO))).thenThrow(new PrompterException("..."));
        phase.setPrompter(mockPrompter);

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseExecutionException e) {
            assertEquals(PrompterException.class, e.getCause().getClass(), "Check cause");
        }

        mockPrompter = mock(Prompter.class);
        when(mockPrompter.prompt(anyString(), eq(YES_NO_ARRAY), eq(NO))).thenThrow(new PrompterException("..."));

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseExecutionException e) {
            assertEquals(PrompterException.class, e.getCause().getClass(), "Check cause");
        }
    }

    @Test
    void testSnapshotDependenciesInProjectOnlyMismatchedVersion() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("internal-differing-snapshot-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotManagedDependenciesInProjectOnly() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("internal-managed-snapshot-dependency");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotUnusedInternalManagedDependency() throws Exception {
        List<MavenProject> reactorProjects =
                createDescriptorFromProjects("unused-internal-managed-snapshot-dependency");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotUnusedExternalManagedDependency() throws Exception {
        List<MavenProject> reactorProjects =
                createDescriptorFromProjects("unused-external-managed-snapshot-dependency");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotExternalManagedDependency() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-managed-snapshot-dependency");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        builder.setInteractive(false);

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotDependenciesOutsideProjectOnlyNonInteractive() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-snapshot-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        builder.setInteractive(false);

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testRangeSnapshotDependenciesOutsideProjectOnlyNonInteractive() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-range-snapshot-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        builder.setInteractive(false);

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotDependenciesOutsideProjectOnlyInteractiveWithSnapshotsResolved() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-snapshot-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createMockPrompter(
                YES, DEFAULT_CHOICE, new VersionPair("1.0", "1.1-SNAPSHOT"), new VersionPair("1.0", "1.0")));

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // validate
        ReleaseDescriptor descriptor = ReleaseUtils.buildReleaseDescriptor(builder);

        assertEquals("1.0", descriptor.getDependencyReleaseVersion("external:artifactId"));
        assertEquals("1.1-SNAPSHOT", descriptor.getDependencyDevelopmentVersion("external:artifactId"));

        builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createMockPrompter(YES, DEFAULT_CHOICE, new VersionPair("1.0", "1.1-SNAPSHOT")));

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);
    }

    @Test
    void testSnapshotDependenciesSelectOlderRelease() throws Exception {

        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-snapshot-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createMockPrompter(
                YES, DEFAULT_CHOICE, new VersionPair("0.9", "1.0-SNAPSHOT"), new VersionPair("1.0", "1.0-SNAPSHOT")));

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // validate
        ReleaseDescriptor descriptor = ReleaseUtils.buildReleaseDescriptor(builder);

        assertEquals("0.9", descriptor.getDependencyReleaseVersion("external:artifactId"));
        assertEquals("1.0-SNAPSHOT", descriptor.getDependencyDevelopmentVersion("external:artifactId"));
    }

    @Test
    void testSnapshotDependenciesSelectDefaults() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-snapshot-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createMockPrompter(YES, DEFAULT_CHOICE, new VersionPair("1.0", "1.0")));

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // validate
        ReleaseDescriptor descriptor = ReleaseUtils.buildReleaseDescriptor(builder);

        assertEquals("1.0", descriptor.getDependencyReleaseVersion("external:artifactId"));
        assertEquals("1.0", descriptor.getDependencyDevelopmentVersion("external:artifactId"));
    }

    @Test
    void testSnapshotDependenciesUpdateAllOnlyDependenciesNeeded() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-snapshot-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createMockPrompter(YES, "0", new VersionPair("1.0", "1.0")));

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // validate
        ReleaseDescriptor descriptor = ReleaseUtils.buildReleaseDescriptor(builder);

        assertEquals("1.0", descriptor.getDependencyReleaseVersion("external:artifactId"));
        assertEquals("1.0", descriptor.getDependencyDevelopmentVersion("external:artifactId"));
    }

    @Test
    void testSnapshotDependenciesUpdateAll() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-snapshot-all");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        Prompter mockPrompter = createMockPrompter(
                YES,
                "0",
                Arrays.asList(
                        new VersionPair("1.0", "1.0"), new VersionPair("1.1", "1.1"),
                        new VersionPair("1.2", "1.2"), new VersionPair("1.3", "1.3")));
        phase.setPrompter(mockPrompter);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // validate
        ReleaseDescriptor descriptor = ReleaseUtils.buildReleaseDescriptor(builder);

        assertEquals("1.0", descriptor.getDependencyReleaseVersion("external:artifactId"));
        assertEquals("1.0", descriptor.getDependencyDevelopmentVersion("external:artifactId"));
    }

    // MRELEASE-589
    @Test
    void testSnapshotDependenciesOutsideMultimoduleProjectOnlyInteractiveWithSnapshotsResolved() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("multimodule-external-snapshot-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        VersionPair pair = new VersionPair("1.0", "1.1-SNAPSHOT");
        VersionPair defaultPair = new VersionPair("1.0", "1.0");
        Prompter mockPrompter =
                createMockPrompter("yes", "1", Arrays.asList(pair, pair), Arrays.asList(defaultPair, defaultPair));
        phase.setPrompter(mockPrompter);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        ReleaseDescriptor descriptor = ReleaseUtils.buildReleaseDescriptor(builder);

        assertEquals("1.0", descriptor.getDependencyReleaseVersion("external:artifactId"));
        assertEquals("1.1-SNAPSHOT", descriptor.getDependencyDevelopmentVersion("external:artifactId"));

        assertEquals("1.0", descriptor.getDependencyReleaseVersion("external:artifactId2"));
        assertEquals("1.1-SNAPSHOT", descriptor.getDependencyDevelopmentVersion("external:artifactId2"));
    }

    @Test
    void testSnapshotDependenciesInsideAndOutsideProject() throws Exception {
        List<MavenProject> reactorProjects =
                createDescriptorFromProjects("internal-and-external-snapshot-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testNoSnapshotReportPlugins() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        List<MavenProject> reactorProjects = createDescriptorFromProjects("no-snapshot-report-plugins");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotReportPluginsInProjectOnly() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("internal-snapshot-report-plugins");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotReportPluginsOutsideProjectOnly() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-snapshot-report-plugins");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotReportPluginsInsideAndOutsideProject() throws Exception {
        List<MavenProject> reactorProjects =
                createDescriptorFromProjects("internal-and-external-snapshot-report-plugins");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testNoSnapshotPlugins() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        List<MavenProject> reactorProjects = createDescriptorFromProjects("no-snapshot-plugins");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotPluginsInProjectOnly() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("internal-snapshot-plugins");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotManagedPluginInProjectOnly() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("internal-managed-snapshot-plugin");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotUnusedInternalManagedPlugin() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("unused-internal-managed-snapshot-plugin");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotUnusedExternalManagedPlugin() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("unused-external-managed-snapshot-plugin");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotExternalManagedPlugin() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-managed-snapshot-plugin");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotPluginsOutsideProjectOnly() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-snapshot-plugins");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotPluginsInsideAndOutsideProject() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("internal-and-external-snapshot-plugins");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotExternalParent() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-snapshot-parent/child");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotExternalParentAdjusted() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-snapshot-parent/child");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        Prompter mockPrompter = createMockPrompter(
                YES, DEFAULT_CHOICE, new VersionPair("1.0-test", "1.0-test"), new VersionPair("1.0", "1.0-test"));
        phase.setPrompter(mockPrompter);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // validate
        ReleaseDescriptor descriptor = ReleaseUtils.buildReleaseDescriptor(builder);

        assertEquals("1.0-test", descriptor.getDependencyReleaseVersion("groupId:parent-external"));
        assertEquals("1.0-test", descriptor.getDependencyDevelopmentVersion("groupId:parent-external"));
    }

    @Test
    void testReleaseExternalParent() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-parent/child");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testSnapshotExternalExtension() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-snapshot-extension");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        phase.setPrompter(createNoMockPrompter());

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSnapshotInternalExtension() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("internal-snapshot-extension");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testReleaseExternalExtension() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-extension");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testAllowTimestampedSnapshots() throws Exception {
        List<MavenProject> reactorProjects = createDescriptorFromProjects("external-timestamped-snapshot-dependencies");
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(reactorProjects);

        builder.setInteractive(false);

        // confirm POM fails without allowTimestampedSnapshots
        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Should have failed execution");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        // check whether flag allows
        builder.setAllowTimestampedSnapshots(true);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // successful execution is verification enough
        assertTrue(true);
    }

    private List<MavenProject> createDescriptorFromProjects(String path) throws Exception {
        String dir = "check-dependencies/" + Objects.toString(path, "");
        return createReactorProjects(dir, dir, null);
    }

    private Prompter createNoMockPrompter() throws PrompterException {
        return createYesNoMockPrompter(false);
    }

    private Prompter createYesMockPrompter() throws PrompterException {
        return createYesNoMockPrompter(true);
    }

    private Prompter createYesNoMockPrompter(boolean yes) throws PrompterException {
        Prompter mockPrompter = mock(Prompter.class);

        when(mockPrompter.prompt(anyString(), eq(YES_NO_ARRAY), eq(NO))).thenReturn(yes ? YES : NO);

        return mockPrompter;
    }

    private Prompter createMockPrompterWithSnapshotReleasePlugin(
            String useSnapshotReleasePlugin, String resolveSnapshots) throws PrompterException {
        Prompter mockPrompter = mock(Prompter.class);

        when(mockPrompter.prompt(anyString(), eq(YES_NO_ARRAY), eq(NO))).thenReturn(useSnapshotReleasePlugin);
        when(mockPrompter.prompt(anyString(), eq(YES_NO_ARRAY), eq(NO))).thenReturn(resolveSnapshots);

        return mockPrompter;
    }

    private Prompter createMockPrompter(String resolveSnapshots, String resolutionType, VersionPair resolvedVersions)
            throws PrompterException {
        return createMockPrompter(resolveSnapshots, resolutionType, resolvedVersions, resolvedVersions);
    }

    private Prompter createMockPrompter(
            String resolveSnapshots, String resolutionType, VersionPair resolvedVersions, VersionPair defaultVersions)
            throws PrompterException {
        return createMockPrompter(
                resolveSnapshots,
                resolutionType,
                Collections.singletonList(resolvedVersions),
                Collections.singletonList(defaultVersions));
    }

    private Prompter createMockPrompter(
            String resolveSnapshots, String resolutionType, List<VersionPair> resolvedVersions)
            throws PrompterException {
        return createMockPrompter(resolveSnapshots, resolutionType, resolvedVersions, resolvedVersions);
    }

    private Prompter createMockPrompter(
            String resolveSnapshots,
            String resolutionType,
            List<VersionPair> resolvedVersions,
            List<VersionPair> defaultVersions)
            throws PrompterException {
        Prompter mockPrompter = mock(Prompter.class);

        when(mockPrompter.prompt(anyString(), eq(YES_NO_ARRAY), eq(NO))).thenReturn(resolveSnapshots);
        when(mockPrompter.prompt(anyString(), eq(CHOICE_ARRAY), eq(DEFAULT_CHOICE)))
                .thenReturn(resolutionType);

        for (int i = 0; i < resolvedVersions.size(); i++) {
            when(mockPrompter.prompt(
                            "Which release version should it be set to?", defaultVersions.get(i).releaseVersion))
                    .thenReturn(resolvedVersions.get(i).releaseVersion);
            when(mockPrompter.prompt(
                            "What version should the dependency be reset to for development?",
                            defaultVersions.get(i).developmentVersion))
                    .thenReturn(resolvedVersions.get(i).developmentVersion);
        }
        return mockPrompter;
    }

    private static class VersionPair {
        String releaseVersion;

        String developmentVersion;

        VersionPair(String releaseVersion, String developmentVersion) {
            this.releaseVersion = releaseVersion;
            this.developmentVersion = developmentVersion;
        }
    }
}
