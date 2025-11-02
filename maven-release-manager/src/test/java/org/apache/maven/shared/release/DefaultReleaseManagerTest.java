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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
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
import org.apache.maven.shared.release.stubs.ScmManagerStub;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
public class DefaultReleaseManagerTest extends PlexusJUnit4TestCase {
    private ReleaseDescriptorStoreStub configStore;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        configStore = (ReleaseDescriptorStoreStub) lookup(ReleaseDescriptorStore.class, "stub");
    }

    @Override
    protected Module[] getCustomModules() {
        return new Module[] {
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(ScmManager.class).toInstance(new ScmManagerStub());
                    bind(ReleaseDescriptorStore.class).toInstance(new ReleaseDescriptorStoreStub());
                }
            }
        };
    }

    @Test
    public void testPrepareNoCompletedPhase() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase(null);

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setUserProperties(new Properties());

        releaseManager.prepare(prepareRequest);

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step1");
        assertTrue(phase.isExecuted(), "step1 executed");
        assertFalse(phase.isSimulated(), "step1 not simulated");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step2");
        assertTrue(phase.isExecuted(), "step2 executed");
        assertFalse(phase.isSimulated(), "step2 not simulated");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step3");
        assertTrue(phase.isExecuted(), "step3 executed");
        assertFalse(phase.isSimulated(), "step3 not simulated");
    }

    @Test
    public void testPrepareCompletedPhase() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("step1");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setUserProperties(new Properties());

        releaseManager.prepare(prepareRequest);

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step1");
        assertFalse(phase.isExecuted(), "step1 not executed");
        assertFalse(phase.isSimulated(), "step1 not simulated");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step2");
        assertTrue(phase.isExecuted(), "step2 executed");
        assertFalse(phase.isSimulated(), "step2 not simulated");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step3");
        assertTrue(phase.isExecuted(), "step3 executed");
        assertFalse(phase.isSimulated(), "step3 not simulated");
    }

    @Test
    public void testPrepareCompletedPhaseNoResume() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("step1");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(false);
        prepareRequest.setResume(false);
        prepareRequest.setUserProperties(new Properties());

        releaseManager.prepare(prepareRequest);

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step1");
        assertFalse(phase.isExecuted(), "step1 executed");
        assertFalse(phase.isSimulated(), "step1 not simulated");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step2");
        assertTrue(phase.isExecuted(), "step2 executed");
        assertFalse(phase.isSimulated(), "step2 not simulated");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step3");
        assertTrue(phase.isExecuted(), "step3 executed");
        assertFalse(phase.isSimulated(), "step3 not simulated");
    }

    @Test
    public void testPrepareCompletedAllPhases() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("step3");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setUserProperties(new Properties());

        releaseManager.prepare(prepareRequest);

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step1");
        assertFalse(phase.isExecuted(), "step1 not executed");
        assertFalse(phase.isSimulated(), "step1 not simulated");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step2");
        assertFalse(phase.isExecuted(), "step2 not executed");
        assertFalse(phase.isSimulated(), "step2 not simulated");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step3");
        assertFalse(phase.isExecuted(), "step3 not executed");
        assertFalse(phase.isSimulated(), "step3 not simulated");
    }

    @Test
    public void testPrepareInvalidCompletedPhase() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("foo");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setUserProperties(new Properties());

        releaseManager.prepare(prepareRequest);

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step1");
        assertTrue(phase.isExecuted(), "step1 executed");
        assertFalse(phase.isSimulated(), "step1 not simulated");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step2");
        assertTrue(phase.isExecuted(), "step2 executed");
        assertFalse(phase.isSimulated(), "step2 not simulated");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step3");
        assertTrue(phase.isExecuted(), "step3 executed");
        assertFalse(phase.isSimulated(), "step3 not simulated");
    }

    @Test
    public void testPrepareSimulateNoCompletedPhase() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase(null);

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(true);
        prepareRequest.setResume(true);
        prepareRequest.setUserProperties(new Properties());

        releaseManager.prepare(prepareRequest);

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step1");
        assertTrue(phase.isSimulated(), "step1 simulated");
        assertFalse(phase.isExecuted(), "step1 not executed");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step2");
        assertTrue(phase.isSimulated(), "step2 simulated");
        assertFalse(phase.isExecuted(), "step2 not executed");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step3");
        assertTrue(phase.isSimulated(), "step3 simulated");
        assertFalse(phase.isExecuted(), "step3 not executed");
    }

    @Test
    public void testPrepareSimulateCompletedPhase() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("step1");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(true);
        prepareRequest.setResume(true);
        prepareRequest.setUserProperties(new Properties());

        releaseManager.prepare(prepareRequest);

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step1");
        assertFalse(phase.isSimulated(), "step1 not simulated");
        assertFalse(phase.isExecuted(), "step1 not executed");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step2");
        assertTrue(phase.isSimulated(), "step2 simulated");
        assertFalse(phase.isExecuted(), "step2 not executed");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step3");
        assertTrue(phase.isSimulated(), "step3 simulated");
        assertFalse(phase.isExecuted(), "step3 not executed");
    }

    @Test
    public void testPrepareSimulateCompletedAllPhases() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("step3");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(true);
        prepareRequest.setResume(true);
        prepareRequest.setUserProperties(new Properties());

        releaseManager.prepare(prepareRequest);

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step1");
        assertFalse(phase.isSimulated(), "step1 not simulated");
        assertFalse(phase.isExecuted(), "step1 not executed");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step2");
        assertFalse(phase.isSimulated(), "step2 not simulated");
        assertFalse(phase.isExecuted(), "step2 not executed");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step3");
        assertFalse(phase.isSimulated(), "step3 not simulated");
        assertFalse(phase.isExecuted(), "step3 not executed");
    }

    @Test
    public void testPrepareSimulateInvalidCompletedPhase() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase("foo");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(true);
        prepareRequest.setResume(true);
        prepareRequest.setUserProperties(new Properties());

        releaseManager.prepare(prepareRequest);

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step1");
        assertTrue(phase.isSimulated(), "step1 simulated");
        assertFalse(phase.isExecuted(), "step1 not executed");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step2");
        assertTrue(phase.isSimulated(), "step2 simulated");
        assertFalse(phase.isExecuted(), "step2 not executed");
        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step3");
        assertTrue(phase.isSimulated(), "step3 simulated");
        assertFalse(phase.isExecuted(), "step3 not executed");
    }

    @Disabled("This is testing messed up XML?")
    @Test
    public void testPrepareUnknownPhaseConfigured() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "bad-phase-configured");

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setReleaseStrategyId("foo");
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setUserProperties(new Properties());

        try {
            releaseManager.prepare(prepareRequest);
            fail("Should have failed to find a phase");
        } catch (ReleaseExecutionException e) {
            // good
        }
    }

    @Test
    public void testReleaseConfigurationStoreReadFailure() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup(ReleaseManager.class, "test");

        ReleaseDescriptorStore configStoreMock = mock(ReleaseDescriptorStore.class);
        when(configStoreMock.read(builder))
                .thenThrow(new ReleaseDescriptorStoreException("message", new IOException("ioExceptionMsg")));

        releaseManager.setConfigStore(configStoreMock);

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setUserProperties(new Properties());

        // execute
        try {
            releaseManager.prepare(prepareRequest);
            fail("Should have failed to read configuration");
        } catch (ReleaseExecutionException e) {
            // good
            assertEquals(
                    ReleaseDescriptorStoreException.class,
                    e.getCause().getClass(),
                    "check cause");
        }

        // verify
        verify(configStoreMock).read(builder);
        verifyNoMoreInteractions(configStoreMock);
    }

    @Test
    public void testReleaseConfigurationStoreWriteFailure() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup(ReleaseManager.class, "test");

        ReleaseDescriptorStore configStoreMock = mock(ReleaseDescriptorStore.class);
        doThrow(new ReleaseDescriptorStoreException("message", new IOException("ioExceptionMsg")))
                .when(configStoreMock)
                .write(any(ReleaseDescriptor.class));

        releaseManager.setConfigStore(configStoreMock);

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder(builder);
        prepareRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        prepareRequest.setDryRun(false);
        prepareRequest.setResume(false);
        prepareRequest.setUserProperties(new Properties());

        // execute
        try {
            releaseManager.prepare(prepareRequest);
            fail("Should have failed to read configuration");
        } catch (ReleaseExecutionException e) {
            // good
            assertEquals(
                    ReleaseDescriptorStoreException.class,
                    e.getCause().getClass(),
                    "check cause");
        }

        // verify
        verify(configStoreMock).write(any(ReleaseDescriptor.class));
        verifyNoMoreInteractions(configStoreMock);
    }

    @Test
    public void testReleaseConfigurationStoreClean() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup(ReleaseManager.class, "test");

        ReleaseDescriptorStore configStoreMock = mock(ReleaseDescriptorStore.class);

        releaseManager.setConfigStore(configStoreMock);

        ReleaseCleanRequest cleanRequest = new ReleaseCleanRequest();
        cleanRequest.setReleaseDescriptorBuilder(builder);

        // execute
        releaseManager.clean(cleanRequest);

        // verify
        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step1");
        assertTrue(phase.isCleaned(), "step1 not cleaned");

        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step2");
        assertTrue(phase.isCleaned(), "step2 not cleaned");

        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "step3");
        assertTrue(phase.isCleaned(), "step3 not cleaned");

        phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "branch1");
        assertTrue(phase.isCleaned(), "branch1 not cleaned");

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
    public void testReleasePerformWithResult() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        File checkoutDirectory = getTestFile("target/checkout-directory");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());
        builder.setWorkingDirectory(getTestPath("target/dummy-project"));

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        performRequest.setReactorProjects(createReactorProjects());

        ReleaseResult result = releaseManager.performWithResult(performRequest);

        assertTrue(result.getOutput().length() > 0);
    }

    @Test
    public void testReleaseConfigurationStoreReadFailureOnPerform() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup(ReleaseManager.class, "test");

        ReleaseDescriptorStore configStoreMock = mock(ReleaseDescriptorStore.class);
        when(configStoreMock.read(builder))
                .thenThrow(new ReleaseDescriptorStoreException("message", new IOException("ioExceptionMsg")));

        releaseManager.setConfigStore(configStoreMock);

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());

        // execute
        try {
            builder.setUseReleaseProfile(false);

            releaseManager.perform(performRequest);
            fail("Should have failed to read configuration");
        } catch (ReleaseExecutionException e) {
            // good
            assertEquals(
                    ReleaseDescriptorStoreException.class,
                    e.getCause().getClass(),
                    "check cause");
        }

        // verify
        verify(configStoreMock).read(builder);
        verifyNoMoreInteractions(configStoreMock);
    }

    @Test
    public void testReleasePerformWithIncompletePrepare() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setWorkingDirectory(getTestFile("target/working-directory").getAbsolutePath());

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup(ReleaseManager.class, "test");

        ReleaseDescriptorStoreStub configStore = new ReleaseDescriptorStoreStub();
        builder.setCompletedPhase("scm-tag");
        releaseManager.setConfigStore(configStore);

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());

        try {
            builder.setUseReleaseProfile(false);

            releaseManager.perform(performRequest);
            fail("Should have failed to perform");
        } catch (ReleaseFailureException e) {
            // good
            assertTrue(true);
        }
    }

    // MRELEASE-758: release:perform no longer removes release.properties
    @Test
    public void testPerformWithDefaultClean() throws Exception {
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

        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        // test
        releaseManager.perform(performRequest);

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
    public void testNoScmUrlPerform() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setWorkingDirectory(getTestFile("target/test/checkout").getAbsolutePath());

        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());

        try {
            builder.setUseReleaseProfile(false);

            releaseManager.perform(performRequest);

            fail("perform should have failed");
        } catch (ReleaseFailureException e) {
            assertNull(e.getCause(), "check no cause");
        }
    }

    @Test
    public void testScmExceptionThrown() throws Exception {
        // prepare
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

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

        ScmManagerStub stub = (ScmManagerStub) lookup(ScmManager.class);
        stub.setScmProvider(scmProviderMock);

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        performRequest.setReactorProjects(createReactorProjects());

        // execute
        try {
            releaseManager.perform(performRequest);

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
    public void testScmResultFailure() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        File checkoutDirectory = getTestFile("target/checkout-directory");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());
        builder.setWorkingDirectory(getTestPath("target/dummy-project"));

        ScmManager scmManager = (ScmManager) lookup(ScmManager.class);
        ScmProviderStub providerStub = (ScmProviderStub) scmManager.getProviderByUrl("scm-url");

        providerStub.setCheckOutScmResult(new CheckOutScmResult("", "", "", false));

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder(builder);
        performRequest.setReleaseEnvironment(new DefaultReleaseEnvironment());
        performRequest.setReactorProjects(createReactorProjects());

        try {
            releaseManager.perform(performRequest);

            fail("commit should have failed");
        } catch (ReleaseScmCommandException e) {
            assertNull(e.getCause(), "check no other cause");
        }
    }

    // MRELEASE-1042
    @Test
    public void testKeepProfilesOnPerform() throws Exception {
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

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup(ReleaseManager.class, "test");

        ReleaseDescriptorBuilder secondBuilder = new ReleaseDescriptorBuilder();
        secondBuilder.setActivateProfiles(Arrays.asList("aProfile", "bProfile"));
        secondBuilder.setScmSourceUrl("scm-url");
        ReleaseDescriptorStore configStoreMock = mock(ReleaseDescriptorStore.class);
        when(configStoreMock.read(any(ReleaseDescriptorBuilder.class))).thenReturn(secondBuilder);
        releaseManager.setConfigStore(configStoreMock);

        // test
        ReleaseResult result = releaseManager.performWithResult(performRequest);

        // verify
        assertTrue(result.getOutput().contains("-P aProfile,bProfile,anotherOne"));
    }

    @Test
    public void testDetermineWorkingDirectory() throws Exception {
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
    public void testRollbackCall() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseRollbackRequest rollbackRequest = new ReleaseRollbackRequest();
        rollbackRequest.setReleaseDescriptorBuilder(configStore.getReleaseConfiguration());

        releaseManager.rollback(rollbackRequest);

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "rollbackPhase1");

        assertTrue(phase.isExecuted(), "rollbackPhase1 executed");
    }

    // MRELEASE-765
    @Test
    public void testUpdateVersionsCall() throws Exception {
        ReleaseManager releaseManager = lookup(ReleaseManager.class, "test");

        ReleaseUpdateVersionsRequest updateVersionsRequest = new ReleaseUpdateVersionsRequest();
        updateVersionsRequest.setReleaseDescriptorBuilder(configStore.getReleaseConfiguration());
        updateVersionsRequest.setUserProperties(new Properties());

        releaseManager.updateVersions(updateVersionsRequest);

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup(ReleasePhase.class, "updateVersionsPhase1");

        assertTrue(phase.isExecuted(), "updateVersionsPhase1 executed");
    }
}
