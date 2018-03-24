package org.apache.maven.plugins.release;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Profile;
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

/**
 * Test release:perform.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PerformReleaseMojoTest
    extends AbstractMojoTestCase
{
    private File workingDirectory;

    public void testPerform()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform.xml" );

        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( mojo );
        builder.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        File checkoutDirectory = getTestFile( "target/checkout" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setPerformGoals( "deploy site-deploy" );

        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );
        
        // execute
        mojo.execute();
        
        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify( mock ).perform( argument.capture() );
        assertNotNull( argument.getValue().getReleaseDescriptorBuilder() );
        assertNotNull( argument.getValue().getReleaseEnvironment()  );
        assertNotNull( argument.getValue().getReactorProjects() );
        assertEquals( Boolean.FALSE, argument.getValue().getDryRun() );
        verifyNoMoreInteractions( mock );
    }

    public void testPerformWithFlatStructure()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform-with-flat-structure.xml" );

        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( mojo );
        builder.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        File checkoutDirectory = getTestFile( "target/checkout" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setPerformGoals( "deploy" );
        builder.setScmSourceUrl( "scm:svn:file://localhost/target/svnroot/flat-multi-module/trunk/root-project" );

        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );

        // execute
        mojo.execute();
        
        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify( mock ).perform( argument.capture() );
        assertNotNull( argument.getValue().getReleaseDescriptorBuilder() );
        assertNotNull( argument.getValue().getReleaseEnvironment()  );
        assertNotNull( argument.getValue().getReactorProjects() );
        assertEquals( Boolean.FALSE, argument.getValue().getDryRun() );
        verifyNoMoreInteractions( mock );
    }

    
    public void testPerformWithoutSite()
        throws Exception
    {
        File testFileDirectory = getTestFile( "target/test-classes/mojos/perform/" );
        PerformReleaseMojo mojo =
            (PerformReleaseMojo) lookupMojo( "perform", new File( testFileDirectory, "perform-without-site.xml" ) );
        mojo.setBasedir( testFileDirectory );
        mojo.setPomFileName( "pom.xml" );
        
        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        setVariableValueToObject( mojo, "session", newMavenSession( project ) );

        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( mojo );
        builder.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        File checkoutDirectory = getTestFile( "target/checkout" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setPerformGoals( "deploy" );

        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );

        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify( mock ).perform( argument.capture() );
        assertNotNull( argument.getValue().getReleaseDescriptorBuilder() );
        assertNotNull( argument.getValue().getReleaseEnvironment()  );
        assertNotNull( argument.getValue().getReactorProjects() );
        assertEquals( Boolean.FALSE, argument.getValue().getDryRun() );
        verifyNoMoreInteractions( mock );
    }

    private PerformReleaseMojo getMojoWithProjectSite( String fileName )
        throws Exception
    {
        PerformReleaseMojo mojo = (PerformReleaseMojo) lookupMojo( "perform", new File( workingDirectory, fileName ) );
        mojo.setBasedir( workingDirectory );
        mojo.setPomFileName( fileName );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        DistributionManagement distributionManagement = new DistributionManagement();
        distributionManagement.setSite( new Site() );
        project.setDistributionManagement( distributionManagement );
        
        setVariableValueToObject( mojo, "session", newMavenSession( project ) );

        return mojo;
    }

    public void testPerformWithExecutionException()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform.xml" );

        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( mojo );
        builder.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        File checkoutDirectory = getTestFile( "target/checkout" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setPerformGoals( "deploy site-deploy" );

        ReleaseManager mock = mock( ReleaseManager.class );
        doThrow( new ReleaseExecutionException( "..." ) ).when( mock ).perform( isA( ReleasePerformRequest.class ) );
        mojo.setReleaseManager( mock );

        // execute
        try
        {
            mojo.execute();

            fail( "Should have thrown an exception" );
        }
        catch ( MojoExecutionException e )
        {
            assertEquals( "Check cause", ReleaseExecutionException.class, e.getCause().getClass() );
        }
        
        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify( mock ).perform( argument.capture() );
        assertNotNull( argument.getValue().getReleaseDescriptorBuilder() );
        assertNotNull( argument.getValue().getReleaseEnvironment()  );
        assertNotNull( argument.getValue().getReactorProjects() );
        assertEquals( Boolean.FALSE, argument.getValue().getDryRun() );

        verifyNoMoreInteractions( mock );
    }

    public void testPerformWithExecutionFailure()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform.xml" );

        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( mojo );
        builder.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        File checkoutDirectory = getTestFile( "target/checkout" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setPerformGoals( "deploy site-deploy" );

        ReleaseManager mock = mock( ReleaseManager.class );
        ReleaseFailureException cause = new ReleaseFailureException( "..." );
        doThrow( cause ).when( mock ).perform( isA( ReleasePerformRequest.class ) );

        mojo.setReleaseManager( mock );

        // execute
        try
        {
            mojo.execute();

            fail( "Should have thrown an exception" );
        }
        catch ( MojoFailureException e )
        {
            assertEquals( "Check cause exists", cause, e.getCause() );
        }
        
        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify( mock ).perform( argument.capture() );
        assertNotNull( argument.getValue().getReleaseDescriptorBuilder() );
        assertNotNull( argument.getValue().getReleaseEnvironment()  );
        assertNotNull( argument.getValue().getReactorProjects() );
        assertEquals( Boolean.FALSE, argument.getValue().getDryRun() );

        verifyNoMoreInteractions( mock );
    }

    public void testPerformWithScm()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform-with-scm.xml" );

        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( mojo );
        builder.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        File checkoutDirectory = getTestFile( "target/checkout" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setPerformGoals( "deploy site-deploy" );
        builder.setScmSourceUrl( "scm-url" );

        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );

        // execute
        mojo.execute();
        
        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify( mock ).perform( argument.capture() );
        assertNotNull( argument.getValue().getReleaseDescriptorBuilder() );
        assertNotNull( argument.getValue().getReleaseEnvironment()  );
        assertNotNull( argument.getValue().getReactorProjects() );
        assertEquals( Boolean.FALSE, argument.getValue().getDryRun() );

        verifyNoMoreInteractions( mock );
    }

    public void testPerformWithProfiles()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform.xml" );

        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( mojo );
        builder.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        File checkoutDirectory = getTestFile( "target/checkout" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setPerformGoals( "deploy site-deploy" );
        builder.setAdditionalArguments( "-P prof1,2prof" );

        MavenSession session = (MavenSession) getVariableValueFromObject( mojo, "session");
        Profile profile1 = new Profile();
        profile1.setId( "prof1" );
        session.getRequest().addProfile( profile1 );
        Profile profile2 = new Profile();
        profile2.setId( "2prof" );
        session.getRequest().addProfile( profile2 );
        session.getRequest().setActiveProfiles( Arrays.asList( "prof1", "2prof" ) );

        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );

        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify( mock ).perform( argument.capture() );
        assertNotNull( argument.getValue().getReleaseDescriptorBuilder() );
        assertNotNull( argument.getValue().getReleaseEnvironment()  );
        assertNotNull( argument.getValue().getReactorProjects() );
        assertEquals( Boolean.FALSE, argument.getValue().getDryRun() );

        verifyNoMoreInteractions( mock );
    }

    public void testPerformWithProfilesAndArguments()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform-with-args.xml" );

        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( mojo );
        builder.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        File checkoutDirectory = getTestFile( "target/checkout" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setPerformGoals( "deploy site-deploy" );
        builder.setAdditionalArguments( "-Dmaven.test.skip=true -P prof1,2prof" );

        MavenSession session = (MavenSession) getVariableValueFromObject( mojo, "session");
        Profile profile1 = new Profile();
        profile1.setId( "prof1" );
        session.getRequest().addProfile( profile1 );
        Profile profile2 = new Profile();
        profile2.setId( "2prof" );
        session.getRequest().addProfile( profile2 );
        session.getRequest().setActiveProfiles( Arrays.asList( "prof1", "2prof" ) );

        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );

        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify( mock ).perform( argument.capture() );
        assertNotNull( argument.getValue().getReleaseDescriptorBuilder() );
        assertNotNull( argument.getValue().getReleaseEnvironment()  );
        assertNotNull( argument.getValue().getReactorProjects() );
        assertEquals( Boolean.FALSE, argument.getValue().getDryRun() );

        verifyNoMoreInteractions( mock );
    }

    public void testPerformWithMultilineGoals()
        throws Exception
    {
	    PerformReleaseMojo mojo = getMojoWithProjectSite( "perform-with-multiline-goals.xml" );

        ReleaseDescriptorBuilder builder = createReleaseDescriptorBuilder( mojo );
        builder.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        File checkoutDirectory = getTestFile( "target/checkout" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setPerformGoals( "deploy site-deploy" );

        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );

        // execute
        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify( mock ).perform( argument.capture() );
        assertNotNull( argument.getValue().getReleaseDescriptorBuilder() );
        assertNotNull( argument.getValue().getReleaseEnvironment()  );
        assertNotNull( argument.getValue().getReactorProjects() );
        assertEquals( Boolean.FALSE, argument.getValue().getDryRun() );

        verifyNoMoreInteractions( mock );
    }

    private ReleaseDescriptorBuilder createReleaseDescriptorBuilder( PerformReleaseMojo mojo ) throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        
        @SuppressWarnings( "unchecked" )
        List<MavenProject> reactorProjects = (List<MavenProject>) getVariableValueFromObject( mojo, "reactorProjects" );
        
        for ( MavenProject project : reactorProjects )
        {
            builder.putOriginalVersion( project.getGroupId() + ':' + project.getArtifactId(), project.getVersion() );
        }
        
        return builder;
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        workingDirectory = getTestFile( "target/test-classes/mojos/perform" );
    }
}