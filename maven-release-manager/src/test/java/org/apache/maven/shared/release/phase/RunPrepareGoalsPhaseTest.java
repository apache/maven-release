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
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.apache.maven.shared.release.stubs.MavenExecutorWrapper;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.codehaus.plexus.testing.PlexusExtension.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test the simple test running phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@PlexusTest
class RunPrepareGoalsPhaseTest {
    @Inject
    @Named("run-preparation-goals")
    private RunPrepareGoalsPhase phase;

    @Inject
    @Named("wrapper")
    private MavenExecutorWrapper mavenExecutorWrapper;

    private DefaultReleaseEnvironment releaseEnvironment;

    @BeforeEach
    void setUp() throws Exception {

        releaseEnvironment = new DefaultReleaseEnvironment();
        releaseEnvironment.setMavenExecutorId("wrapper");
    }

    @Test
    void testExecute() throws ReleaseExecutionException, MavenExecutorException {
        // prepare
        File testFile = getTestFile("target/working-directory");

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setPreparationGoals("clean integration-test");
        builder.setWorkingDirectory(testFile.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);

        mavenExecutorWrapper.setMavenExecutor(mock);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), releaseEnvironment, (List<MavenProject>) null);

        // verify
        verify(mock)
                .executeGoals(
                        eq(testFile),
                        eq("clean integration-test"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isA(ReleaseResult.class));
        verifyNoMoreInteractions(mock);
    }

    @Test
    void testSimulate() throws ReleaseExecutionException, MavenExecutorException {
        // prepare
        File testFile = getTestFile("target/working-directory");

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setPreparationGoals("clean integration-test");
        builder.setWorkingDirectory(testFile.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);

        mavenExecutorWrapper.setMavenExecutor(mock);

        // execute
        phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), releaseEnvironment, null);

        // verify
        verify(mock)
                .executeGoals(
                        eq(testFile),
                        eq("clean integration-test"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isA(ReleaseResult.class));
        verifyNoMoreInteractions(mock);
    }

    @Test
    void testExecuteException() throws MavenExecutorException {
        // prepare
        File testFile = getTestFile("target/working-directory");

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setPreparationGoals("clean integration-test");
        builder.setWorkingDirectory(testFile.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);
        doThrow(new MavenExecutorException("...", new Exception()))
                .when(mock)
                .executeGoals(
                        eq(testFile),
                        eq("clean integration-test"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isA(ReleaseResult.class));

        mavenExecutorWrapper.setMavenExecutor(mock);

        // execute
        try {
            phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), releaseEnvironment, (List<MavenProject>) null);

            fail("Should have thrown an exception");
        } catch (ReleaseExecutionException e) {
            assertEquals(MavenExecutorException.class, e.getCause().getClass(), "Check cause");
        }

        // verify
        verify(mock)
                .executeGoals(
                        eq(testFile),
                        eq("clean integration-test"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isA(ReleaseResult.class));
        verifyNoMoreInteractions(mock);
    }

    @Test
    void testSimulateException() throws MavenExecutorException {
        // prepare
        File testFile = getTestFile("target/working-directory");

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setPreparationGoals("clean integration-test");
        builder.setWorkingDirectory(testFile.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);
        doThrow(new MavenExecutorException("...", new Exception()))
                .when(mock)
                .executeGoals(
                        eq(testFile),
                        eq("clean integration-test"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isA(ReleaseResult.class));

        mavenExecutorWrapper.setMavenExecutor(mock);

        // execute
        try {
            phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), releaseEnvironment, null);

            fail("Should have thrown an exception");
        } catch (ReleaseExecutionException e) {
            assertEquals(MavenExecutorException.class, e.getCause().getClass(), "Check cause");
        }

        // verify
        verify(mock)
                .executeGoals(
                        eq(testFile),
                        eq("clean integration-test"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        isNull(),
                        isNull(),
                        isA(ReleaseResult.class));
        verifyNoMoreInteractions(mock);
    }

    @Test
    void testEmptyGoals() throws Exception {
        // prepare
        File testFile = getTestFile("target/working-directory");

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setPreparationGoals("");
        builder.setWorkingDirectory(testFile.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);

        mavenExecutorWrapper.setMavenExecutor(mock);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), releaseEnvironment, (List<MavenProject>) null);

        // verify
        // no invocations of mock
        verifyNoMoreInteractions(mock);
    }
}
