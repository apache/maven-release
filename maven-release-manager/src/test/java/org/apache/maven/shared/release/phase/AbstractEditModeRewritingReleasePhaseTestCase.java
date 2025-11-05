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

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.edit.EditScmResult;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Base class with tests for rewriting POMs with edit mode.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
abstract class AbstractEditModeRewritingReleasePhaseTestCase extends AbstractRewritingReleasePhaseTestCase {

    @Test
    void testRewriteBasicPomWithEditMode() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom");
        ReleaseDescriptorBuilder builder = createDescriptorFromBasicPom(reactorProjects, "basic-pom");
        builder.setScmUseEditMode(true);
        mapNextVersion(builder, "groupId:artifactId");

        getTestedPhase()
                .execute(
                        ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewriteBasicPomWithEditModeFailure() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "basic-pom");
        builder.setScmUseEditMode(true);
        mapNextVersion(builder, "groupId:artifactId");

        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl(
                ReleaseUtils.buildReleaseDescriptor(builder).getScmSourceUrl());
        providerStub.setEditScmResult(new EditScmResult("", "", "", false));

        try {
            getTestedPhase()
                    .execute(
                            ReleaseUtils.buildReleaseDescriptor(builder),
                            new DefaultReleaseEnvironment(),
                            reactorProjects);

            fail("Should have thrown an exception");
        } catch (ReleaseScmCommandException e) {
            assertNull(e.getCause(), "Check no other cause");
        }
    }

    @Test
    void testRewriteBasicPomWithEditModeException() throws Exception {
        // prepare
        List<MavenProject> reactorProjects = createReactorProjects("basic-pom");
        ReleaseDescriptorBuilder builder = createDescriptorFromProjects(reactorProjects, "basic-pom");
        builder.setScmUseEditMode(true);
        mapNextVersion(builder, "groupId:artifactId");

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.edit(isA(ScmRepository.class), isA(ScmFileSet.class)))
                .thenThrow(new ScmException("..."));

        scmManager.setScmProvider(scmProviderMock);

        // execute
        try {
            getTestedPhase()
                    .execute(
                            ReleaseUtils.buildReleaseDescriptor(builder),
                            new DefaultReleaseEnvironment(),
                            reactorProjects);

            fail("Should have thrown an exception");
        } catch (ReleaseExecutionException e) {
            assertEquals(ScmException.class, e.getCause().getClass(), "Check cause");
        }
        // verify
        verify(scmProviderMock).edit(isA(ScmRepository.class), isA(ScmFileSet.class));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testRewritePomPluginDependencies() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("internal-snapshot-plugin-deps");
        ReleaseDescriptorBuilder builder = createDefaultConfiguration(reactorProjects, "internal-snapshot-plugin-deps");

        getTestedPhase()
                .execute(
                        ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewritePomUnmappedPluginDependencies() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("internal-snapshot-plugin-deps");
        ReleaseDescriptorBuilder builder =
                createUnmappedConfiguration(reactorProjects, "internal-snapshot-plugin-deps");

        try {
            getTestedPhase()
                    .execute(
                            ReleaseUtils.buildReleaseDescriptor(builder),
                            new DefaultReleaseEnvironment(),
                            reactorProjects);

            fail("Should have thrown an exception");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testRewritePomProfile() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("internal-snapshot-profile");
        ReleaseDescriptorBuilder builder = createDefaultConfiguration(reactorProjects, "internal-snapshot-profile");

        getTestedPhase()
                .execute(
                        ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), reactorProjects);

        assertTrue(comparePomFiles(reactorProjects));
    }

    @Test
    void testRewritePomUnmappedProfile() throws Exception {
        List<MavenProject> reactorProjects = createReactorProjects("internal-snapshot-profile");
        ReleaseDescriptorBuilder builder = createUnmappedConfiguration(reactorProjects, "internal-snapshot-profile");

        try {
            getTestedPhase()
                    .execute(
                            ReleaseUtils.buildReleaseDescriptor(builder),
                            new DefaultReleaseEnvironment(),
                            reactorProjects);

            fail("Should have thrown an exception");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }
}
