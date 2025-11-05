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
import java.util.Collections;
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
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 */
@PlexusTest
class RunPerformGoalsPhaseTest {
    @Inject
    @Named("run-perform-goals")
    private RunPerformGoalsPhase phase;

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
    void testExecuteException() throws Exception {
        // prepare
        File testFile = getTestFile("target/checkout-directory");

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setPerformGoals("goal1 goal2");
        builder.setCheckoutDirectory(testFile.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);

        mavenExecutorWrapper.setMavenExecutor(mock);

        doThrow(new MavenExecutorException("...", new Exception()))
                .when(mock)
                .executeGoals(
                        eq(testFile),
                        eq("goal1 goal2"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        eq("-DperformRelease=true -f pom.xml"),
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
                        eq("goal1 goal2"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        eq("-DperformRelease=true -f pom.xml"),
                        isNull(),
                        isA(ReleaseResult.class));
        verifyNoMoreInteractions(mock);
    }

    @Test
    void testCustomPomFile() throws Exception {
        // prepare
        File testFile = getTestFile("target/checkout-directory");
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setPerformGoals("goal1 goal2");
        builder.setPomFileName("pom1.xml");
        builder.setCheckoutDirectory(testFile.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);

        mavenExecutorWrapper.setMavenExecutor(mock);

        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), releaseEnvironment, (List<MavenProject>) null);

        verify(mock)
                .executeGoals(
                        eq(testFile),
                        eq("goal1 goal2"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        eq("-DperformRelease=true -f pom1.xml"),
                        eq("pom1.xml"),
                        isA(ReleaseResult.class));

        verifyNoMoreInteractions(mock);
    }

    @Test
    void testReleasePerformWithArgumentsNoReleaseProfile() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setAdditionalArguments("-Dmaven.test.skip=true");
        builder.setPerformGoals("goal1 goal2");
        File checkoutDirectory = getTestFile("target/checkout-directory");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);
        mavenExecutorWrapper.setMavenExecutor(mock);

        builder.setUseReleaseProfile(false);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), releaseEnvironment, createReactorProjects());

        // verify
        verify(mock)
                .executeGoals(
                        eq(checkoutDirectory),
                        eq("goal1 goal2"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        eq("-Dmaven.test.skip=true -f pom.xml"),
                        isNull(),
                        isA(ReleaseResult.class));
    }

    @Test
    void testReleasePerform() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setPerformGoals("goal1 goal2");
        File checkoutDirectory = getTestFile("target/checkout-directory");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);
        mavenExecutorWrapper.setMavenExecutor(mock);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), releaseEnvironment, createReactorProjects());

        // verify
        verify(mock)
                .executeGoals(
                        eq(checkoutDirectory),
                        eq("goal1 goal2"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        eq("-DperformRelease=true -f pom.xml"),
                        isNull(),
                        isA(ReleaseResult.class));
    }

    @Test
    void testReleasePerformNoReleaseProfile() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setPerformGoals("goal1 goal2");
        File checkoutDirectory = getTestFile("target/checkout-directory");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);
        mavenExecutorWrapper.setMavenExecutor(mock);

        builder.setUseReleaseProfile(false);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), releaseEnvironment, createReactorProjects());

        // verify
        verify(mock)
                .executeGoals(
                        eq(checkoutDirectory),
                        eq("goal1 goal2"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        eq("-f pom.xml"),
                        isNull(),
                        isA(ReleaseResult.class));
    }

    @Test
    void testReleasePerformWithArguments() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setAdditionalArguments("-Dmaven.test.skip=true");
        builder.setPerformGoals("goal1 goal2");
        File checkoutDirectory = getTestFile("target/checkout-directory");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);
        mavenExecutorWrapper.setMavenExecutor(mock);

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), releaseEnvironment, createReactorProjects());

        // verify
        verify(mock)
                .executeGoals(
                        eq(checkoutDirectory),
                        eq("goal1 goal2"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        eq("-Dmaven.test.skip=true -DperformRelease=true -f pom.xml"),
                        isNull(),
                        isA(ReleaseResult.class));
    }

    @Test
    void testReleasePerformWithReleasePropertiesCompleted() throws Exception {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm-url");
        builder.setPerformGoals("goal1 goal2");
        File checkoutDirectory = getTestFile("target/checkout-directory");
        builder.setCheckoutDirectory(checkoutDirectory.getAbsolutePath());

        MavenExecutor mock = mock(MavenExecutor.class);
        mavenExecutorWrapper.setMavenExecutor(mock);

        builder.setCompletedPhase("end-release");

        // execute
        phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), releaseEnvironment, createReactorProjects());

        // verify
        verify(mock)
                .executeGoals(
                        eq(checkoutDirectory),
                        eq("goal1 goal2"),
                        isA(ReleaseEnvironment.class),
                        eq(true),
                        eq("-DperformRelease=true -f pom.xml"),
                        isNull(),
                        isA(ReleaseResult.class));
    }

    private static List<MavenProject> createReactorProjects() {
        MavenProject project = new MavenProject();
        project.setFile(getTestFile("target/dummy-project/pom.xml"));
        return Collections.singletonList(project);
    }
}
