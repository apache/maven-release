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
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.ReleasePrepareRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test release:prepare.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@ExtendWith(MockitoExtension.class)
@MojoTest
class PrepareReleaseMojoTest {

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
        when(mavenProject.getFile()).thenReturn(new File(getBasedir(), "prepare.xml"));

        when(mavenProject.getGroupId()).thenReturn("groupId");
        when(mavenProject.getArtifactId()).thenReturn("artifactId");
        when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");

        when(mavenSession.getProjects()).thenReturn(Collections.singletonList(mavenProject));
        when(mavenSession.getRequest()).thenReturn(new DefaultMavenExecutionRequest());
    }

    @Test
    @Basedir("/mojos/prepare")
    @InjectMojo(goal = "prepare", pom = "prepare.xml")
    @MojoParameter(name = "updateDependencies", value = "false")
    void testPrepare(PrepareReleaseMojo mojo) throws Exception {
        // execute
        mojo.execute();

        ArgumentCaptor<ReleasePrepareRequest> prepareRequest = ArgumentCaptor.forClass(ReleasePrepareRequest.class);

        // verify
        verify(releaseManagerMock).prepare(prepareRequest.capture());

        assertThat(
                prepareRequest.getValue().getReleaseDescriptorBuilder(),
                is(instanceOf(ReleaseDescriptorBuilder.class)));
        assertThat(prepareRequest.getValue().getReleaseEnvironment(), is(instanceOf(ReleaseEnvironment.class)));
        assertThat(prepareRequest.getValue().getReactorProjects(), is(notNullValue()));
        assertThat(prepareRequest.getValue().getResume(), is(true));
        assertThat(prepareRequest.getValue().getDryRun(), is(false));

        ReleaseDescriptorBuilder.BuilderReleaseDescriptor releaseDescriptor =
                prepareRequest.getValue().getReleaseDescriptorBuilder().build();
        assertThat(releaseDescriptor.isScmSignTags(), is(false));
        assertThat(releaseDescriptor.isUpdateDependencies(), is(false));
    }

    @Test
    @Basedir("/mojos/prepare")
    @InjectMojo(goal = "prepare", pom = "prepare.xml")
    void testPrepareWithExecutionException(PrepareReleaseMojo mojo) throws Exception {
        doThrow(new ReleaseExecutionException("..."))
                .when(releaseManagerMock)
                .prepare(isA(ReleasePrepareRequest.class));

        // execute
        try {
            mojo.execute();

            fail("Should have thrown an exception");
        } catch (MojoExecutionException e) {
            assertEquals(ReleaseExecutionException.class, e.getCause().getClass(), "Check cause");
        }

        // verify
        verify(releaseManagerMock).prepare(isA(ReleasePrepareRequest.class));
        verifyNoMoreInteractions(releaseManagerMock);
    }

    @Test
    @Basedir("/mojos/prepare")
    @InjectMojo(goal = "prepare", pom = "prepare.xml")
    void testPrepareWithExecutionFailure(PrepareReleaseMojo mojo) throws Exception {

        ReleaseFailureException cause = new ReleaseFailureException("...");
        doThrow(cause).when(releaseManagerMock).prepare(isA(ReleasePrepareRequest.class));

        // execute
        try {
            mojo.execute();

            fail("Should have thrown an exception");
        } catch (MojoFailureException e) {
            assertEquals(cause, e.getCause(), "Check cause exists");
        }
        // verify
        verify(releaseManagerMock).prepare(isA(ReleasePrepareRequest.class));
        verifyNoMoreInteractions(releaseManagerMock);
    }

    @Test
    @Basedir("/mojos/prepare")
    @InjectMojo(goal = "prepare-with-pom", pom = "prepare.xml")
    void testLineSeparatorInPrepareWithPom(PrepareReleaseMojo mojo) throws Exception {
        int times = 1;
        testLineSeparator(null, "\n", mojo, releaseManagerMock, times++);
        testLineSeparator("source", "\n", mojo, releaseManagerMock, times++);
        testLineSeparator("cr", "\r", mojo, releaseManagerMock, times++);
        testLineSeparator("lf", "\n", mojo, releaseManagerMock, times++);
        testLineSeparator("crlf", "\r\n", mojo, releaseManagerMock, times++);
        testLineSeparator("system", System.lineSeparator(), mojo, releaseManagerMock, times++);
    }

    @Test
    @Basedir("/mojos/prepare")
    @InjectMojo(goal = "prepare", pom = "prepare.xml")
    void testLineSeparatorInPrepare(PrepareReleaseMojo mojo) throws Exception {
        int times = 1;
        testLineSeparator(null, "\n", mojo, releaseManagerMock, times++);
        testLineSeparator("source", "\n", mojo, releaseManagerMock, times++);
        testLineSeparator("cr", "\r", mojo, releaseManagerMock, times++);
        testLineSeparator("lf", "\n", mojo, releaseManagerMock, times++);
        testLineSeparator("crlf", "\r\n", mojo, releaseManagerMock, times++);
        testLineSeparator("system", System.lineSeparator(), mojo, releaseManagerMock, times++);
    }

    private void testLineSeparator(
            String lineSeparator, String expected, PrepareReleaseMojo mojo, ReleaseManager releaseManager, int times)
            throws Exception {

        setVariableValueToObject(mojo, "lineSeparator", lineSeparator);

        mojo.execute();

        ArgumentCaptor<ReleasePrepareRequest> prepareRequest = ArgumentCaptor.forClass(ReleasePrepareRequest.class);
        verify(releaseManager, times(times)).prepare(prepareRequest.capture());

        assertEquals(
                expected,
                prepareRequest.getValue().getReleaseDescriptorBuilder().build().getLineSeparator());
    }

    /*
        public void testPerformWithScm()
            throws Exception
        {
            PerformReleaseMojo mojo = (PerformReleaseMojo) lookupMojo( "perform", getTestFile(
                "target/test-classes/mojos/perform/perform-with-scm.xml" ) );

            ReleaseDescriptor releaseConfiguration = new ReleaseDescriptor();
            releaseConfiguration.setSettings( mojo.getSettings() );
            releaseConfiguration.setUrl( "scm-url" );

            Mock mock = new Mock( ReleaseManager.class );
            Constraint[] constraints = new Constraint[]{new IsEqual( releaseConfiguration ),
                new IsEqual( new File( getBasedir(), "target/checkout" ) ), new IsEqual( "deploy site-deploy" ),
                new IsEqual( Boolean.TRUE )};
            mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
            mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

            mojo.execute();

            assertTrue( true );
        }

        public void testPerformWithProfiles()
            throws Exception
        {
            PerformReleaseMojo mojo = (PerformReleaseMojo) lookupMojo( "perform", getTestFile(
                "target/test-classes/mojos/perform/perform.xml" ) );

            ReleaseDescriptor releaseConfiguration = new ReleaseDescriptor();
            releaseConfiguration.setSettings( mojo.getSettings() );
            releaseConfiguration.setAdditionalArguments( "-P prof1,2prof" );

            MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
            Profile profile1 = new Profile();
            profile1.setId( "prof1" );
            Profile profile2 = new Profile();
            profile2.setId( "2prof" );
            project.setActiveProfiles( Arrays.asList( new Profile[]{profile1, profile2} ) );

            Mock mock = new Mock( ReleaseManager.class );
            Constraint[] constraints = new Constraint[]{new IsEqual( releaseConfiguration ),
                new IsEqual( new File( getBasedir(), "target/checkout" ) ), new IsEqual( "deploy site-deploy" ),
                new IsEqual( Boolean.TRUE )};
            mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
            mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

            mojo.execute();

            assertTrue( true );
        }

        public void testPerformWithProfilesAndArguments()
            throws Exception
        {
            PerformReleaseMojo mojo = (PerformReleaseMojo) lookupMojo( "perform", getTestFile(
                "target/test-classes/mojos/perform/perform-with-args.xml" ) );

            ReleaseDescriptor releaseConfiguration = new ReleaseDescriptor();
            releaseConfiguration.setSettings( mojo.getSettings() );
            releaseConfiguration.setAdditionalArguments( "-Dmaven.test.skip=true -P prof1,2prof" );

            MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
            Profile profile1 = new Profile();
            profile1.setId( "prof1" );
            Profile profile2 = new Profile();
            profile2.setId( "2prof" );
            project.setActiveProfiles( Arrays.asList( new Profile[]{profile1, profile2} ) );

            Mock mock = new Mock( ReleaseManager.class );
            Constraint[] constraints = new Constraint[]{new IsEqual( releaseConfiguration ),
                new IsEqual( new File( getBasedir(), "target/checkout" ) ), new IsEqual( "deploy site-deploy" ),
                new IsEqual( Boolean.TRUE )};
            mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
            mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

            mojo.execute();

            assertTrue( true );
        }
    */
}
