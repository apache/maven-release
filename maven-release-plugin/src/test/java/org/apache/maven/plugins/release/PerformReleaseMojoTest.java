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
import java.util.Arrays;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.ReleasePerformRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test release:perform.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PerformReleaseMojoTest extends AbstractMojoTestCase {
    private File workingDirectory;

    public void testPerform() throws Exception {
        PerformReleaseMojo mojo = getMojoWithProjectSite("perform.xml");

        ReleaseManager mock = mock(ReleaseManager.class);
        mojo.setReleaseManager(mock);

        // execute
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
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());

        verifyNoMoreInteractions(mock);
    }

    public void testPerformWithFlatStructure() throws Exception {
        PerformReleaseMojo mojo = getMojoWithProjectSite("perform-with-flat-structure.xml");

        ReleaseManager mock = mock(ReleaseManager.class);
        mojo.setReleaseManager(mock);

        // execute
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
        assertEquals("deploy", releaseDescriptor.getPerformGoals());
        assertEquals(
                "scm:svn:file://localhost/target/svnroot/flat-multi-module/trunk/root-project",
                releaseDescriptor.getScmSourceUrl());

        verifyNoMoreInteractions(mock);
    }

    public void testPerformWithoutSite() throws Exception {
        File testFileDirectory = getTestFile("target/test-classes/mojos/perform/");
        PerformReleaseMojo mojo = lookupMojo("perform", new File(testFileDirectory, "perform-without-site.xml"));
        mojo.setBasedir(testFileDirectory);
        mojo.setPomFileName("pom.xml");

        MavenProject project = getVariableValueFromObject(mojo, "project");
        setVariableValueToObject(mojo, "session", newMavenSession(project));

        ReleaseManager mock = mock(ReleaseManager.class);
        mojo.setReleaseManager(mock);

        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(mock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());
        verifyNoMoreInteractions(mock);

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy", releaseDescriptor.getPerformGoals());
    }

    private PerformReleaseMojo getMojoWithProjectSite(String fileName) throws Exception {
        PerformReleaseMojo mojo = lookupMojo("perform", new File(workingDirectory, fileName));
        mojo.setBasedir(workingDirectory);
        mojo.setPomFileName(fileName);

        MavenProject project = getVariableValueFromObject(mojo, "project");
        DistributionManagement distributionManagement = new DistributionManagement();
        distributionManagement.setSite(new Site());
        project.setDistributionManagement(distributionManagement);

        setVariableValueToObject(mojo, "session", newMavenSession(project));

        return mojo;
    }

    public void testPerformWithExecutionException() throws Exception {
        PerformReleaseMojo mojo = getMojoWithProjectSite("perform.xml");

        ReleaseManager mock = mock(ReleaseManager.class);
        doThrow(new ReleaseExecutionException("...")).when(mock).perform(isA(ReleasePerformRequest.class));
        mojo.setReleaseManager(mock);

        // execute
        try {
            mojo.execute();

            fail("Should have thrown an exception");
        } catch (MojoExecutionException e) {
            assertEquals(
                    "Check cause", ReleaseExecutionException.class, e.getCause().getClass());
        }

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(mock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());

        verifyNoMoreInteractions(mock);
    }

    public void testPerformWithExecutionFailure() throws Exception {
        PerformReleaseMojo mojo = getMojoWithProjectSite("perform.xml");

        ReleaseManager mock = mock(ReleaseManager.class);
        ReleaseFailureException cause = new ReleaseFailureException("...");
        doThrow(cause).when(mock).perform(isA(ReleasePerformRequest.class));

        mojo.setReleaseManager(mock);

        // execute
        try {
            mojo.execute();

            fail("Should have thrown an exception");
        } catch (MojoFailureException e) {
            assertEquals("Check cause exists", cause, e.getCause());
        }

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify(mock).perform(argument.capture());
        assertNotNull(argument.getValue().getReleaseDescriptorBuilder());
        assertNotNull(argument.getValue().getReleaseEnvironment());
        assertNotNull(argument.getValue().getReactorProjects());
        assertEquals(Boolean.FALSE, argument.getValue().getDryRun());

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                argument.getValue().getReleaseDescriptorBuilder().build();
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());

        verifyNoMoreInteractions(mock);
    }

    public void testPerformWithScm() throws Exception {
        PerformReleaseMojo mojo = getMojoWithProjectSite("perform-with-scm.xml");

        ReleaseManager mock = mock(ReleaseManager.class);
        mojo.setReleaseManager(mock);

        // execute
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
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());
        assertEquals("scm-url", releaseDescriptor.getScmSourceUrl());

        verifyNoMoreInteractions(mock);
    }

    public void testPerformWithProfiles() throws Exception {
        PerformReleaseMojo mojo = getMojoWithProjectSite("perform.xml");

        MavenSession session = getVariableValueFromObject(mojo, "session");
        session.getRequest().setActiveProfiles(Arrays.asList("prof1", "2prof"));

        ReleaseManager mock = mock(ReleaseManager.class);
        mojo.setReleaseManager(mock);

        // execute
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
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());
        assertTrue(releaseDescriptor.getActivateProfiles().contains("prof1"));
        assertTrue(releaseDescriptor.getActivateProfiles().contains("2prof"));

        verifyNoMoreInteractions(mock);
    }

    public void testPerformWithProfilesAndArguments() throws Exception {
        PerformReleaseMojo mojo = getMojoWithProjectSite("perform-with-args.xml");

        MavenSession session = getVariableValueFromObject(mojo, "session");
        session.getRequest().setActiveProfiles(Arrays.asList("prof1", "2prof"));

        ReleaseManager mock = mock(ReleaseManager.class);
        mojo.setReleaseManager(mock);

        // execute
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
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());
        assertTrue(releaseDescriptor.getActivateProfiles().contains("prof1"));
        assertTrue(releaseDescriptor.getActivateProfiles().contains("2prof"));
        assertEquals("-Dmaven.test.skip=true", releaseDescriptor.getAdditionalArguments());

        verifyNoMoreInteractions(mock);
    }

    public void testPerformWithMultilineGoals() throws Exception {
        PerformReleaseMojo mojo = getMojoWithProjectSite("perform-with-multiline-goals.xml");

        ReleaseManager mock = mock(ReleaseManager.class);
        mojo.setReleaseManager(mock);

        // execute
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
        assertEquals("deploy site-deploy", releaseDescriptor.getPerformGoals());

        verifyNoMoreInteractions(mock);
    }

    protected void setUp() throws Exception {
        super.setUp();
        workingDirectory = getTestFile("target/test-classes/mojos/perform");
    }
}
