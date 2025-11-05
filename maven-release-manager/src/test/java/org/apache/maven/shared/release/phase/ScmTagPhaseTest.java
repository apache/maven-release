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
import java.io.IOException;
import java.nio.file.LinkOption;
import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmTagParameters;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseDescriptorStore;
import org.apache.maven.shared.release.config.ReleaseDescriptorStoreStub;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the SCM tag phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@PlexusTest
class ScmTagPhaseTest extends AbstractReleaseTestCase {
    @Inject
    @Named("scm-tag")
    private ReleasePhase phase;

    @Override
    public void customizeContainer(PlexusContainer container) {
        super.customizeContainer(container);
        container.addComponent(new ReleaseDescriptorStoreStub(), ReleaseDescriptorStore.class.getName());
    }

    public static String getPath(File file) throws IOException {
        return file.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
    }

    @Test
    void testTag() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();
        builder.setScmSourceUrl("scm-url");
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(getPath(rootProject.getFile().getParentFile()));
        builder.setPomFileName(rootProject.getFile().getName());
        builder.setScmReleaseLabel("release-label");
        builder.setScmCommentPrefix("[my prefix] ");
        builder.setScmSignTags(true);

        ScmTagParameters expectedTagParameters = new ScmTagParameters("[my prefix] copy for tag release-label");
        expectedTagParameters.setSign(true);

        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile());

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.tag(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        eq("release-label"),
                        argThat(new IsScmTagParametersEquals(expectedTagParameters))))
                .thenReturn(new TagScmResult(
                        "...",
                        Collections.singletonList(new ScmFile(getPath(rootProject.getFile()), ScmFileStatus.TAGGED))));
        scmManager.setScmProvider(scmProviderMock);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(scmProviderMock)
                .tag(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        eq("release-label"),
                        argThat(new IsScmTagParametersEquals(expectedTagParameters)));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testCommitMultiModuleDeepFolders() throws Exception {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects("scm-commit/multimodule-with-deep-subprojects", "");
        String sourceUrl = "http://svn.example.com/repos/project/trunk/";
        String scmUrl = "scm:svn:" + sourceUrl;
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl(scmUrl);
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(getPath(rootProject.getFile().getParentFile()));
        builder.setPomFileName(rootProject.getFile().getName());
        builder.setScmReleaseLabel("release-label");
        builder.setScmCommentPrefix("[my prefix] ");
        builder.setScmTagBase("http://svn.example.com/repos/project/releases/");

        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile());

        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository(sourceUrl);
        scmProviderRepository.setTagBase("http://svn.example.com/repos/project/releases/");
        ScmRepository repository = new ScmRepository("svn", scmProviderRepository);
        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.tag(
                        eq(repository),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        eq("release-label"),
                        argThat(new IsScmTagParametersEquals(
                                new ScmTagParameters("[my prefix] copy for tag release-label")))))
                .thenReturn(new TagScmResult(
                        "...",
                        Collections.singletonList(new ScmFile(getPath(rootProject.getFile()), ScmFileStatus.TAGGED))));

        scmManager.setScmProvider(scmProviderMock);
        scmManager.addScmRepositoryForUrl(scmUrl, repository);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(scmProviderMock)
                .tag(
                        eq(repository),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        eq("release-label"),
                        argThat(new IsScmTagParametersEquals(
                                new ScmTagParameters("[my prefix] copy for tag release-label"))));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testCommitForFlatMultiModule() throws Exception {
        // prepare
        List<MavenProject> reactorProjects =
                createReactorProjects("rewrite-for-release/pom-with-parent-flat", "root-project");
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl(rootProject.getScm().getConnection());
        builder.setWorkingDirectory(getWorkingDirectory("rewrite-for-release/pom-with-parent-flat")
                .toRealPath(LinkOption.NOFOLLOW_LINKS)
                .toString());
        builder.setPomFileName("root-project/pom.xml");
        builder.setScmReleaseLabel("release-label");
        builder.setScmCommentPrefix("[my prefix] ");

        // one directory up from root project
        ScmFileSet fileSet =
                new ScmFileSet(rootProject.getFile().getParentFile().getParentFile());

        String scmUrl = "file://localhost/tmp/scm-repo/trunk";
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository(scmUrl);
        ScmRepository repository = new ScmRepository("svn", scmProviderRepository);
        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.tag(
                        eq(repository),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        eq("release-label"),
                        argThat(new IsScmTagParametersEquals(
                                new ScmTagParameters("[my prefix] copy for tag release-label")))))
                .thenReturn(new TagScmResult(
                        "...",
                        Collections.singletonList(new ScmFile(getPath(rootProject.getFile()), ScmFileStatus.TAGGED))));

        scmManager.setScmProvider(scmProviderMock);
        scmManager.addScmRepositoryForUrl("scm:svn:" + scmUrl, repository);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(scmProviderMock)
                .tag(
                        eq(repository),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        eq("release-label"),
                        argThat(new IsScmTagParametersEquals(
                                new ScmTagParameters("[my prefix] copy for tag release-label"))));
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
        builder.setWorkingDirectory(getPath(rootProject.getFile().getParentFile()));
        builder.setPomFileName(rootProject.getFile().getName());
        builder.setScmReleaseLabel("release-label");
        builder.setScmCommentPrefix("[my prefix] ");

        ScmFileSet fileSet = new ScmFileSet(rootProject.getFile().getParentFile());

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.tag(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        eq("release-label"),
                        argThat(new IsScmTagParametersEquals(
                                new ScmTagParameters("[my prefix] copy for tag release-label")))))
                .thenReturn(new TagScmResult(
                        "...",
                        Collections.singletonList(new ScmFile(getPath(rootProject.getFile()), ScmFileStatus.TAGGED))));

        scmManager.setScmProvider(scmProviderMock);

        // exeucte
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        verify(scmProviderMock)
                .tag(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        eq("release-label"),
                        argThat(new IsScmTagParametersEquals(
                                new ScmTagParameters("[my prefix] copy for tag release-label"))));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testTagNoReleaseLabel() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);
            fail("Should have thrown an exception");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testSimulateTag() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();
        builder.setScmSourceUrl("scm-url");
        MavenProject rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(getPath(rootProject.getFile().getParentFile()));
        builder.setPomFileName(rootProject.getFile().getName());
        builder.setScmReleaseLabel("release-label");

        ScmProvider scmProviderMock = mock(ScmProvider.class);

        scmManager.setScmProvider(scmProviderMock);

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        // no scmProvider invocation
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testSimulateTagNoReleaseLabel() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        List<MavenProject> reactorProjects = createReactorProjects();

        try {
            phase.simulate(
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
            phase.execute(
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
            phase.execute(
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
        when(scmProviderMock.tag(
                        isA(ScmRepository.class),
                        isA(ScmFileSet.class),
                        isA(String.class),
                        isA(ScmTagParameters.class)))
                .thenThrow(new ScmException("..."));

        scmManager.setScmProvider(scmProviderMock);

        // execute
        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Status check should have failed");
        } catch (ReleaseExecutionException e) {
            assertEquals(ScmException.class, e.getCause().getClass(), "check cause");
        }

        // verify
        verify(scmProviderMock)
                .tag(isA(ScmRepository.class), isA(ScmFileSet.class), isA(String.class), isA(ScmTagParameters.class));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testScmResultFailure() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects();
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl("scm-url");

        providerStub.setTagScmResult(new TagScmResult("", "", "", false));

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("Commit should have failed");
        } catch (ReleaseScmCommandException e) {
            assertNull(e.getCause(), "check no other cause");
        }
    }

    private List<MavenProject> createReactorProjects() throws Exception {
        return createReactorProjects("scm-commit/single-pom", "");
    }

    private static ReleaseDescriptorBuilder createReleaseDescriptorBuilder() throws IOException {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setScmReleaseLabel("release-label");
        File workingDir = getTestFile("target/test/checkout");
        if (!workingDir.exists()) {
            assertTrue(
                    workingDir.mkdirs(), "Failed to create the directory, along with all necessary parent directories");
        }
        builder.setWorkingDirectory(getPath(workingDir));
        builder.setPomFileName("pom.xml");
        return builder;
    }
}
