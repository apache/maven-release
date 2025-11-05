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
package org.apache.maven.shared.release;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseDescriptorStore;
import org.apache.maven.shared.release.config.ReleaseDescriptorStoreException;
import org.apache.maven.shared.release.config.ReleaseDescriptorStoreStub;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.phase.ReleasePhase;
import org.apache.maven.shared.release.phase.ReleasePhaseStub;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.codehaus.plexus.testing.PlexusExtension.getTestPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the default release manager.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@PlexusTest
class DefaultReleaseManagerTest {

    @Inject
    private ReleaseDescriptorStoreStub configStore;

    @Inject
    @Named("test")
    private ReleaseManager releaseManagerTest;

    @Inject
    @Named("step1")
    private ReleasePhase phaseStep1;

    @Inject
    @Named("step2")
    private ReleasePhase phaseStep2;

    @Inject
    @Named("step3")
    private ReleasePhase phaseStep3;

    @Inject
    @Named("rollbackPhase1")
    private ReleasePhase phaseRollbackPhase1;

    @Inject
    @Named("updateVersionsPhase1")
    private ReleasePhase phaseUpdateVersionsPhase1;

    @Inject
    @Named("branch1")
    private ReleasePhase phaseBranch1;

    @Inject
    private ScmManager scmManager;

    @Test
    void testPrepareNoCompletedPhase() throws Exception {
        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase(null);

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setUserProperties(new Properties());

        releaseManagerTest.prepare(prepareRequest);

        assertTrue(((ReleasePhaseStub) phaseStep1).isExecuted(), "step1 executed");
        assertFalse(((ReleasePhaseStub) phaseStep1).isSimulated(), "step1 not simulated");
        assertTrue(((ReleasePhaseStub) phaseStep2).isExecuted(), "step2 executed");
        assertFalse(((ReleasePhaseStub) phaseStep2).isSimulated(), "step2 not simulated");
        assertTrue(((ReleasePhaseStub) phaseStep3).isExecuted(), "step3 executed");
        assertFalse(((ReleasePhaseStub) phaseStep3).isSimulated(), "step3 not simulated");
    }

    @Test
    void testPrepareCompletedPhase() throws Exception {
        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("step1");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setUserProperties(new Properties());

        releaseManagerTest.prepare(prepareRequest);

        assertFalse(((ReleasePhaseStub) phaseStep1).isExecuted(), "step1 not executed");
        assertFalse(((ReleasePhaseStub) phaseStep1).isSimulated(), "step1 not simulated");
        assertTrue(((ReleasePhaseStub) phaseStep2).isExecuted(), "step2 executed");
        assertFalse(((ReleasePhaseStub) phaseStep2).isSimulated(), "step2 not simulated");
        assertTrue(((ReleasePhaseStub) phaseStep3).isExecuted(), "step3 executed");
        assertFalse(((ReleasePhaseStub) phaseStep3).isSimulated(), "step3 not simulated");
    }

    @Test
    void testPrepareCompletedPhaseNoResume() throws Exception {
        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("step1");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(false);
        prepareRequest.setResume(false);
        prepareRequest.setUserProperties(new Properties());

        releaseManagerTest.prepare(prepareRequest);

        assertFalse(((ReleasePhaseStub) phaseStep1).isExecuted(), "step1 executed");
        assertFalse(((ReleasePhaseStub) phaseStep1).isSimulated(), "step1 not simulated");
        assertTrue(((ReleasePhaseStub) phaseStep2).isExecuted(), "step2 executed");
        assertFalse(((ReleasePhaseStub) phaseStep2).isSimulated(), "step2 not simulated");
        assertTrue(((ReleasePhaseStub) phaseStep3).isExecuted(), "step3 executed");
        assertFalse(((ReleasePhaseStub) phaseStep3).isSimulated(), "step3 not simulated");
    }

    @Test
    void testPrepareCompletedAllPhases() throws Exception {
        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("step3");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setUserProperties(new Properties());

        releaseManagerTest.prepare(prepareRequest);

        assertFalse(((ReleasePhaseStub) phaseStep1).isExecuted(), "step1 not executed");
        assertFalse(((ReleasePhaseStub) phaseStep1).isSimulated(), "step1 not simulated");
        assertFalse(((ReleasePhaseStub) phaseStep2).isExecuted(), "step2 not executed");
        assertFalse(((ReleasePhaseStub) phaseStep2).isSimulated(), "step2 not simulated");
        assertFalse(((ReleasePhaseStub) phaseStep3).isExecuted(), "step3 not executed");
        assertFalse(((ReleasePhaseStub) phaseStep3).isSimulated(), "step3 not simulated");
    }

    @Test
    void testPrepareInvalidCompletedPhase() throws Exception {
        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("foo");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setUserProperties(new Properties());

        releaseManagerTest.prepare(prepareRequest);

        assertTrue(((ReleasePhaseStub) phaseStep1).isExecuted(), "step1 executed");
        assertFalse(((ReleasePhaseStub) phaseStep1).isSimulated(), "step1 not simulated");
        assertTrue(((ReleasePhaseStub) phaseStep2).isExecuted(), "step2 executed");
        assertFalse(((ReleasePhaseStub) phaseStep2).isSimulated(), "step2 not simulated");
        assertTrue(((ReleasePhaseStub) phaseStep3).isExecuted(), "step3 executed");
        assertFalse(((ReleasePhaseStub) phaseStep3).isSimulated(), "step3 not simulated");
    }

    @Test
    void testPrepareSimulateNoCompletedPhase() throws Exception {
        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase(null);

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(true);
        prepareRequest.setResume(true);
        prepareRequest.setUserProperties(new Properties());

        releaseManagerTest.prepare(prepareRequest);

        assertTrue(((ReleasePhaseStub) phaseStep1).isSimulated(), "step1 simulated");
        assertFalse(((ReleasePhaseStub) phaseStep1).isExecuted(), "step1 not executed");
        assertTrue(((ReleasePhaseStub) phaseStep2).isSimulated(), "step2 simulated");
        assertFalse(((ReleasePhaseStub) phaseStep2).isExecuted(), "step2 not executed");
        assertTrue(((ReleasePhaseStub) phaseStep3).isSimulated(), "step3 simulated");
        assertFalse(((ReleasePhaseStub) phaseStep3).isExecuted(), "step3 not executed");
    }

    @Test
    void testPrepareSimulateCompletedPhase() throws Exception {
        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("step1");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(true);
        prepareRequest.setResume(true);
        prepareRequest.setUserProperties(new Properties());

        releaseManagerTest.prepare(prepareRequest);

        assertFalse(((ReleasePhaseStub) phaseStep1).isSimulated(), "step1 not simulated");
        assertFalse(((ReleasePhaseStub) phaseStep1).isExecuted(), "step1 not executed");
        assertTrue(((ReleasePhaseStub) phaseStep2).isSimulated(), "step2 simulated");
        assertFalse(((ReleasePhaseStub) phaseStep2).isExecuted(), "step2 not executed");
        assertTrue(((ReleasePhaseStub) phaseStep3).isSimulated(), "step3 simulated");
        assertFalse(((ReleasePhaseStub) phaseStep3).isExecuted(), "step3 not executed");
    }

    @Test
    void testPrepareSimulateCompletedAllPhases() throws Exception {
        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("step3");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(true);
        prepareRequest.setResume(true);
        prepareRequest.setUserProperties(new Properties());

        releaseManagerTest.prepare(prepareRequest);

        assertFalse(((ReleasePhaseStub) phaseStep1).isSimulated(), "step1 not simulated");
        assertFalse(((ReleasePhaseStub) phaseStep1).isExecuted(), "step1 not executed");
        assertFalse(((ReleasePhaseStub) phaseStep2).isSimulated(), "step2 not simulated");
        assertFalse(((ReleasePhaseStub) phaseStep2).isExecuted(), "step2 not executed");
        assertFalse(((ReleasePhaseStub) phaseStep3).isSimulated(), "step3 not simulated");
        assertFalse(((ReleasePhaseStub) phaseStep3).isExecuted(), "step3 not executed");
    }

    @Test
    void testPrepareSimulateInvalidCompletedPhase() throws Exception {
        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("foo");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(true);
        prepareRequest.setResume(true);
        prepareRequest.setUserProperties(new Properties());

        releaseManagerTest.prepare(prepareRequest);

        assertTrue(((ReleasePhaseStub) phaseStep1).isSimulated(), "step1 simulated");
        assertFalse(((ReleasePhaseStub) phaseStep1).isExecuted(), "step1 not executed");
        assertTrue(((ReleasePhaseStub) phaseStep2).isSimulated(), "step2 simulated");
        assertFalse(((ReleasePhaseStub) phaseStep2).isExecuted(), "step2 not executed");
        assertTrue(((ReleasePhaseStub) phaseStep3).isSimulated(), "step3 simulated");
        assertFalse(((ReleasePhaseStub) phaseStep3).isExecuted(), "step3 not executed");
    }

    @Test
    void testReleaseConfigurationStoreReadFailure() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());

        ReleaseDescriptorStore configStoreMock = mock(ReleaseDescriptorStore.class);
        when(configStoreMock.read(builder))
                .thenThrow(new ReleaseDescriptorStoreException("message", new IOException("ioExceptionMsg")));

        ((DefaultReleaseManager) releaseManagerTest).setConfigStore(configStoreMock);

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setUserProperties(new Properties());

        // execute
        try {
            releaseManagerTest.prepare(prepareRequest);
            fail("Should have failed to read configuration");
        } catch (ReleaseExecutionException e) {
            // good
            assertEquals(ReleaseDescriptorStoreException.class, e.getCause().getClass(), "check cause");
        }

        // verify
        verify(configStoreMock).read(builder);
        verifyNoMoreInteractions(configStoreMock);
    }

    @Test
    void testReleaseConfigurationStoreWriteFailure() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());

        ReleaseDescriptorStore configStoreMock = mock(ReleaseDescriptorStore.class);
        doThrow(new ReleaseDescriptorStoreException("message", new IOException("ioExceptionMsg")))
                .when(configStoreMock)
                .write(any(ReleaseDescriptor.class));

        ((DefaultReleaseManager) releaseManagerTest).setConfigStore(configStoreMock);

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(false);
        prepareRequest.setResume(false);
        prepareRequest.setUserProperties(new Properties());

        // execute
        try {
            releaseManagerTest.prepare(prepareRequest);
            fail("Should have failed to read configuration");
        } catch (ReleaseExecutionException e) {
            // good
            assertEquals(ReleaseDescriptorStoreException.class, e.getCause().getClass(), "check cause");
        }

        // verify
        verify(configStoreMock).write(any(ReleaseDescriptor.class));
        verifyNoMoreInteractions(configStoreMock);
    }

    @Test
    void testReleaseConfigurationStoreClean() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());

        ReleaseDescriptorStore configStoreMock = mock(ReleaseDescriptorStore.class);

        ((DefaultReleaseManager) releaseManagerTest).setConfigStore(configStoreMock);

        ReleaseCleanRequest cleanRequest = new ReleaseCleanRequest();
        cleanRequest.setReleaseDescriptorBuilder(builder);

        // execute
        releaseManagerTest.clean(cleanRequest);

        // verify
        assertTrue(((ReleasePhaseStub) phaseStep1).isCleaned(), "step1 not cleaned");

        assertTrue(((ReleasePhaseStub) phaseStep2).isCleaned(), "step2 not cleaned");

        assertTrue(((ReleasePhaseStub) phaseStep3).isCleaned(), "step3 not cleaned");

        //        assertTrue("branch1 not cleaned", phaseBranch1.isCleaned());

        verify(configStoreMock).delete(any(ReleaseDescriptor.class));
        verifyNoMoreInteractions(configStoreMock);
    }

    private static List<MavenProject> createReactorProjects() throws IOException {
        MavenProject project = new MavenProject();

        File projectFile = getTestFile("target/dummy-project/pom.xml");
        if (!projectFile.exists()) {
            projectFile.getParentFile().mkdirs();
            projectFile.createNewFile();
        }
        project.setFile(projectFile);
        return Collections.singletonList(project);
    }

    @Test
    void testReleasePerformWithResult() throws Exception {

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        File checkoutDirectory = getTestFile("target/checkout-directory");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());
        builder.setWorkingDirectory(getTestPath("target/dummy-project"));

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        performRequest.setReactorProjects(createReactorProjects());

        ReleaseResult result = releaseManagerTest.performWithResult(performRequest);

        assertTrue(result.getOutput().length() > 0);
    }

    @Test
    void testReleaseConfigurationStoreReadFailureOnPerform() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());

        ReleaseDescriptorStore configStoreMock = mock(ReleaseDescriptorStore.class);
        when(configStoreMock.read(builder))
                .thenThrow(new ReleaseDescriptorStoreException("message", new IOException("ioExceptionMsg")));

        ((DefaultReleaseManager) releaseManagerTest).setConfigStore(configStoreMock);

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());

        // execute
        try {
            builder.setUseReleaseProfile(false);

            releaseManagerTest.perform(performRequest);
            fail("Should have failed to read configuration");
        } catch (ReleaseExecutionException e) {
            // good
            assertEquals(ReleaseDescriptorStoreException.class, e.getCause().getClass(), "check cause");
        }

        // verify
        verify(configStoreMock).read(builder);
        verifyNoMoreInteractions(configStoreMock);
    }

    @Test
    void testReleasePerformWithIncompletePrepare() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());

        ReleaseDescriptorStoreStub configStore = new ReleaseDescriptorStoreStub();
        builder.setCompletedPhase("scm-tag");
        ((DefaultReleaseManager) releaseManagerTest).setConfigStore(configStore);

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());

        try {
            builder.setUseReleaseProfile(false);

            releaseManagerTest.perform(performRequest);
            fail("Should have failed to perform");
        } catch (ReleaseFailureException e) {
            // good
            assertTrue(true);
        }
    }

    // MRELEASE-758: release:perform no longer removes release.properties
    @Test
    void testPerformWithDefaultClean() throws Exception {
        // prepare
        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setDryRun(true);
        performRequest.setReactorProjects(createReactorProjects());

        ReleaseManagerListener managerListener = mock(ReleaseManagerListener.class);
        performRequest.setReleaseManagerListener(managerListener);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());
        performRequest.setReleaseDescriptorBuilder(builder);

        // test
        releaseManagerTest.perform(performRequest);

        // verify
        verify(managerListener).phaseStart("verify-completed-prepare-phases");
        verify(managerListener).phaseStart("checkout-project-from-scm");
        verify(managerListener).phaseStart("run-perform-goals");
        verify(managerListener, times(3)).phaseEnd();

        // not part of actual test, but required to confirm 'no more interactions'
        verify(managerListener).goalStart(anyString(), any());
        verify(managerListener).goalEnd();

        verifyNoMoreInteractions(managerListener);
    }

    @Test
    void testNoScmUrlPerform() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setWorkingDirectory(getTestFile("target/test/checkout").getAbsolutePath());

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());

        try {
            builder.setUseReleaseProfile(false);

            releaseManagerTest.perform(performRequest);

            fail("perform should have failed");
        } catch (ReleaseFailureException e) {
            assertNull(e.getCause(), "check no cause");
        }
    }

    @Test
    void testScmExceptionThrown() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        File checkoutDirectory = getTestFile("target/checkout-directory");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());

        ScmProvider scmProviderMock = mock(ScmProvider.class);
        when(scmProviderMock.checkOut(
                        any(ScmRepository.class),
                        any(ScmFileSet.class),
                        any(ScmTag.class),
                        any(CommandParameters.class)))
                .thenThrow(new ScmException("..."));

        ((ScmManagerStub) scmManager).setScmProvider(scmProviderMock);

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        performRequest.setReactorProjects(createReactorProjects());

        // execute
        try {
            releaseManagerTest.perform(performRequest);

            fail("commit should have failed");
        } catch (ReleaseExecutionException e) {
            assertEquals(ScmException.class, e.getCause().getClass(), "check cause");
        }

        // verify
        verify(scmProviderMock)
                .checkOut(
                        any(ScmRepository.class), any(ScmFileSet.class),
                        any(ScmTag.class), any(CommandParameters.class));
        verifyNoMoreInteractions(scmProviderMock);
    }

    @Test
    void testScmResultFailure() throws Exception {

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        File checkoutDirectory = getTestFile("target/checkout-directory");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());
        builder.setWorkingDirectory(getTestPath("target/dummy-project"));

        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl("scm-url");

        providerStub.setCheckOutScmResult(new CheckOutScmResult("", "", "", false));

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        performRequest.setReactorProjects(createReactorProjects());

        try {
            releaseManagerTest.perform(performRequest);

            fail("commit should have failed");
        } catch (ReleaseScmCommandException e) {
            assertNull(e.getCause(), "check no other cause");
        }
    }

    // MRELEASE-1042
    @Test
    void testKeepProfilesOnPerform() throws Exception {
        // prepare
        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setDryRun(true);
        performRequest.setReactorProjects(createReactorProjects());

        ReleaseManagerListener managerListener = mock(ReleaseManagerListener.class);
        performRequest.setReleaseManagerListener(managerListener);

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setActivateProfiles(Arrays.asList("aProfile", "anotherOne"));
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());
        performRequest.setReleaseDescriptorBuilder(builder);

        ReleaseDescriptorBuilder secondBuilder = new ReleaseDescriptorBuilder();
        secondBuilder.setActivateProfiles(Arrays.asList("aProfile", "bProfile"));
        secondBuilder.setScmSourceUrl("scm-url");
        ReleaseDescriptorStore configStoreMock = mock(ReleaseDescriptorStore.class);
        when(configStoreMock.read(any(ReleaseDescriptorBuilder.class))).thenReturn(secondBuilder);
        ((DefaultReleaseManager) releaseManagerTest).setConfigStore(configStoreMock);

        // test
        ReleaseResult result = releaseManagerTest.performWithResult(performRequest);

        // verify
        assertTrue(result.getOutput().contains("-P aProfile,bProfile,anotherOne"));
    }

    @Test
    void testDetermineWorkingDirectory() throws Exception {
        DefaultReleaseManager defaultReleaseManager = new DefaultReleaseManager(
                Collections.emptyMap(), Collections.emptyMap(), mock(ReleaseDescriptorStore.class));

        File checkoutDir = getTestFile("target/checkout");
        FileUtils.forceDelete(checkoutDir);
        checkoutDir.mkdirs();

        File projectDir = getTestFile("target/checkout/my/project");
        projectDir.mkdirs();

        // only checkout dir
        assertEquals(checkoutDir, defaultReleaseManager.determineWorkingDirectory(checkoutDir, ""));
        assertEquals(checkoutDir, defaultReleaseManager.determineWorkingDirectory(checkoutDir, null));

        // checkout dir and relative path project dir
        assertEquals(projectDir, defaultReleaseManager.determineWorkingDirectory(checkoutDir, "my/project"));
        assertEquals(projectDir, defaultReleaseManager.determineWorkingDirectory(checkoutDir, "my/project/"));
        assertEquals(
                projectDir,
                defaultReleaseManager.determineWorkingDirectory(checkoutDir, "my" + File.separator + "project"));

        FileUtils.forceDelete(checkoutDir);
    }

    // MRELEASE-761
    @Test
    void testRollbackCall() throws Exception {

        ReleaseRollbackRequest rollbackRequest = new ReleaseRollbackRequest();
        rollbackRequest.setReleaseDescriptorBuilder(configStore.getReleaseConfiguration());

        releaseManagerTest.rollback(rollbackRequest);

        assertTrue(((ReleasePhaseStub) phaseRollbackPhase1).isExecuted(), "rollbackPhase1 executed");
    }

    // MRELEASE-765
    @Test
    void testUpdateVersionsCall() throws Exception {

        ReleaseUpdateVersionsRequest updateVersionsRequest = new ReleaseUpdateVersionsRequest();
        updateVersionsRequest.setReleaseDescriptorBuilder(configStore.getReleaseConfiguration());
        updateVersionsRequest.setUserProperties(new Properties());

        releaseManagerTest.updateVersions(updateVersionsRequest);

        assertTrue(((ReleasePhaseStub) phaseUpdateVersionsPhase1).isExecuted(), "updateVersionsPhase1 executed");
    }
}
