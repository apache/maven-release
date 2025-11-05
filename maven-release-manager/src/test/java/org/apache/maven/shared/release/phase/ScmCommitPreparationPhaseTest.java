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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the release or branch preparation SCM commit phaseScmCommitRelease.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@PlexusTest
class ScmCommitPreparationPhaseTest extends AbstractReleaseTestCase {
    private static final String PREFIX = "[maven-release-manager] prepare release ";

    @Inject
    @Named("scm-commit-release")
    private ReleasePhase phaseScmCommitRelease;

    @Inject
    @Named("scm-commit-development")
    private ReleasePhase phaseScmCommitDevelopment;

    @Test
    void testIsCorrectImplementation() {
        assertEquals(ScmCommitReleasePhase.class, phaseScmCommitRelease.getClass());
    }

    @Test
    void testResolvesCorrectBranchImplementation() throws Exception {
        assertInstanceOf(ScmCommitReleasePhase.class, phaseScmCommitRelease);
        assertInstanceOf(ScmCommitDevelopmentPhase.class, phaseScmCommitDevelopment);
    }

    @Test
    void testCommit() throws Exception {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(rootProject.getFile().getParentFile().getAbsolutePath());
        builder.setScmReleaseLabel("release-label");

        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile(), rootProject.getFile());

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.checkIn(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class),
                        eq(PREFIX + "release-label")))
                .thenReturn(new CheckInScmResult(
                        "...",
                        Collections.singletonList(
                                new ScmFile(rootProject.getFile().getPath(), ScmFileStatus.CHECKED_IN))));

        scmManager.setScmProvider(scmProviderMock);

        // execute
        phaseScmCommitRelease.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(scmProviderMock)
                .checkIn(
                        isA(ScmRepository.class), argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class), eq(PREFIX + "release-label"));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testCommitAlternateMessage() throws Exception {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setScmCommentPrefix("[release]");
        builder.setScmReleaseCommitComment("@{prefix} Release of @{groupId}:@{artifactId} @{releaseLabel}");
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(rootProject.getFile().getParentFile().getAbsolutePath());
        builder.setScmReleaseLabel("release-label");

        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile(), rootProject.getFile());

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.checkIn(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class),
                        eq("[release] Release of groupId:artifactId release-label")))
                .thenReturn(new CheckInScmResult(
                        "...",
                        Collections.singletonList(
                                new ScmFile(rootProject.getFile().getPath(), ScmFileStatus.CHECKED_IN))));

        scmManager.setScmProvider(scmProviderMock);

        // execute
        phaseScmCommitRelease.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(scmProviderMock)
                .checkIn(
                        isA(ScmRepository.class), argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class), eq("[release] Release of groupId:artifactId release-label"));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testCommitMultiModule() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        String dir = "scm-commit/multiple-poms";
        List<MavenProject> reactorProjects = createReactorProjects(dir, dir, null);
        builder.setScmSourceUrl("scm-url");
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(rootProject.getFile().getParentFile().getAbsolutePath());
        builder.setScmReleaseLabel("release-label");

        List<File> poms = new ArrayList<>();
        for (Iterator<MavenProject> i = reactorProjects.iterator(); i.hasNext(); ) {
            MavenProject project = i.next();
            poms.add(project.getFile());
        }
        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile(), poms);

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.checkIn(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class),
                        eq(PREFIX + "release-label")))
                .thenReturn(new CheckInScmResult(
                        "...",
                        Collections.singletonList(
                                new ScmFile(rootProject.getFile().getPath(), ScmFileStatus.CHECKED_IN))));
        scmManager.setScmProvider(scmProviderMock);

        // execute
        phaseScmCommitRelease.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(scmProviderMock)
                .checkIn(
                        isA(ScmRepository.class), argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class), eq(PREFIX + "release-label"));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testCommitDevelopment() throws Exception {
        // prepare

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();
        builder.setScmSourceUrl("scm-url");
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(rootProject.getFile().getParentFile().getAbsolutePath());
        builder.setScmReleaseLabel("release-label");

        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile(), rootProject.getFile());

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.checkIn(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class),
                        eq("[maven-release-manager] prepare for next development iteration")))
                .thenReturn(new CheckInScmResult(
                        "...",
                        Collections.singletonList(
                                new ScmFile(rootProject.getFile().getPath(), ScmFileStatus.CHECKED_IN))));

        scmManager.setScmProvider(scmProviderMock);

        // execute
        phaseScmCommitDevelopment.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(scmProviderMock)
                .checkIn(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class),
                        eq("[maven-release-manager] prepare for next development iteration"));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testCommitDevelopmentAlternateMessage() throws Exception {

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();
        builder.setScmSourceUrl("scm-url");
        builder.setScmCommentPrefix("[release]");
        builder.setScmDevelopmentCommitComment(
                "@{prefix} Bump version of @{groupId}:@{artifactId} after @{releaseLabel}");
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(rootProject.getFile().getParentFile().getAbsolutePath());
        builder.setScmReleaseLabel("release-label");

        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile(), rootProject.getFile());

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.checkIn(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class),
                        eq("[release] Bump version of groupId:artifactId after release-label")))
                .thenReturn(new CheckInScmResult(
                        "...",
                        Collections.singletonList(
                                new ScmFile(rootProject.getFile().getPath(), ScmFileStatus.CHECKED_IN))));

        scmManager.setScmProvider(scmProviderMock);

        // execute
        phaseScmCommitDevelopment.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(scmProviderMock)
                .checkIn(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class),
                        eq("[release] Bump version of groupId:artifactId after release-label"));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testCommitNoReleaseLabel() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();

        try {
            phaseScmCommitRelease.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);
            fail("Should have thrown an exception");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testCommitGenerateReleasePoms() throws Exception {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setGenerateReleasePoms(true);
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(rootProject.getFile().getParentFile().getAbsolutePath());
        builder.setScmReleaseLabel("release-label");

        List<File> files = new ArrayList<>();
        files.add(rootProject.getFile());
        files.add(ReleaseUtil.getReleasePom(rootProject));
        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile(), files);

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.checkIn(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class),
                        eq(PREFIX + "release-label")))
                .thenReturn(new CheckInScmResult(
                        "...",
                        Collections.singletonList(
                                new ScmFile(rootProject.getFile().getPath(), ScmFileStatus.CHECKED_IN))));

        scmManager.setScmProvider(scmProviderMock);

        // execute
        phaseScmCommitRelease.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(scmProviderMock)
                .checkIn(
                        isA(ScmRepository.class), argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class), eq(PREFIX + "release-label"));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testSimulateCommit() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();
        builder.setScmSourceUrl("scm-url");
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(rootProject.getFile().getParentFile().getAbsolutePath());
        builder.setScmReleaseLabel("release-label");

        ScmProvider scmProviderMock = mock(ScmProvider.class);

        scmManager.setScmProvider(scmProviderMock);

        phaseScmCommitRelease.simulate(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // never invoke scmProviderMock
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testSimulateCommitNoReleaseLabel() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();

        try {
            phaseScmCommitRelease.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);
            fail("Should have thrown an exception");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testNoSuchScmProviderExceptionThrown() throws Exception {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        scmManager.setException(new NoSuchScmProviderException("..."));
        // execute
        try {
            phaseScmCommitRelease.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Status check should have failed");
        } catch (ReleaseExecutionException e) {
            // verify
            assertEquals(NoSuchScmProviderException.class, e.getCause().getClass(), "check cause");
        }
    }

    @Test
    void testScmRepositoryExceptionThrown() throws Exception {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        scmManager.setException(new ScmRepositoryException("..."));

        // execute
        try {
            phaseScmCommitRelease.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Status check should have failed");
        } catch (ReleaseScmRepositoryException e) {
            // verify
            assertNull(e.getCause(), "Check no additional cause");
        }
    }

    @Test
    void testScmExceptionThrown() throws Exception {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.checkIn(
                        isA(ScmRepository.class), isA(ScmFileSet.class), isNull(ScmVersion.class), isA(String.class)))
                .thenThrow(new ScmException("..."));

        scmManager.setScmProvider(scmProviderMock);

        // execute
        try {
            phaseScmCommitRelease.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Status check should have failed");
        } catch (ReleaseExecutionException e) {
            assertEquals(ScmException.class, e.getCause().getClass(), "check cause");
        }

        // verify
        verify(scmProviderMock)
                .checkIn(
                        isA(ScmRepository.class), isA(ScmFileSet.class),
                        isNull(ScmVersion.class), isA(String.class));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testScmResultFailure() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl("scm-url");

        providerStub.setCheckInScmResult(new CheckInScmResult("", "", "", false));

        try {
            phaseScmCommitRelease.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Commit should have failed");
        } catch (ReleaseScmCommandException e) {
            assertNull(e.getCause(), "check no other cause");
        }
    }

    @Test
    void testSuppressCommitWithRemoteTaggingFails() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();

        builder.setRemoteTagging(true);
        builder.setPinExternals(false);
        builder.setSuppressCommitBeforeTagOrBranch(true);

        ScmProvider scmProviderMock = mock(ScmProvider.class);

        scmManager.setScmProvider(scmProviderMock);

        try {
            phaseScmCommitRelease.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Commit should have failed with ReleaseFailureException");
        } catch (ReleaseFailureException e) {
            assertNull(e.getCause(), "check no other cause");
        }

        // never invoke scmProviderMock
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testSuppressCommitAfterBranch() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();

        builder.setBranchCreation(true);
        builder.setRemoteTagging(false);
        builder.setSuppressCommitBeforeTagOrBranch(true);

        ScmProvider scmProviderMock = mock(ScmProvider.class);

        scmManager.setScmProvider(scmProviderMock);

        phaseScmCommitRelease.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // never invoke scmProviderMock
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testCommitMultiModuleWithCheckModificationExcludes() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        String dir = "scm-commit/multiple-poms";
        List<MavenProject> reactorProjects = createReactorProjects(dir, dir, null);
        builder.setScmSourceUrl("scm-url");
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(rootProject.getFile().getParentFile().getAbsolutePath());
        builder.setScmReleaseLabel("release-label");
        builder.setCheckModificationExcludes(Collections.singletonList("**/subproject2/*"));

        List<File> poms = new ArrayList<>();
        for (Iterator<MavenProject> i = reactorProjects.iterator(); i.hasNext(); ) {
            MavenProject project = i.next();
            // This is a mock match that verifies that the project has not been submitted
            if (!"subproject2".equals(project.getName())) {
                poms.add(project.getFile());
            }
        }
        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile(), poms);

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.checkIn(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class),
                        eq(PREFIX + "release-label")))
                .thenReturn(new CheckInScmResult(
                        "...",
                        Collections.singletonList(
                                new ScmFile(rootProject.getFile().getPath(), ScmFileStatus.CHECKED_IN))));
        scmManager.setScmProvider(scmProviderMock);

        // execute
        phaseScmCommitRelease.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(scmProviderMock)
                .checkIn(
                        isA(ScmRepository.class), argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class), eq(PREFIX + "release-label"));
        verifyNoMoreInteractions(scmProviderMock);
    }

    private List<MavenProject> createReactorProjects() throws Exception {
        String dir = "scm-commit/single-pom";
        return createReactorProjects(dir, dir, null);
    }

    private static ReleaseDescriptorBuilder createReleaseDescriptorBuilder() {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setScmReleaseLabel("release-label");
        builder.setWorkingDirectory(getTestFile("target/test/checkout").getAbsolutePath());
        return builder;
    }
}
