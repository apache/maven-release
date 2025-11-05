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
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.CommandParameter;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@PlexusTest
class CheckoutProjectFromScmTest extends AbstractReleaseTestCase {

    @Inject
    @Named("checkout-project-from-scm")
    private ReleasePhase phase;

    @Test
    void testExecuteStandard() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        File checkoutDirectory = getTestFile("target/checkout-test/standard");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());
        builder.setScmReleaseLabel("release-label");
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk";
        String scmUrl = "scm:svn:" + sourceUrl;
        builder.setScmSourceUrl(scmUrl);

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository(sourceUrl);
        ScmRepository repository = new ScmRepository("svn", scmProviderRepository);
        when(scmProviderMock.checkOut(
                        eq(repository),
                        argThat(new IsScmFileSetEquals(new ScmFileSet(checkoutDirectory))),
                        argThat(new IsScmTagEquals(new ScmTag("release-label"))),
                        argThat(new HasCommandParameter(CommandParameter.SHALLOW, true))))
                .thenReturn(new CheckOutScmResult("", null));

        scmManager.setScmProvider(scmProviderMock);
        scmManager.addScmRepositoryForUrl(scmUrl, repository);

        String dir = "scm-commit/single-pom";
        List<MavenProject> reactorProjects = createReactorProjects(dir, dir, null);
        builder.setWorkingDirectory(getWorkingDirectory(dir).toString());

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // prepare
        assertEquals("", ReleaseUtils.buildReleaseDescriptor(builder).getScmRelativePathProjectDirectory());

        verify(scmProviderMock)
                .checkOut(
                        eq(repository),
                        argThat(new IsScmFileSetEquals(new ScmFileSet(checkoutDirectory))),
                        argThat(new IsScmTagEquals(new ScmTag("release-label"))),
                        argThat(new HasCommandParameter(CommandParameter.SHALLOW, true)));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testExecuteMultiModuleWithDeepSubprojects() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        File checkoutDirectory = getTestFile("target/checkout-test/multimodule-with-deep-subprojects");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());
        builder.setScmReleaseLabel("release-label");
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk";
        String scmUrl = "scm:svn:" + sourceUrl;
        builder.setScmSourceUrl(scmUrl);

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository(sourceUrl);
        ScmRepository repository = new ScmRepository("svn", scmProviderRepository);
        when(scmProviderMock.checkOut(
                        eq(repository),
                        argThat(new IsScmFileSetEquals(new ScmFileSet(checkoutDirectory))),
                        argThat(new IsScmTagEquals(new ScmTag("release-label"))),
                        argThat(new HasCommandParameter(CommandParameter.SHALLOW, true))))
                .thenReturn(new CheckOutScmResult("", null));

        scmManager.setScmProvider(scmProviderMock);
        scmManager.addScmRepositoryForUrl(scmUrl, repository);

        String dir = "scm-commit/multimodule-with-deep-subprojects";
        List<MavenProject> reactorProjects = createReactorProjects(dir, dir, null);
        builder.setWorkingDirectory(getWorkingDirectory(dir).toString());

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals("", ReleaseUtils.buildReleaseDescriptor(builder).getScmRelativePathProjectDirectory());

        verify(scmProviderMock)
                .checkOut(
                        eq(repository),
                        argThat(new IsScmFileSetEquals(new ScmFileSet(checkoutDirectory))),
                        argThat(new IsScmTagEquals(new ScmTag("release-label"))),
                        argThat(new HasCommandParameter(CommandParameter.SHALLOW, true)));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testExecuteFlatMultiModule() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        File checkoutDirectory = getTestFile("target/checkout-test/flat-multi-module");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());
        builder.setScmReleaseLabel("release-label");
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk/root-project";
        String scmUrl = "scm:svn:" + sourceUrl;
        builder.setScmSourceUrl(scmUrl);

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository(sourceUrl);
        ScmRepository repository = new ScmRepository("svn", scmProviderRepository);
        when(scmProviderMock.checkOut(
                        eq(repository),
                        argThat(new IsScmFileSetEquals(new ScmFileSet(checkoutDirectory))),
                        argThat(new IsScmTagEquals(new ScmTag("release-label"))),
                        argThat(new HasCommandParameter(CommandParameter.SHALLOW, true))))
                .thenReturn(new CheckOutScmResult("", null));

        scmManager.setScmProvider(scmProviderMock);
        scmManager.addScmRepositoryForUrl(scmUrl, repository);

        List<MavenProject> reactorProjects =
                createReactorProjects("rewrite-for-release/pom-with-parent-flat", "root-project");
        builder.setWorkingDirectory(
                getWorkingDirectory("rewrite-for-release/pom-with-parent-flat").toString());

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(
                "root-project",
                ReleaseUtils.buildReleaseDescriptor(builder).getScmRelativePathProjectDirectory(),
                "not found root-project but "
                        + ReleaseUtils.buildReleaseDescriptor(builder).getScmRelativePathProjectDirectory());

        verify(scmProviderMock)
                .checkOut(
                        eq(repository),
                        argThat(new IsScmFileSetEquals(new ScmFileSet(checkoutDirectory))),
                        argThat(new IsScmTagEquals(new ScmTag("release-label"))),
                        argThat(new HasCommandParameter(CommandParameter.SHALLOW, true)));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testNoSuchScmProviderExceptionThrown() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/test/checkout").getAbsolutePath());

        scmManager.setException(new NoSuchScmProviderException("..."));

        String dir = "scm-commit/single-pom";
        List<MavenProject> reactorProjects = createReactorProjects(dir, dir, null);

        // execute
        try {
            builder.setUseReleaseProfile(false);

            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("commit should have failed");
        } catch (ReleaseExecutionException e) {
            assertEquals(NoSuchScmProviderException.class, e.getCause().getClass(), "check cause");
        }
    }

    @Test
    void testScmRepositoryExceptionThrown() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/test/checkout").getAbsolutePath());

        scmManager.setException(new ScmRepositoryException("..."));

        String dir = "scm-commit/single-pom";
        List<MavenProject> reactorProjects = createReactorProjects(dir, dir, null);

        // execute
        try {
            builder.setUseReleaseProfile(false);

            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

            fail("commit should have failed");
        } catch (ReleaseScmRepositoryException e) {
            assertNull(e.getCause(), "Check no additional cause");
        }
    }
}
