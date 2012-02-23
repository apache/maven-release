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

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;

/**
 * Test release:prepare.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PrepareReleaseMojoTest
    extends AbstractMojoTestCase
{
    private void setDefaults( PrepareReleaseMojo mojo )
        throws IllegalAccessException
    {
        setVariableValueToObject( mojo, "updateWorkingCopyVersions", Boolean.TRUE );
    }
    
    @SuppressWarnings( "unchecked" )
    public void testPrepare()
        throws Exception
    {
        File testFile = getTestFile( "target/test-classes/mojos/prepare/prepare.xml" );
        PrepareReleaseMojo mojo = (PrepareReleaseMojo) lookupMojo( "prepare", testFile );
        setDefaults( mojo );
        mojo.setBasedir( testFile.getParentFile() );
        mojo.session = new MavenSession( null, null, null, null, null, null, null, null, null )
        {
          public Properties getExecutionProperties(){
              return new Properties();
          };
        };
        
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( testFile.getParentFile().getAbsolutePath() );
        releaseDescriptor.setUpdateDependencies( false );
        
        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );

        // execute
        mojo.execute();

        // verify
        verify( mock ).prepare( eq( releaseDescriptor ), isA( ReleaseEnvironment.class ), isNull( List.class), eq( true ), eq( false ) );
        assertTrue( true );
    }

    @SuppressWarnings( "unchecked" )
    public void testPrepareWithExecutionException()
        throws Exception
    {
        File testFile = getTestFile( "target/test-classes/mojos/prepare/prepare.xml" );
        PrepareReleaseMojo mojo = (PrepareReleaseMojo) lookupMojo( "prepare", testFile );
        setDefaults( mojo );
        mojo.setBasedir( testFile.getParentFile() );
        mojo.session = new MavenSession( null, null, null, null, null, null, null, null, null )
        {
          public Properties getExecutionProperties(){
              return new Properties();
          };
        };
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( testFile.getParentFile().getAbsolutePath() );
        releaseDescriptor.setUpdateDependencies( false );
        
        ReleaseManager mock = mock( ReleaseManager.class );
        doThrow( new ReleaseExecutionException( "..." ) ).when( mock ).prepare( eq( releaseDescriptor ), 
                                                                                isA( ReleaseEnvironment.class ), 
                                                                                isNull( List.class), 
                                                                                eq( true ), 
                                                                                eq( false ) );
        mojo.setReleaseManager( mock );

        //execute
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
        verify( mock ).prepare( eq( releaseDescriptor ), 
                                isA( ReleaseEnvironment.class ), 
                                isNull( List.class), 
                                eq( true ), 
                                eq( false ) );
        verifyNoMoreInteractions( mock );
    }

    @SuppressWarnings( "unchecked" )
    public void testPrepareWithExecutionFailure()
        throws Exception
    {
        File testFile = getTestFile( "target/test-classes/mojos/prepare/prepare.xml" );
        PrepareReleaseMojo mojo = (PrepareReleaseMojo) lookupMojo( "prepare", testFile );
        setDefaults( mojo );
        mojo.setBasedir( testFile.getParentFile() );
        mojo.session = new MavenSession( null, null, null, null, null, null, null, null, null )
        {
          public Properties getExecutionProperties(){
              return new Properties();
          };
        };
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( testFile.getParentFile().getAbsolutePath() );
        releaseDescriptor.setUpdateDependencies( false );
        
        ReleaseManager mock = mock( ReleaseManager.class );
        ReleaseFailureException cause = new ReleaseFailureException( "..." );
        doThrow( cause ).when( mock ).prepare( eq( releaseDescriptor ), 
                                               isA( ReleaseEnvironment.class ), 
                                               isNull( List.class), 
                                               eq( true ), 
                                               eq( false ) );
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
        verify( mock ).prepare( eq( releaseDescriptor ), 
                                isA( ReleaseEnvironment.class ), 
                                isNull( List.class), 
                                eq( true ), 
                                eq( false ) );
        verifyNoMoreInteractions( mock );
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
