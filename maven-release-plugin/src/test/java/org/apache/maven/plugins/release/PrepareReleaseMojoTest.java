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


import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.ReleasePrepareRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.mockito.ArgumentCaptor;

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
    
    public void testPrepare()
        throws Exception
    {
        File testFile = getTestFile( "target/test-classes/mojos/prepare/prepare.xml" );
        final PrepareReleaseMojo mojo = (PrepareReleaseMojo) lookupMojo( "prepare", testFile );
        setDefaults( mojo );
        mojo.setBasedir( testFile.getParentFile() );
        mojo.setPomFileName( "pom.xml" );
        mojo.session = new MavenSession( null, null, null, null, null, null, null, null, null )
        {
            public Properties getExecutionProperties()
            {
                return new Properties();
            };

            @Override
            public List<MavenProject> getProjects()
            {
                return Collections.singletonList( mojo.project );
            }
        };
        
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setWorkingDirectory( testFile.getParentFile().getAbsolutePath() );
        builder.setUpdateDependencies( false );
        
        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );

        // execute
        mojo.execute();

        ArgumentCaptor<ReleasePrepareRequest> prepareRequest = ArgumentCaptor.forClass( ReleasePrepareRequest.class );
        
        // verify
        verify( mock ).prepare( prepareRequest.capture() );
        
        assertThat( prepareRequest.getValue().getReleaseDescriptorBuilder(),
                    is( instanceOf( ReleaseDescriptorBuilder.class ) ) );
        assertThat( prepareRequest.getValue().getReleaseEnvironment(), is( instanceOf( ReleaseEnvironment.class ) ) );
        assertThat( prepareRequest.getValue().getReactorProjects(), is( notNullValue() ) );
        assertThat( prepareRequest.getValue().getResume(), is( true ) );
        assertThat( prepareRequest.getValue().getDryRun(), is( false ) );
    }

    public void testPrepareWithExecutionException()
        throws Exception
    {
        File testFile = getTestFile( "target/test-classes/mojos/prepare/prepare.xml" );
        final PrepareReleaseMojo mojo = (PrepareReleaseMojo) lookupMojo( "prepare", testFile );
        setDefaults( mojo );
        mojo.setBasedir( testFile.getParentFile() );
        mojo.setPomFileName( "pom.xml" );
        mojo.session = new MavenSession( null, null, null, null, null, null, null, null, null )
        {
          public Properties getExecutionProperties(){
              return new Properties();
          };

          @Override
          public List<MavenProject> getProjects()
          {
              return Collections.singletonList( mojo.project );
          }
        };

        ReleaseManager mock = mock( ReleaseManager.class );
        doThrow( new ReleaseExecutionException( "..." ) ).when( mock ).prepare( isA( ReleasePrepareRequest.class ) );
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
        verify( mock ).prepare( isA( ReleasePrepareRequest.class ) );
        verifyNoMoreInteractions( mock );
    }

    public void testPrepareWithExecutionFailure()
        throws Exception
    {
        File testFile = getTestFile( "target/test-classes/mojos/prepare/prepare.xml" );
        final PrepareReleaseMojo mojo = (PrepareReleaseMojo) lookupMojo( "prepare", testFile );
        setDefaults( mojo );
        mojo.setBasedir( testFile.getParentFile() );
        mojo.setPomFileName( "pom.xml" );
        mojo.session = new MavenSession( null, null, null, null, null, null, null, null, null )
        {
          public Properties getExecutionProperties(){
              return new Properties();
          };
          
          @Override
          public List<MavenProject> getProjects()
          {
              return Collections.singletonList( mojo.project );
          }
        };
        
        ReleaseManager mock = mock( ReleaseManager.class );
        ReleaseFailureException cause = new ReleaseFailureException( "..." );
        doThrow( cause ).when( mock ).prepare( isA( ReleasePrepareRequest.class ) );
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
        verify( mock ).prepare( isA( ReleasePrepareRequest.class ) );
        verifyNoMoreInteractions( mock );
    }

    public void testLineSeparatorInPrepareWithPom()
      throws Exception
    {
        File testFile = getTestFile( "target/test-classes/mojos/prepare/prepare.xml" );
        final PrepareWithPomReleaseMojo mojo = (PrepareWithPomReleaseMojo) lookupMojo( "prepare-with-pom", testFile );
        setDefaults( mojo );
        setVariableValueToObject( mojo, "generateReleasePoms", Boolean.TRUE );
        mojo.setBasedir( testFile.getParentFile() );
        mojo.setPomFileName( "pom.xml" );
        mojo.project.setFile( testFile );
        mojo.session = new MavenSession( null, null, null, null, null, null, null, null, null )
        {
            public Properties getExecutionProperties()
            {
                return new Properties();
            };

            @Override
            public List<MavenProject> getProjects()
            {
                return Collections.singletonList( mojo.project );
            }
        };

        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );

        int times = 1;
        testLineSeparator(null, "\n", mojo, mock, times++);
        testLineSeparator("source", "\n", mojo, mock, times++);
        testLineSeparator("cr", "\r", mojo, mock, times++);
        testLineSeparator("lf", "\n", mojo, mock, times++);
        testLineSeparator("crlf", "\r\n", mojo, mock, times++);
        testLineSeparator("system", System.getProperty( "line.separator" ), mojo, mock, times++);
    }
    
    private void testLineSeparator( String lineSeparator, String expected, PrepareWithPomReleaseMojo mojo,
                                   ReleaseManager releaseManager, int times )
      throws Exception 
    {
        
        setVariableValueToObject( mojo, "lineSeparator", lineSeparator );
        
        mojo.execute();
        
        ArgumentCaptor<ReleasePrepareRequest> prepareRequest = ArgumentCaptor.forClass( ReleasePrepareRequest.class );
        verify( releaseManager , times( times ) ).prepare( prepareRequest.capture() );
        
        assertEquals( expected, prepareRequest.getValue().getReleaseDescriptorBuilder().build().getLineSeparator() );
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
