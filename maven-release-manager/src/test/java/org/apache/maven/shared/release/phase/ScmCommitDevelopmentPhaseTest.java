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

import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the SCM development commit phase.
 *
 * @author <a href="mailto:me@lcorneliussen.de">Lars Corneliussen</a>
 */
@PlexusTest
class ScmCommitDevelopmentPhaseTest extends AbstractReleaseTestCase {
    private static final String COMMIT_MESSAGE = "[maven-release-manager] prepare for next development iteration";

    private static final String ROLLBACK_PREFIX =
            "[maven-release-manager] rollback changes from release preparation of ";

    private List<MavenProject> reactorProjects;

    private MavenProject rootProject;

    private ReleaseDescriptorBuilder builder;

    private ScmProvider scmProviderMock;

    @Inject
    @Named("scm-commit-development")
    private ReleasePhase phase;

    @BeforeEach
    void setUp() throws Exception {
        reactorProjects = createReactorProjects();
        rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder = createReleaseDescriptorBuilder(rootProject);
    }

    @Test
    void testIsCorrectImplementation() {
        assertEquals(ScmCommitDevelopmentPhase.class, phase.getClass());
    }

    @Test
    void testNoCommitOrRollbackRequired() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder(rootProject);
        List<MavenProject> reactorProjects = createReactorProjects();

        builder.setRemoteTagging(false);
        builder.setPinExternals(false);
        builder.setSuppressCommitBeforeTagOrBranch(true);
        builder.setUpdateWorkingCopyVersions(false);

        prepareNoCheckin();

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        verifyNoCheckin();
    }

    @Test
    void testCommitsNextVersions() throws Exception {
        builder.setUpdateWorkingCopyVersions(true);

        prepareCheckin(COMMIT_MESSAGE);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        verifyCheckin(COMMIT_MESSAGE);
    }

    @Test
    void testCommitsNextVersionsAlternateMessage() throws Exception {
        builder.setUpdateWorkingCopyVersions(true);
        builder.setScmCommentPrefix("[release]");
        builder.setScmDevelopmentCommitComment("@{prefix} Development of @{groupId}:@{artifactId}");

        prepareCheckin("[release] Development of groupId:artifactId");

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        verifyCheckin("[release] Development of groupId:artifactId");
    }

    @Test
    void testCommitsRollbackPrepare() throws Exception {
        builder.setUpdateWorkingCopyVersions(false);

        String message = ROLLBACK_PREFIX + "release-label";

        prepareCheckin(message);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        verifyCheckin(message);
    }

    private void prepareCheckin(String message) throws Exception {
        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile(), rootProject.getFile());
        scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.checkIn(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class),
                        eq(message)))
                .thenReturn(new CheckInScmResult(
                        "...",
                        Collections.singletonList(
                                new ScmFile(rootProject.getFile().getPath(), ScmFileStatus.CHECKED_IN))));
        scmManager.setScmProvider(scmProviderMock);
    }

    private void verifyCheckin(String message) throws Exception {
        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile(), rootProject.getFile());
        verify(scmProviderMock)
                .checkIn(
                        isA(ScmRepository.class), argThat(new IsScmFileSetEquals(fileSet)),
                        isNull(ScmVersion.class), eq(message));
        verifyNoMoreInteractions(scmProviderMock);
    }

    private void prepareNoCheckin() throws Exception {
        scmProviderMock = mock(ScmProvider.class);
        scmManager.setScmProvider(scmProviderMock);
    }

    private void verifyNoCheckin() {
        verifyNoMoreInteractions(scmProviderMock);
    }

    private List<MavenProject> createReactorProjects() throws Exception {
        String dir = "scm-commit/single-pom";
        return createReactorProjects(dir, dir, null);
    }

    private static ReleaseDescriptorBuilder createReleaseDescriptorBuilder(MavenProject rootProject) {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setScmReleaseLabel("release-label");
        builder.setWorkingDirectory(rootProject.getFile().getParentFile().getAbsolutePath());
        return builder;
    }
}
