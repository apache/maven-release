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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.stubs.ScmManagerStub;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.testing.PlexusTestConfiguration;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the SCM modification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@PlexusTest
class ScmCheckModificationsPhaseTest implements PlexusTestConfiguration {
    @Inject
    @Named("scm-check-modifications")
    private ReleasePhase phase;

    private ScmManagerStub scmManagerStub;

    @Override
    public void customizeContainer(PlexusContainer container) {
        scmManagerStub = new ScmManagerStub();
        container.addComponent(scmManagerStub, ScmManager.class.getName());
    }

    @Test
    void testNoSuchScmProviderExceptionThrown() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/test/checkout").getAbsolutePath());

        scmManagerStub.setException(new NoSuchScmProviderException("..."));

        // execute
        try {
            phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseExecutionException e) {
            assertEquals(NoSuchScmProviderException.class, e.getCause().getClass(), "check cause");
        }

        try {
            phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseExecutionException e) {
            // verify
            assertEquals(NoSuchScmProviderException.class, e.getCause().getClass(), "check cause");
        }
    }

    @Test
    void testScmRepositoryExceptionThrown() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/test/checkout").getAbsolutePath());

        scmManagerStub.setException(new ScmRepositoryException("..."));

        // execute
        try {
            phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseScmRepositoryException e) {
            assertNull(e.getCause(), "Check no additional cause");
        }

        try {
            phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseScmRepositoryException e) {
            assertNull(e.getCause(), "Check no additional cause");
        }
    }

    @Test
    void testScmExceptionThrown() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/test/checkout").getAbsolutePath());

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.status(isA(ScmRepository.class), isA(ScmFileSet.class)))
                .thenThrow(new ScmException("..."));

        scmManagerStub.setScmProvider(scmProviderMock);

        // execute
        try {
            phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseExecutionException e) {
            assertEquals(ScmException.class, e.getCause().getClass(), "check cause");
        }

        try {
            phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseExecutionException e) {
            assertEquals(ScmException.class, e.getCause().getClass(), "check cause");
        }

        // verify
        verify(scmProviderMock, times(2)).status(isA(ScmRepository.class), isA(ScmFileSet.class));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testScmResultFailure() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        ScmProviderStub providerStub =
                (ScmProviderStub) scmManagerStub.getProviderByUrl("scm:svn:file://localhost/tmp/scm-repo");

        providerStub.setStatusScmResult(new StatusScmResult("", "", "", false));

        try {
            phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseScmCommandException e) {
            assertNull(e.getCause(), "check no other cause");
        }

        try {
            phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseScmCommandException e) {
            assertNull(e.getCause(), "check no other cause");
        }
    }

    @Test
    void testNoModifications() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        setChangedFiles(builder, Collections.emptyList());

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testModificationsToExcludedFilesOnly() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        setChangedFiles(builder, Arrays.asList("release.properties", "pom.xml.backup", "pom.xml.tag", "pom.xml.next"));

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

        // successful execution is verification enough
        assertTrue(true);
    }

    // MRELEASE-645: Allow File/Directory Patterns for the checkModificationExcludes Option
    @Test
    void testModificationsToCustomExcludedFilesOnly() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        builder.setCheckModificationExcludes(Collections.singletonList("**/keep.me"));

        setChangedFiles(
                builder,
                Arrays.asList(
                        "release.properties",
                        "pom.xml.backup",
                        "pom.xml.tag",
                        "pom.xml.next",
                        "keep.me",
                        "src/app/keep.me",
                        "config\\keep.me"));

        assertEquals(
                ReleaseResult.SUCCESS,
                phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null)
                        .getResultCode());

        assertEquals(
                ReleaseResult.SUCCESS,
                phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null)
                        .getResultCode());
    }

    @Test
    void testModificationsToPoms() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        setChangedFiles(builder, Arrays.asList("pom.xml", "module/pom.xml"));

        try {
            phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        try {
            phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testModificationsToIncludedFilesOnly() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        setChangedFiles(builder, Collections.singletonList("something.txt"));

        try {
            phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        try {
            phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testModificationsToIncludedAndExcludedFiles() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        setChangedFiles(
                builder,
                Arrays.asList(
                        "release.properties", "pom.xml.backup", "pom.xml.tag", "pom.xml.release", "something.txt"));

        try {
            phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        try {
            phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null);

            fail("Status check should have failed");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testModificationsToAdditionalExcludedFiles() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();
        builder.setCheckModificationExcludes(Collections.singletonList("something.*"));

        setChangedFiles(builder, Collections.singletonList("something.txt"));

        assertEquals(
                ReleaseResult.SUCCESS,
                phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null)
                        .getResultCode());

        assertEquals(
                ReleaseResult.SUCCESS,
                phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null)
                        .getResultCode());
    }

    // MRELEASE-775
    @Test
    void testMultipleExclusionPatternMatch() throws Exception {
        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder();

        builder.setCheckModificationExcludes(Collections.singletonList("release.properties"));

        setChangedFiles(builder, Collections.singletonList("release.properties"));

        assertEquals(
                ReleaseResult.SUCCESS,
                phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null)
                        .getResultCode());

        assertEquals(
                ReleaseResult.SUCCESS,
                phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), new DefaultReleaseEnvironment(), null)
                        .getResultCode());
    }

    private void setChangedFiles(ReleaseDescriptorBuilder builder, List<String> changedFiles) throws Exception {
        ScmProviderStub providerStub =
                (ScmProviderStub) scmManagerStub.getProviderByUrl("scm:svn:file://localhost/tmp/scm-repo");

        providerStub.setStatusScmResult(new StatusScmResult("", createScmFiles(changedFiles)));
    }

    private static List<ScmFile> createScmFiles(List<String> changedFiles) {
        List<ScmFile> files = new ArrayList<>(changedFiles.size());
        for (Iterator<String> i = changedFiles.iterator(); i.hasNext(); ) {
            String fileName = i.next();
            files.add(new ScmFile(fileName, ScmFileStatus.MODIFIED));
        }
        return files;
    }

    private static ReleaseDescriptorBuilder createReleaseDescriptorBuilder() {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:file://localhost/tmp/scm-repo");
        builder.setWorkingDirectory(getTestFile("target/test/checkout").getAbsolutePath());
        return builder;
    }
}
