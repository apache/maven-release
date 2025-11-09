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
import java.util.Collections;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
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
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test release:stage.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@ExtendWith(MockitoExtension.class)
@MojoTest
class StageReleaseMojoTest {
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
        when(mavenSession.getProjects()).thenReturn(Collections.singletonList(mavenProject));
    }

    private void prepareMocks() {
        when(mavenSession.getRequest()).thenReturn(new DefaultMavenExecutionRequest());

        when(mavenProject.getGroupId()).thenReturn("groupId");
        when(mavenProject.getArtifactId()).thenReturn("artifactId");
        when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
    }

    @Test
    @Basedir("/mojos/stage")
    @InjectMojo(goal = "stage", pom = "stage.xml")
    void testStage(StageReleaseMojo mojo) throws Exception {
        prepareMocks();

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
        assertEquals("deploy site:stage-deploy", releaseDescriptor.getPerformGoals());
        assertEquals("-DskipTests -DaltDeploymentRepository=\"staging\"", releaseDescriptor.getAdditionalArguments());

        verifyNoMoreInteractions(releaseManagerMock);
    }

    @Test
    @Basedir("/mojos/stage")
    @InjectMojo(goal = "stage", pom = "stage.xml")
    void testCreateGoals(StageReleaseMojo mojo) {
        mojo.createGoals();
        assertEquals("deploy site:stage-deploy", mojo.goals);
        mojo.goals = "deploy site:deploy";
        mojo.createGoals();
        assertEquals("deploy site:stage-deploy", mojo.goals);
    }

    @Test
    @Basedir("/mojos/stage")
    @InjectMojo(goal = "stage", pom = "stage.xml")
    void testCreateArguments(StageReleaseMojo mojo) {
        prepareMocks();

        mojo.setDeploymentRepository();
        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                mojo.createReleaseDescriptor().build();
        assertEquals("-DskipTests -DaltDeploymentRepository=\"staging\"", releaseDescriptor.getAdditionalArguments());
    }
}
