package org.apache.maven.shared.release;

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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderStub;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseDescriptorStore;
import org.apache.maven.shared.release.config.ReleaseDescriptorStoreException;
import org.apache.maven.shared.release.config.ReleaseDescriptorStoreStub;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.phase.ReleasePhase;
import org.apache.maven.shared.release.phase.ReleasePhaseStub;
import org.apache.maven.shared.release.phase.RunPerformGoalsPhase;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

/**
 * Test the default release manager.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DefaultReleaseManagerTest
    extends PlexusTestCase
{
    private ReleaseDescriptorStoreStub configStore;


    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        configStore = (ReleaseDescriptorStoreStub) lookup( ReleaseDescriptorStore.class, "stub" );
    }

    public void testPrepareNoCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase( null );

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setUserProperties( new Properties() );
        
        releaseManager.prepare( prepareRequest );

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step1" );
        assertTrue( "step1 executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step2" );
        assertTrue( "step2 executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step3" );
        assertTrue( "step3 executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase( "step1" );

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setUserProperties( new Properties() );
        
        releaseManager.prepare( prepareRequest );

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step1" );
        assertFalse( "step1 not executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step2" );
        assertTrue( "step2 executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step3" );
        assertTrue( "step3 executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareCompletedPhaseNoResume()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase( "step1" );

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setDryRun( false );
        prepareRequest.setResume( false );
        prepareRequest.setUserProperties( new Properties() );
        
        releaseManager.prepare( prepareRequest );

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step1" );
        assertFalse( "step1 executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step2" );
        assertTrue( "step2 executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step3" );
        assertTrue( "step3 executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareCompletedAllPhases()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase( "step3" );
        
        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setUserProperties( new Properties() );

        releaseManager.prepare( prepareRequest );

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step1" );
        assertFalse( "step1 not executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step2" );
        assertFalse( "step2 not executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step3" );
        assertFalse( "step3 not executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareInvalidCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase( "foo" );

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setUserProperties( new Properties() );
        
        releaseManager.prepare( prepareRequest );

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step1" );
        assertTrue( "step1 executed", phase.isExecuted() );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step2" );
        assertTrue( "step2 executed", phase.isExecuted() );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step3" );
        assertTrue( "step3 executed", phase.isExecuted() );
        assertFalse( "step3 not simulated", phase.isSimulated() );
    }

    public void testPrepareSimulateNoCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase( null );

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setDryRun( true );
        prepareRequest.setResume( true );
        prepareRequest.setUserProperties( new Properties() );
        
        releaseManager.prepare( prepareRequest );

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step1" );
        assertTrue( "step1 simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step2" );
        assertTrue( "step2 simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step3" );
        assertTrue( "step3 simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareSimulateCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase( "step1" );

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setDryRun( true );
        prepareRequest.setResume( true );
        prepareRequest.setUserProperties( new Properties() );
        
        releaseManager.prepare( prepareRequest );

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step1" );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step2" );
        assertTrue( "step2 simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step3" );
        assertTrue( "step3 simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareSimulateCompletedAllPhases()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase( "step3" );

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setDryRun( true );
        prepareRequest.setResume( true );
        prepareRequest.setUserProperties( new Properties() );
        
        releaseManager.prepare( prepareRequest );

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step1" );
        assertFalse( "step1 not simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step2" );
        assertFalse( "step2 not simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step3" );
        assertFalse( "step3 not simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareSimulateInvalidCompletedPhase()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = configStore.getReleaseConfiguration();
        builder.setCompletedPhase( "foo" );

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setDryRun( true );
        prepareRequest.setResume( true );
        prepareRequest.setUserProperties( new Properties() );
        
        releaseManager.prepare( prepareRequest );

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step1" );
        assertTrue( "step1 simulated", phase.isSimulated() );
        assertFalse( "step1 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step2" );
        assertTrue( "step2 simulated", phase.isSimulated() );
        assertFalse( "step2 not executed", phase.isExecuted() );
        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step3" );
        assertTrue( "step3 simulated", phase.isSimulated() );
        assertFalse( "step3 not executed", phase.isExecuted() );
    }

    public void testPrepareUnknownPhaseConfigured()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "bad-phase-configured" );

        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setReleaseStrategyId( "foo" );
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setUserProperties( new Properties() );
        
        try
        {
            releaseManager.prepare( prepareRequest );
            fail( "Should have failed to find a phase" );
        }
        catch ( ReleaseExecutionException e )
        {
            // good
        }
    }

    public void testReleaseConfigurationStoreReadFailure()
        throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorStore configStoreMock = mock( ReleaseDescriptorStore.class );
        when( configStoreMock.read( builder ) ).thenThrow( new ReleaseDescriptorStoreException( "message", new IOException( "ioExceptionMsg" ) ) );

        releaseManager.setConfigStore( configStoreMock );
        
        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setUserProperties( new Properties() );

        // execute
        try
        {
            releaseManager.prepare( prepareRequest );
            fail( "Should have failed to read configuration" );
        }
        catch ( ReleaseExecutionException e )
        {
            // good
            assertEquals( "check cause", ReleaseDescriptorStoreException.class, e.getCause().getClass() );
        }

        // verify
        verify( configStoreMock ).read( builder );
        verifyNoMoreInteractions( configStoreMock );
    }

    public void testReleaseConfigurationStoreWriteFailure()
        throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorStore configStoreMock = mock( ReleaseDescriptorStore.class );
        doThrow( new ReleaseDescriptorStoreException( "message", new IOException( "ioExceptionMsg" ) ) ).when( configStoreMock ).write( any( ReleaseDescriptor.class) );

        releaseManager.setConfigStore( configStoreMock );
        
        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( builder );
        prepareRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        prepareRequest.setDryRun( false );
        prepareRequest.setResume( false );
        prepareRequest.setUserProperties( new Properties() );


        // execute
        try
        {
            releaseManager.prepare( prepareRequest );
            fail( "Should have failed to read configuration" );
        }
        catch ( ReleaseExecutionException e )
        {
            // good
            assertEquals( "check cause", ReleaseDescriptorStoreException.class, e.getCause().getClass() );
        }

        // verify
        verify( configStoreMock ).write( any( ReleaseDescriptor.class ) ) ;
        verifyNoMoreInteractions( configStoreMock );
    }

    public void testReleaseConfigurationStoreClean()
        throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorStore configStoreMock = mock( ReleaseDescriptorStore.class );

        releaseManager.setConfigStore( configStoreMock );

        ReleaseCleanRequest cleanRequest = new ReleaseCleanRequest();
        cleanRequest.setReleaseDescriptorBuilder( builder );
        
        // execute
        releaseManager.clean( cleanRequest );

        // verify
        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step1" );
        assertTrue( "step1 not cleaned", phase.isCleaned() );

        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step2" );
        assertTrue( "step2 not cleaned", phase.isCleaned() );

        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "step3" );
        assertTrue( "step3 not cleaned", phase.isCleaned() );

        phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "branch1" );
        assertTrue( "branch1 not cleaned", phase.isCleaned() );

        verify( configStoreMock ).delete( any( ReleaseDescriptor.class ) );
        verifyNoMoreInteractions( configStoreMock );
    }

    private static List<MavenProject> createReactorProjects() throws IOException
    {
        MavenProject project = new MavenProject();
        
        File projectFile = getTestFile( "target/dummy-project/pom.xml" );
        if ( !projectFile.exists() )
        {
            projectFile.getParentFile().mkdirs();
            projectFile.createNewFile();
        }
        project.setFile( projectFile );
        return Collections.singletonList( project );
    }

    public void testReleasePerformWithResult()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setWorkingDirectory( getTestPath( "target/dummy-project" ) );
        
        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder( builder );
        performRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        performRequest.setReactorProjects( createReactorProjects() );

        ReleaseResult result = releaseManager.performWithResult( performRequest );

        assertTrue( result.getOutput().length() > 0 );
    }

    public void testReleaseConfigurationStoreReadFailureOnPerform()
        throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorStore configStoreMock = mock( ReleaseDescriptorStore.class );
        when( configStoreMock.read( builder ) ).thenThrow( new ReleaseDescriptorStoreException( "message", new IOException( "ioExceptionMsg" ) ) );

        releaseManager.setConfigStore( configStoreMock );
        
        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder( builder );
        performRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );

        // execute
        try
        {
            builder.setUseReleaseProfile( false );

            releaseManager.perform( performRequest );
            fail( "Should have failed to read configuration" );
        }
        catch ( ReleaseExecutionException e )
        {
            // good
            assertEquals( "check cause", ReleaseDescriptorStoreException.class, e.getCause().getClass() );
        }

        // verify
        verify( configStoreMock ).read( builder );
        verifyNoMoreInteractions( configStoreMock );
    }

    public void testReleasePerformWithIncompletePrepare()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorStoreStub configStore = new ReleaseDescriptorStoreStub();
        builder.setCompletedPhase( "scm-tag" );
        releaseManager.setConfigStore( configStore );

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder( builder );
        performRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );

        try
        {
            builder.setUseReleaseProfile( false );

            releaseManager.perform( performRequest );
            fail( "Should have failed to perform" );
        }
        catch ( ReleaseFailureException e )
        {
            // good
            assertTrue( true );
        }
    }

    // MRELEASE-758: release:perform no longer removes release.properties
    public void testPerformWithDefaultClean()
        throws Exception
    {
        // prepare
        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setDryRun( true );

        ReleaseManagerListener managerListener = mock( ReleaseManagerListener.class );
        performRequest.setReleaseManagerListener( managerListener );

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );
        performRequest.setReleaseDescriptorBuilder( builder );

        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        // test
        releaseManager.perform( performRequest );

        // verify
        verify( managerListener ).phaseStart( "verify-release-configuration" );
        verify( managerListener ).phaseStart( "verify-completed-prepare-phases" );
        verify( managerListener ).phaseStart( "checkout-project-from-scm" );
        verify( managerListener ).phaseStart( "run-perform-goals" );
        verify( managerListener ).phaseStart( "cleanup" );
        verify( managerListener, times( 5 ) ).phaseEnd();

        // not part of actual test, but required to confirm 'no more interactions'
        verify( managerListener ).goalStart( anyString(), any( List.class ) );
        verify( managerListener ).goalEnd();

        verifyNoMoreInteractions( managerListener );
    }

    public void testNoScmUrlPerform()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder( builder );
        performRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        
        try
        {
            builder.setUseReleaseProfile( false );

            releaseManager.perform( performRequest );

            fail( "perform should have failed" );
        }
        catch ( ReleaseFailureException e )
        {
            assertNull( "check no cause", e.getCause() );
        }
    }

    public void testScmExceptionThrown()
        throws Exception
    {
        // prepare
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        when( scmProviderMock.checkOut( any( ScmRepository.class ),
                                        any( ScmFileSet.class ),
                                        any( ScmTag.class ),
                                        any(CommandParameters.class)) )
            .thenThrow( new ScmException( "..." ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder( builder );
        performRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        performRequest.setReactorProjects( createReactorProjects() );

        // execute
        try
        {
            releaseManager.perform( performRequest );

            fail( "commit should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", ScmException.class, e.getCause().getClass() );
        }

        // verify
        verify(  scmProviderMock ).checkOut( any( ScmRepository.class ), any( ScmFileSet.class ),
                                             any( ScmTag.class ), any( CommandParameters.class ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

    public void testScmResultFailure()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        File checkoutDirectory = getTestFile( "target/checkout-directory" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setWorkingDirectory( getTestPath( "target/dummy-project" ) );

        ScmManager scmManager = (ScmManager) lookup( ScmManager.class );
        ScmProviderStub providerStub =
            (ScmProviderStub) scmManager.getProviderByUrl( "scm-url" );

        providerStub.setCheckOutScmResult( new CheckOutScmResult( "", "", "", false ) );
        
        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptorBuilder( builder );
        performRequest.setReleaseEnvironment( new DefaultReleaseEnvironment() );
        performRequest.setReactorProjects( createReactorProjects() );

        try
        {
            releaseManager.perform( performRequest );

            fail( "commit should have failed" );
        }
        catch ( ReleaseScmCommandException e )
        {
            assertNull( "check no other cause", e.getCause() );
        }
    }

    // MRELEASE-1042
    public void testKeepProfilesOnPerform()
            throws Exception
    {
        // prepare
        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setDryRun( true );

        ReleaseManagerListener managerListener = mock( ReleaseManagerListener.class );
        performRequest.setReleaseManagerListener( managerListener );

        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setActivateProfiles( Arrays.asList("aProfile", "anotherOne") );
        builder.setWorkingDirectory( getTestFile( "target/working-directory" ).getAbsolutePath() );
        performRequest.setReleaseDescriptorBuilder( builder );

        DefaultReleaseManager releaseManager = (DefaultReleaseManager) lookup( ReleaseManager.class, "test" );

        ReleaseDescriptorBuilder secondBuilder = new ReleaseDescriptorBuilder();
        secondBuilder.setActivateProfiles( new ArrayList( Arrays.asList("aProfile", "bProfile") ) );
        secondBuilder.setScmSourceUrl( "scm-url" );
        ReleaseDescriptorStore configStoreMock = mock( ReleaseDescriptorStore.class );
        when( configStoreMock.read( any( ReleaseDescriptorBuilder.class ) ) ).thenReturn( secondBuilder );
        releaseManager.setConfigStore( configStoreMock );

        // test
        ReleaseResult result = releaseManager.performWithResult( performRequest );

        // verify
        assertTrue( result.getOutput().contains( "-P aProfile,bProfile,anotherOne" ) );
    }

    public void testDetermineWorkingDirectory()
        throws Exception
    {
        DefaultReleaseManager defaultReleaseManager = new DefaultReleaseManager();

        File checkoutDir = getTestFile( "target/checkout" );
        FileUtils.forceDelete( checkoutDir );
        checkoutDir.mkdirs();

        File projectDir = getTestFile( "target/checkout/my/project" );
        projectDir.mkdirs();

        // only checkout dir
        assertEquals( checkoutDir, defaultReleaseManager.determineWorkingDirectory( checkoutDir, "" ) );
        assertEquals( checkoutDir, defaultReleaseManager.determineWorkingDirectory( checkoutDir, null ) );

        // checkout dir and relative path project dir
        assertEquals( projectDir, defaultReleaseManager.determineWorkingDirectory( checkoutDir, "my/project" ) );
        assertEquals( projectDir, defaultReleaseManager.determineWorkingDirectory( checkoutDir, "my/project/" ) );
        assertEquals( projectDir, defaultReleaseManager.determineWorkingDirectory( checkoutDir, "my" + File.separator +
            "project" ) );

        FileUtils.forceDelete( checkoutDir );
    }

    // MRELEASE-761
    public void testRollbackCall()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );
        
        ReleaseRollbackRequest rollbackRequest = new ReleaseRollbackRequest();
        rollbackRequest.setReleaseDescriptorBuilder( configStore.getReleaseConfiguration() );

        releaseManager.rollback( rollbackRequest );

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "rollbackPhase1" );

        assertTrue( "rollbackPhase1 executed", phase.isExecuted() );
    }


    // MRELEASE-765
    public void testUpdateVersionsCall()
        throws Exception
    {
        ReleaseManager releaseManager = lookup( ReleaseManager.class, "test" );
        
        ReleaseUpdateVersionsRequest updateVersionsRequest = new ReleaseUpdateVersionsRequest();
        updateVersionsRequest.setReleaseDescriptorBuilder( configStore.getReleaseConfiguration() );
        updateVersionsRequest.setUserProperties( new Properties() );

        releaseManager.updateVersions( updateVersionsRequest );

        ReleasePhaseStub phase = (ReleasePhaseStub) lookup( ReleasePhase.class, "updateVersionsPhase1" );

        assertTrue( "updateVersionsPhase1 executed", phase.isExecuted() );
    }
}