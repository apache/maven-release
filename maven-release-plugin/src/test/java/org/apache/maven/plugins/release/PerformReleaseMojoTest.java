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
package org.apache.maven.plugins.release;

import javax.inject.Inject;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.ReleasePerformRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test release:perform.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@ExtendWith(MockitoExtension.class)
@MojoTest
class PerformReleaseMojoTest {

    @Mock
    private ReleaseManager releaseManagerMock;

    @Inject
    private MavenProject mavenProject;

    @Inject
    private MavenSession mavenSession;

    @Provides
    private ReleaseManager releaseManager() {
        return releaseManagerMock;
    }

    @BeforeEach
    void setup() {
        when(mavenProject.getFile()).thenReturn(new File("pom.xml"));

        when(mavenProject.getGroupId()).thenReturn("groupId");
        when(mavenProject.getArtifactId()).thenReturn("artifactId");
        when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");

        when(mavenSession.getProjects()).thenReturn(Collections.singletonList(mavenProject));
        when(mavenSession.getRequest()).thenReturn(new DefaultMavenExecutionRequest());
    }

    @Test
    @Basedir("/mojos/perform")
    @InjectMojo(goal = "perform", pom = "perform.xml")
    void testPerform(PerformReleaseMojo mojo) throws Exception {
        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(releaseManagerMock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());

        verifyNoMoreInteractions(releaseManagerMock);
    }

    @Test
    @Basedir("/mojos/perform")
    @InjectMojo(goal = "perform", pom = "perform-with-flat-structure.xml")
    void testPerformWithFlatStructure(PerformReleaseMojo mojo) throws Exception {
        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(releaseManagerMock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy", releaseDescriptor.getPerformGoals());
        assertEquals(
                "scm:svn:file://localhost/target/svnroot/flat-multi-module/trunk/root-project",
                releaseDescriptor.getScmSourceUrl());

        verifyNoMoreInteractions(releaseManagerMock);
    }

    @Test
    @Basedir("/mojos/perform")
    @InjectMojo(goal = "perform", pom = "perform-without-site.xml")
    void testPerformWithoutSite(PerformReleaseMojo mojo) throws Exception {
        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(releaseManagerMock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());
        verifyNoMoreInteractions(releaseManagerMock);

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy", releaseDescriptor.getPerformGoals());
    }

    @Test
    @Basedir("/mojos/perform")
    @InjectMojo(goal = "perform", pom = "perform.xml")
    void testPerformWithExecutionException(PerformReleaseMojo mojo) throws Exception {
        doThrow(new ReleaseExecutionException("..."))
                .when(releaseManagerMock)
                .perform(isA(ReleasePerformRequest.class));

        // execute
        try {
            mojo.execute();

            fail("Should have thrown an exception");
        } catch (MojoExecutionException e) {
            assertEquals(ReleaseExecutionException.class, e.getCause().getClass(), "Check cause");
        }

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(releaseManagerMock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());

        verifyNoMoreInteractions(releaseManagerMock);
    }

    @Test
    @Basedir("/mojos/perform")
    @InjectMojo(goal = "perform", pom = "perform.xml")
    void testPerformWithExecutionFailure(PerformReleaseMojo mojo) throws Exception {
        ReleaseFailureException cause = new ReleaseFailureException("...");
        doThrow(cause).when(releaseManagerMock).perform(isA(ReleasePerformRequest.class));

        mojo.setReleaseManager(releaseManagerMock);

        // execute
        try {
            mojo.execute();

            fail("Should have thrown an exception");
        } catch (MojoFailureException e) {
            assertEquals(cause, e.getCause(), "Check cause exists");
        }

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(releaseManagerMock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());

        verifyNoMoreInteractions(releaseManagerMock);
    }

    @Test
    @Basedir("/mojos/perform")
    @InjectMojo(goal = "perform", pom = "perform-with-scm.xml")
    void testPerformWithScm(PerformReleaseMojo mojo) throws Exception {
        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(releaseManagerMock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());
        assertEquals("scm-url", releaseDescriptor.getScmSourceUrl());

        verifyNoMoreInteractions(releaseManagerMock);
    }

    @Test
    @Basedir("/mojos/perform")
    @InjectMojo(goal = "perform", pom = "perform.xml")
    void testPerformWithProfiles(PerformReleaseMojo mojo) throws Exception {
        MavenExecutionRequest request = mock(MavenExecutionRequest.class);
        when(request.getActiveProfiles()).thenReturn(Arrays.asList("prof1", "2prof"));
        when(mavenSession.getRequest()).thenReturn(request);

        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(releaseManagerMock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());
        assertTrue(releaseDescriptor.getActivateProfiles().contains("prof1"));
        assertTrue(releaseDescriptor.getActivateProfiles().contains("2prof"));

        verifyNoMoreInteractions(releaseManagerMock);
    }

    @Test
    @Basedir("/mojos/perform")
    @InjectMojo(goal = "perform", pom = "perform-with-args.xml")
    void testPerformWithProfilesAndArguments(PerformReleaseMojo mojo) throws Exception {
        MavenExecutionRequest request = mock(MavenExecutionRequest.class);
        when(request.getActiveProfiles()).thenReturn(Arrays.asList("prof1", "2prof"));
        when(mavenSession.getRequest()).thenReturn(request);

        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(releaseManagerMock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());
        assertTrue(releaseDescriptor.getActivateProfiles().contains("prof1"));
        assertTrue(releaseDescriptor.getActivateProfiles().contains("2prof"));
        assertEquals("-Dmaven.test.skip=true", releaseDescriptor.getAdditionalArguments());

        verifyNoMoreInteractions(releaseManagerMock);
    }

    @Test
    @Basedir("/mojos/perform")
    @InjectMojo(goal = "perform", pom = "perform-with-multiline-goals.xml")
    void testPerformWithMultilineGoals(PerformReleaseMojo mojo) throws Exception {
        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(releaseManagerMock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());

        verifyNoMoreInteractions(releaseManagerMock);
    }
}
