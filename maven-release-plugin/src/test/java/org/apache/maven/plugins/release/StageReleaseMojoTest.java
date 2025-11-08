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

import java.io.File;

import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.ReleasePerformRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test release:stage.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class StageReleaseMojoTest extends AbstractMojoTestCase {
    private File workingDirectory;

    public void testStage() throws Exception {
        StageReleaseMojo mojo = getMojoWithProjectSite("stage.xml");

        ReleaseManager mock = mock(ReleaseManager.class);
        mojo.setReleaseManager(mock);

        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(mock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy site:stage-deploy", releaseDescriptor.getPerformGoals());
        assertEquals("-DskipTests -DaltDeploymentRepository=\"staging\"", releaseDescriptor.getAdditionalArguments());

        verifyNoMoreInteractions(mock);
    }

    public void testCreateGoals() throws Exception {
        StageReleaseMojo mojo = getMojoWithProjectSite("stage.xml");
        mojo.createGoals();
        assertEquals("deploy site:stage-deploy", mojo.goals);
        mojo.goals = "deploy site:deploy";
        mojo.createGoals();
        assertEquals("deploy site:stage-deploy", mojo.goals);
    }

    public void testCreateArguments() throws Exception {
        StageReleaseMojo mojo = getMojoWithProjectSite("stage.xml");
        mojo.setDeploymentRepository();
        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                mojo.createReleaseDescriptor().build();
        assertEquals("-DskipTests -DaltDeploymentRepository=\"staging\"", releaseDescriptor.getAdditionalArguments());
    }

    private StageReleaseMojo getMojoWithProjectSite(String fileName) throws Exception {
        StageReleaseMojo mojo = lookupMojo("stage", new File(workingDirectory, fileName));
        mojo.setBasedir(workingDirectory);
        mojo.setPomFileName("pom.xml");

        MavenProject project = getVariableValueFromObject(mojo, "project");
        DistributionManagement distributionManagement = new DistributionManagement();
        distributionManagement.setSite(new Site());
        project.setDistributionManagement(distributionManagement);

        setVariableValueToObject(mojo, "session", newMavenSession(project));

        return mojo;
    }

    protected void setUp() throws Exception {
        super.setUp();
        workingDirectory = getTestFile("target/test-classes/mojos/stage");
    }
}
